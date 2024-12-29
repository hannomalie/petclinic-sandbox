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
package org.springframework.samples.petclinic.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.samples.petclinic.BaseAppTest
import org.springframework.samples.petclinic.owner.*
import org.springframework.samples.petclinic.system.Page
import org.springframework.samples.petclinic.system.Pageable
import org.springframework.samples.petclinic.system.Pageable.unpaged
import org.springframework.samples.petclinic.vet.Specialty
import org.springframework.samples.petclinic.vet.Vet
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.util.function.Function

/**
 * Integration test of the Service and the Repository layer.
 *
 *
 * ClinicServiceSpringDataJpaTests subclasses benefit from the following services provided
 * by the Spring TestContext Framework:
 *
 *
 *  * **Spring IoC container caching** which spares us unnecessary set up
 * time between test execution.
 *  * **Dependency Injection** of test fixture instances, meaning that we
 * don't need to perform application context lookups. See the use of
 * [@Autowired][Autowired] on the ` ` instance variable, which uses
 * autowiring *by type*.
 *  * **Transaction management**, meaning each test method is executed in
 * its own transaction, which is automatically rolled back by default. Thus, even if tests
 * insert or otherwise change database state, there is no need for a teardown or cleanup
 * script.
 *  * An [ApplicationContext][org.springframework.context.ApplicationContext] is
 * also inherited and can be used for explicit bean lookup if necessary.
 *
 *
 * @author Ken Krebs
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Dave Syer
 */
@Testcontainers(disabledWithoutDocker = true)
internal class ClinicServiceTests : BaseAppTest(container) {
    var pageable: Pageable? = unpaged()

    @Test
    fun shouldFindOwnersByLastName() {
        createOwner("Mark", "Davis", "Foostread", "Manchaster", "0123456789")
        createOwner("Kurt", "Davis", "Foostread", "Manchaster", "0123456789")
        var owners = database.findByLastName("Davis", pageable)
        assertThat(owners.content).hasSize(2)

        owners = database.findByLastName("Daviss", pageable)
        assertThat(owners.content).isEmpty()
    }

    @Test
    fun shouldFindSingleOwnerWithPet() {
        val ownerAndPets = createOwnerAndPets()
        val owner = ownerAndPets.owner
        val pets = ownerAndPets.pets
        assertThat(owner.lastName).startsWith("Franklin")
        assertThat(pets).hasSize(1)
        assertThat(pets[0].type).isNotNull()
        assertThat(pets[0].type!!.name).isEqualTo("Bird")
    }

    @Test
    fun shouldInsertOwner() {
        var owner = Owner()
        owner.firstName = "Sam"
        owner.lastName = "Schultz"
        owner.address = "4, Evans Street"
        owner.city = "Wollongong"
        owner.telephone = "4444444444"
        database.save(owner)
        assertThat(owner.id).isNotZero()

        val owners = database.findByLastName("Schultz", pageable)
        assertThat(owners.totalElements).isEqualTo(1)
    }

    @Test
    fun shouldUpdateOwner() {
        val ownerAndPets = createOwnerAndPets()
        var owner = ownerAndPets.owner
        val oldLastName = owner.lastName
        val newLastName = oldLastName + "X"

        owner.lastName = newLastName
        database.save(owner)

        // retrieving new name from database
        database.findOwnerAndPetsByOwnerId(owner.id!!)!!.owner
        assertThat(owner.lastName).isEqualTo(newLastName)
    }

    @Test
    fun shouldFindAllPetTypes() {
        val petTypes: Collection<PetType> = database.findPetTypes()

        val petType1 = database.findPetTypes().stream().filter { it: PetType -> it.id == 1 }.findFirst().get()
        assertThat(petType1.name).isEqualTo("Cat")
        val petType4 = database.findPetTypes().stream().filter { it: PetType -> it.id == 4 }.findFirst().get()
        assertThat(petType4.name).isEqualTo("Snake")
    }

    @Test
    fun shouldInsertPetIntoDatabaseAndGenerateId() {
        val owner6BeforeAndPets = createOwnerAndPets()
        val ownerBefore = owner6BeforeAndPets.owner
        val found = owner6BeforeAndPets.pets.size

        val newPet = Pet()
        newPet.name = "bowser"
        val types: Collection<PetType> = database.findPetTypes()
        newPet.type = database.findPetTypes().stream().filter { it: PetType -> it.id == 2 }.findFirst().get()
        newPet.birthDate = LocalDate.now()
        newPet.ownerId = ownerBefore.id

        database.save(newPet)

        val ownerAfterAndPets = database.findOwnerAndPetsByOwnerId(ownerBefore.id!!)
        val ownerAfter = ownerAfterAndPets!!.owner
        assertThat(ownerAfterAndPets.pets).hasSize(found + 1)
        // checks that id has been generated
        assertThat(ownerAfterAndPets.pets.stream().filter { it: Pet -> it.name == "bowser" }
            .findFirst().get().id).isNotNull()
    }

    @Test
    fun shouldUpdatePetName() {
        val ownerAndPetsBefore = createOwnerAndPets()
        val pets = ownerAndPetsBefore.pets
        var pet = pets[0]
        val oldName = pet.name

        val newName = oldName + "X"
        pet.name = newName
        database.save(pet)

        val ownerAndPets =
            database.findOwnerAndPetsByOwnerId(ownerAndPetsBefore.owner.id!!)!!.pets
        pet = ownerAndPets[0]
        assertThat(pet.name).isEqualTo(newName)
    }

    @Test
    fun shouldFindVets() {
        val specialty0 = database.save(Specialty().apply {
            name = "dentistry"
        })
        val specialty1 = database.save(Specialty().apply {
            name = "surgery"
        })
        val vet = database.save(Vet().apply {
            firstName = "Arthur"
            lastName = "Douglas"
        })
        database.saveSpecialtyFor(vet, specialty0)
        database.saveSpecialtyFor(vet, specialty1)
        val vets: Collection<Vet> = database.findAllVets()

        assertThat(vet.lastName).isEqualTo("Douglas")
        val specialties = database.getSpecialtiesForVet(vet)
        assertThat(specialties).hasSize(2)
        assertThat(specialties[0].name).isEqualTo("dentistry")
        assertThat(specialties[1].name).isEqualTo("surgery")
    }

    @Test
    fun shouldAddNewVisitForPet() {
        val ownerAndPets = createOwnerAndPets()
        val owner6 = ownerAndPets.owner
        val pet7 = ownerAndPets.pets[0]
        val visits = database.findVisitsForPet(pet7.id)
        val found = visits.size
        val visit = Visit()
        visit.description = "test"

        database.save(visit, pet7.id!!)

        val visitsAfter = database.findVisitsForPet(pet7.id)
        assertThat(visitsAfter) //
            .hasSize(found + 1) //
            .allMatch { value: Visit -> value.id != null }
    }

    @Test
    fun shouldFindVisitsByPetId() {
        val ownerAndPets = createOwnerAndPets()
        val pet = ownerAndPets.pets.stream().findFirst().get()
        database.save(Visit(), pet.id!!)
        database.save(Visit(), pet.id!!)

        val visits: Collection<Visit> = database.findVisitsForPet(pet.id)

        assertThat(visits).hasSize(2)
        assertThat(visits.first().date).isNotNull()
    }

    companion object {
        @Container
        var container = PostgreSQLContainer("pgvector/pgvector:pg16").withDatabaseName("petclinic")
    }
}
