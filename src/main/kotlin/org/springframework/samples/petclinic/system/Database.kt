/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.system

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.StatementContext
import org.springframework.samples.petclinic.PetClinicApplication
import org.springframework.samples.petclinic.PetClinicApplication.getHikariDataSource
import org.springframework.samples.petclinic.owner.*
import org.springframework.samples.petclinic.vet.Specialty
import org.springframework.samples.petclinic.vet.Vet
import java.math.BigInteger
import java.sql.Date
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource

/**
 * Repository class for `Owner` domain objects All method names are compliant
 * with Spring Data naming conventions so this interface can easily be extended for Spring
 * Data. See:
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 */
class Database(
    dataSource: DataSource? = getHikariDataSource("jdbc:h2:mem:testdb", "sa", "password"), val type: DatabaseType = DatabaseType.H2
) {
    fun createTables() {
        val folderName = when (type) {
            DatabaseType.H2 -> "h2"
            DatabaseType.MySQL -> "mysql"
            DatabaseType.Postgres -> "postgres"
        }
        executeScript(
            String(PetClinicApplication::class.java.getResourceAsStream("/db/$folderName/schema.sql").readAllBytes())
        )
    }

    fun createPetTypes() {
        executeScript(
            String(PetClinicApplication::class.java.getResourceAsStream("/db/pet_types_data.sql").readAllBytes())
        )
    }

    enum class DatabaseType { H2, MySQL, Postgres }

    private val jdbi: Jdbi = Jdbi.create(dataSource)

    /**
     * Retrieve all [PetType]s from the data store.
     * @return a Collection of [PetType]s.
     */
    fun findPetTypes(): List<PetType> = jdbi.inTransaction<List<PetType>, RuntimeException> { handle: Handle ->
        handle.createQuery("SELECT * FROM types ORDER BY name")
            .mapToBean(PetType::class.java)
            .map { petType ->
                val capitalized = petType.name!!.substring(0, 1).uppercase(Locale.getDefault()) + petType.name!!.substring(1)
                petType.name = capitalized
                petType
            }
            .list()
    }

    /**
     * Retrieve [Owner]s from the data store by last name, returning all owners
     * whose last name *starts* with the given name.
     * @param lastName Value to search for
     * @return a Collection of matching [Owner]s (or an empty Collection if none
     * found)
     */
    fun findByLastName(lastName: String?, pageable: Pageable?): Page<OwnerAndPets> {
        val results = jdbi.inTransaction<List<OwnerAndPets>, RuntimeException> { handle: Handle ->
            val owners = handle.createQuery("SELECT DISTINCT * FROM owners WHERE last_name LIKE CONCAT(:lastName, '%')")
                .bind("lastName", lastName).mapToBean(Owner::class.java).list()
            owners.map { owner ->
                OwnerAndPets(
                    owner,
                    pets = handle.createQuery("SELECT * FROM pets WHERE owner_id = :id")
                        .bind("id", owner.id)
                        .mapToBean(Pet::class.java).list()
                )
            }
        }

        return Page(results, pageable)
    }

    /**
     * Retrieve an [Owner] from the data store by id.
     * @param id the id to search for
     * @return the [Owner] if found
     */
    fun findOwnerAndPetsByOwnerId(id: Int): OwnerAndPets? = jdbi.inTransaction<OwnerAndPets, RuntimeException> { handle: Handle ->
        val optionalOwner = handle.createQuery("SELECT * FROM owners WHERE id = :id").bind("id", id).mapToBean(
            Owner::class.java
        ).findFirst()

        if (optionalOwner.isPresent) {
            val owner = optionalOwner.get()
            val pets = handle.createQuery("SELECT pets.*, types.id as pet_type_id, types.name as pet_type_name FROM pets LEFT JOIN types on pets.type_id = types.id WHERE owner_id = :id")
                .bind("id", id)
                .mapToMap()
                .list().map { map ->
                    Pet().apply {
                        this.id = when (val petId = map["id"]) {
                            is Int -> petId
                            is Long -> Math.toIntExact(petId)
                            else -> throw IllegalArgumentException("petId type not supported: " + petId!!.javaClass)
                        }
                        this.type = PetType().apply {
                            this.id = when (val petTypeId = map["pet_type_id"]) {
                                is Int -> petTypeId
                                is Long -> Math.toIntExact((petTypeId as Long?)!!)
                                else -> throw IllegalArgumentException("petId type not supported: " + petTypeId!!.javaClass)
                            }
                            name = map["pet_type_name"] as String?
                        }
                        this.ownerId = when (val ownerId = map["owner_id"]) {
                            is Int -> ownerId
                            is Long -> Math.toIntExact((ownerId as Long?)!!)
                            else -> throw IllegalArgumentException("petId type not supported: " + ownerId!!.javaClass)
                        }
                        this.name = map["name"] as String?
                        this.birthDate = (map["birth_date"] as Date).toLocalDate()
                    }
                }.toList()
            OwnerAndPets(owner, pets)
        } else {
            null
        }
    }

    fun findVisitsForPet(petId: Int?): List<Visit> = jdbi.withHandle<List<Visit>, RuntimeException> { handle: Handle ->
        handle.createQuery("SELECT * FROM visits WHERE pet_id =:id")
            .bind("id", petId)
            .map { rs: ResultSet, _: StatementContext ->
                Visit().apply {
                    this.id = rs.getInt("id")
                    this.description = rs.getString("description")
                    this.date = rs.getDate("visit_date").toLocalDate()
                }
            }
            .list()
    }

    /**
     * Save an [Owner] to the data store, either inserting or updating it.
     * @param owner the [Owner] to save
     */
    fun save(owner: Owner) {
        jdbi.inTransaction<Any?, RuntimeException> { handle: Handle ->
            val updatedRowsCount = if (owner.id == null) 0 else handle.createUpdate(
                """
				update owners
				set first_name = :firstName, last_name = :lastName, address = :address, city = :city, telephone = :telephone
				where id = :id

				""".trimIndent()
            )
                .bindBean(owner)
                .execute()
            if (updatedRowsCount == 0) {
                val id = handle.createUpdate(
                    """
						insert into owners (first_name, last_name, address, city, telephone) values (:firstName, :lastName, :address, :city, :telephone)

						""".trimIndent()
                )
                    .bindBean(owner)
                    .executeAndReturnGeneratedKeys("id")
                    .mapToMap().one()
                owner.id = when (val idObject = id.values.stream().findFirst().get()) {
                    is BigInteger -> idObject.toInt()
                    is Int -> idObject
                    is Long -> idObject.toInt()
                    else -> throw IllegalStateException("Don't understand id value: $idObject")
                }
            }
        }
    }

    /**
     * Returns all the owners from data store
     */
    fun findAll(pageable: Pageable?) = Page(jdbi.inTransaction<List<Owner>, RuntimeException> { handle ->
        handle.createQuery("SELECT owner FROM Owner owner")
            .mapToBean(Owner::class.java)
            .list()
    })

    fun clear() {
        jdbi.inTransaction<Any?, RuntimeException> { handle ->
            handle.createUpdate("delete from visits;").execute()
            handle.createUpdate("delete from pets;").execute()
            handle.createUpdate("delete from owners;").execute()
            handle.createUpdate("delete from vet_specialties;").execute()
            handle.createUpdate("delete from vets;").execute()
        }
    }

    fun save(pet: Pet) {
        jdbi.inTransaction<Any?, RuntimeException> { handle: Handle ->
            val updatedRowsCount = if (pet.id == null) 0 else handle.createUpdate(
                "update pets " +
                        "set name = :name, birth_date = :birthDate, type_id = :typeId, owner_id = :ownerId " +
                        "where id = :id "
            )
                .bindBean(pet)
                .bind("typeId", pet.type!!.id)
                .execute()
            if (updatedRowsCount == 0) {
                val id = handle.createUpdate("insert into pets (name, birth_date, type_id, owner_id) values (:name, :birthDate, :typeId, :ownerId)\n")
                    .bindBean(pet)
                    .bind("typeId", pet.type!!.id)
                    .executeAndReturnGeneratedKeys("id")
                    .mapToMap().one()
                pet.id = when (val idObject = id.values.first()) {
                    is BigInteger -> idObject.toInt()
                    is Int -> idObject
                    is Long -> idObject.toInt()
                    else -> throw IllegalStateException("Don't understand id value: $idObject")
                }
            }
        }
    }

    fun save(visit: Visit, petId: Int) {
        jdbi.inTransaction<Any?, RuntimeException> { handle: Handle ->
            val updatedRowsCount = if (visit.id == null) 0 else handle.createUpdate(
                """
				update visits
				set pet_id = :petId, visit_date = :date, description = :description
				where id = :id

				""".trimIndent()
            )
                .bindBean(visit)
                .bind("petId", petId)
                .execute()
            if (updatedRowsCount == 0) {
                val id = handle.createUpdate(
                    """
					insert into visits (pet_id, visit_date, description) values (:petId, :date, :description)

					""".trimIndent()
                )
                    .bindBean(visit)
                    .bind("petId", petId)
                    .executeAndReturnGeneratedKeys("id")
                    .mapToMap().one()
                visit.id = when (val idObject = id.values.first()) {
                    is BigInteger -> idObject.toInt()
                    is Int -> idObject
                    is Long -> idObject.toInt()
                    else -> throw IllegalStateException("Don't understand id value: $idObject")
                }
            }
        }
    }

    fun save(pets: List<Pet>) {
        for (pet in pets) {
            save(pet)
        }
    }

    fun findAllVets(): List<Vet> = jdbi.withHandle<List<Vet>, RuntimeException> { handle ->
        handle.createQuery("select * from vets").mapToBean(Vet::class.java).list()
    }

    fun findAllVetsPageable(pageable: Pageable): Page<Vet> = Page(findAllVets(), pageable)

    fun findPetById(petId: Int): Pet = jdbi.withHandle<Pet, RuntimeException> { handle: Handle ->
        val petOrNull =
            handle.createQuery("select pets.*, types.id as _type_id, types.name as _type_name from pets left join types on pets.type_id = types.id where pets.id = :id ")
                .bind("id", petId).mapToMap().findFirst().orElse(null)
        if (petOrNull != null) {
            Pet().apply {
                this.id = petOrNull["id"] as Int?
                this.ownerId = petOrNull["owner_id"] as Int?
                this.birthDate = (petOrNull["birth_date"] as Date).toLocalDate()
                this.name = petOrNull["name"] as String?
                this.type = PetType().apply {
                    this.id = petOrNull["_type_id"] as Int?
                    this.name = petOrNull["_type_name"] as String?
                }
            }
        } else {
            null
        }
    }

    fun save(vet: Vet): Vet {
        jdbi.inTransaction<Any?, RuntimeException> { handle: Handle ->
            val updatedRowsCount = if (vet.id == null) 0 else handle.createUpdate(
                """
				update vets
				set first_name = :firstName, last_name = :lastName
				where id = :id

				""".trimIndent()
            )
                .bindBean(vet)
                .execute()
            if (updatedRowsCount == 0) {
                val id = handle.createUpdate(
                    """
					insert into vets (first_name, last_name) values (:firstName, :lastName)

					""".trimIndent()
                )
                    .bindBean(vet)
                    .executeAndReturnGeneratedKeys("id")
                    .mapToMap().one()
                vet.id = id["id"] as Int?
            }
            null
        }

        return vet
    }

    fun save(specialty: Specialty): Specialty {
        jdbi.inTransaction<Any?, RuntimeException> { handle: Handle ->
            val updatedRowsCount = if (specialty.id == null) 0 else handle.createUpdate(
                """
				update specialties
				set name = :name
				where id = :id

				""".trimIndent()
            )
                .bindBean(specialty)
                .execute()
            if (updatedRowsCount == 0) {
                val id = handle.createUpdate(
                    """
					insert into specialties (name) values (:name)

					""".trimIndent()
                )
                    .bindBean(specialty)
                    .executeAndReturnGeneratedKeys("id")
                    .mapToMap().one()
                specialty.id = id["id"] as Int?
            }
            null
        }

        return specialty
    }

    fun saveSpecialtyFor(vet: Vet, specialty: Specialty) {
        jdbi.inTransaction<Int, RuntimeException> { handle: Handle ->
            if (specialty.id == null) 0 else handle.createUpdate(
                """
					insert into vet_specialties values (:vetId, :specialtyId)

					""".trimIndent()
            )
                .bind("vetId", vet.id)
                .bind("specialtyId", specialty.id)
                .execute()
        }
    }

    fun getSpecialtiesForVet(vet: Vet): List<Specialty> = jdbi.withHandle<List<Specialty>, RuntimeException> { handle: Handle ->
        handle.createQuery("select * from vet_specialties left join specialties on specialty_id = id where vet_id = :vetId")
            .bind("vetId", vet.id).mapToBean(
            Specialty::class.java
        ).list()
    }

    fun getVisitsForPets(ownerAndPetsByOwnerId: OwnerAndPets): Map<Pet, List<Visit>> = ownerAndPetsByOwnerId.pets.associateWith { findVisitsForPet(it.id) }

    private fun executeScript(script: String) {
        jdbi.withHandle<IntArray, RuntimeException> { handle: Handle -> handle.createScript(script).execute() }
    }
}
