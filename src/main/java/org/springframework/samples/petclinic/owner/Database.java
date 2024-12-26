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
package org.springframework.samples.petclinic.owner;

import org.jdbi.v3.core.Jdbi;
import org.springframework.samples.petclinic.PetClinicApplication;
import org.springframework.samples.petclinic.system.Page;
import org.springframework.samples.petclinic.system.Pageable;
import org.springframework.samples.petclinic.vet.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.samples.petclinic.PetClinicApplication.getHikariDataSource;

/**
 * Repository class for <code>Owner</code> domain objects All method names are compliant
 * with Spring Data naming conventions so this interface can easily be extended for Spring
 * Data. See:
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 */
public class Database {
	public DatabaseType getType() {
		return type;
	}

	public void createTables() {
		var folderName = switch (getType()) {
			case H2 -> "h2";
			case MySQL -> "mysql";
			case Postgres -> "postgres";
		};
		try {
			executeScript(new String(PetClinicApplication.class.getResourceAsStream("/db/" + folderName + "/schema.sql").readAllBytes()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void createPetTypes() {
		try {
			executeScript(new String(PetClinicApplication.class.getResourceAsStream("/db/pet_types_data.sql").readAllBytes()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public enum DatabaseType {
		H2, MySQL, Postgres
	}

	private final DatabaseType type;
	private final Jdbi jdbi;

	public Database(DataSource dataSource, DatabaseType type) {
		this.jdbi = Jdbi.create(dataSource);
		this.type = type;
	}
	public Database() {
		this(getHikariDataSource("jdbc:h2:mem:testdb", "sa", "password"), DatabaseType.H2);
	}

	/**
	 * Retrieve all {@link PetType}s from the data store.
	 * @return a Collection of {@link PetType}s.
	 */
	public List<PetType> findPetTypes() {
		return jdbi.inTransaction(handle -> handle.createQuery("SELECT * FROM types ORDER BY name")
			.mapToBean(PetType.class)
			.map(it -> {
				String capitalized = it.getName().substring(0, 1).toUpperCase() + it.getName().substring(1);
				it.setName(capitalized);
				return it;
			})
			.list());
	}

	/**
	 * Retrieve {@link Owner}s from the data store by last name, returning all owners
	 * whose last name <i>starts</i> with the given name.
	 * @param lastName Value to search for
	 * @return a Collection of matching {@link Owner}s (or an empty Collection if none
	 * found)
	 */

	public Page<OwnerAndPets> findByLastName(String lastName, Pageable pageable) {
		List<OwnerAndPets> results = jdbi.inTransaction(handle -> {
			var owners = handle.createQuery("SELECT DISTINCT * FROM owners WHERE last_name LIKE CONCAT(:lastName, '%')")
				.bind("lastName", lastName).mapToBean(Owner.class).list();
			return owners.stream().map(owner -> {
				var pets = handle.createQuery("SELECT * FROM pets WHERE owner_id = :id")
					.bind("id", owner.getId())
					.mapToBean(Pet.class).list();
				return new OwnerAndPets(owner, pets);
			}).toList();
		});

		return new Page<>(results, pageable);
	}

	/**
	 * Retrieve an {@link Owner} from the data store by id.
	 * @param id the id to search for
	 * @return the {@link Owner} if found
	 */
	public OwnerAndPets findOwnerAndPetsByOwnerId(Integer id) {
		 return jdbi.inTransaction(handle -> {
			var optionalOwner = handle.createQuery("SELECT * FROM owners WHERE id = :id").bind("id", id).mapToBean(Owner.class).findFirst();
			if(optionalOwner.isPresent()) {
				var owner = optionalOwner.get();
				var pets = handle.createQuery("SELECT pets.*, types.id as pet_type_id, types.name as pet_type_name FROM pets LEFT JOIN types on pets.type_id = types.id WHERE owner_id = :id")
					.bind("id", id)
					.mapToMap()
					.list().stream().map(it -> {
						var pet = new Pet();
						var petId = it.get("id");
						if(petId instanceof Integer) {
							pet.setId((Integer) petId);
						} else if(petId instanceof Long) {
							pet.setId(Math.toIntExact((Long) petId));
						} else {
							throw new IllegalArgumentException("petId type not supported: " + petId.getClass());
						}
						pet.setType(new PetType() {{
							var petTypeId = it.get("pet_type_id");
							if(petTypeId instanceof Integer) {
								setId((Integer) petTypeId);
							} else if(petTypeId instanceof Long) {
								setId(Math.toIntExact((Long) petTypeId));
							} else {
								throw new IllegalArgumentException("petId type not supported: " + petTypeId.getClass());
							}
							setName((String) it.get("pet_type_name"));
						}});
						var ownerId = it.get("owner_id");
						if(ownerId instanceof Integer) {
							pet.setOwnerId((Integer) ownerId);
						} else if(ownerId instanceof Long) {
							pet.setOwnerId(Math.toIntExact((Long) ownerId));
						} else {
							throw new IllegalArgumentException("petId type not supported: " + ownerId.getClass());
						}
						pet.setName((String) it.get("name"));
						pet.setBirthDate(((Date) it.get("birth_date")).toLocalDate());
						return pet;
					}).toList();
				return new OwnerAndPets(owner, pets);
			} else {
				return null;
			}
		});
	}
	public List<Visit> findVisitsForPet(Integer petId) {
		return jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM visits WHERE pet_id =:id")
			.bind("id", petId)
			.map((rs, ctx) -> {
				var visit = new Visit();
				visit.setId(rs.getInt("id"));
				visit.setDescription(rs.getString("description"));
				visit.setDate(rs.getDate("visit_date").toLocalDate());

				return visit;
				})
			.list());
	}

	/**
	 * Save an {@link Owner} to the data store, either inserting or updating it.
	 * @param owner the {@link Owner} to save
	 */
	public Owner save(Owner owner) {
		jdbi.inTransaction(handle -> {
			var updatedRowsCount = owner.getId() == null ? 0 : handle.createUpdate("""
				update owners
				set first_name = :firstName, last_name = :lastName, address = :address, city = :city, telephone = :telephone
				where id = :id
				""")
				.bindBean(owner)
				.execute();
			if(updatedRowsCount == 0) {
				Map<String, Object> id = handle.createUpdate("""
						insert into owners (first_name, last_name, address, city, telephone) values (:firstName, :lastName, :address, :city, :telephone)
						""")
					.bindBean(owner)
					.executeAndReturnGeneratedKeys("id")
					.mapToMap().one();
				var idObject = id.values().stream().findFirst().get();
				if (idObject instanceof BigInteger) {
					owner.setId(((BigInteger) idObject).intValue());
				} else if (idObject instanceof Integer) {
					owner.setId((Integer) idObject);
				}  else if (idObject instanceof Long) {
					owner.setId(((Long) idObject).intValue());
				} else {
					throw new IllegalStateException("Don't understand id value: " + idObject);
				}
			}
			return null;
		});
		return owner;
	}

	/**
	 * Returns all the owners from data store
	 **/
	public Page<Owner> findAll(Pageable pageable) {
		return new Page<>(jdbi.inTransaction(handle -> handle.createQuery(
				"SELECT owner FROM Owner owner")
			.mapToBean(Owner.class)
			.list()));
	}

	public void clear() {
		jdbi.inTransaction(handle -> {
			handle.createUpdate("delete from visits;").execute();
			handle.createUpdate("delete from pets;").execute();
			handle.createUpdate("delete from owners;").execute();
			handle.createUpdate("delete from vet_specialties;").execute();
			handle.createUpdate("delete from vets;").execute();
			return null;
		});
	}

	public Pet save(Pet pet) {
		jdbi.inTransaction(handle -> {
			var updatedRowsCount = pet.getId() == null ? 0 : handle.createUpdate("update pets " +
																				 "set name = :name, birth_date = :birthDate, type_id = :typeId, owner_id = :ownerId " +
																				 "where id = :id ")
				.bindBean(pet)
				.bind("typeId", pet.getType().getId())
				.execute();
			if(updatedRowsCount == 0) {
				var id = handle.createUpdate("insert into pets (name, birth_date, type_id, owner_id) values (:name, :birthDate, :typeId, :ownerId)\n")
					.bindBean(pet)
					.bind("typeId", pet.getType().getId())
					.executeAndReturnGeneratedKeys("id")
					.mapToMap().one();
				pet.setId((Integer) id.get("id"));
			}
			return null;
		});
		return pet;
	}
	public Visit save(Visit visit, Integer petId) {
		jdbi.inTransaction(handle -> {
			var updatedRowsCount = visit.getId() == null ? 0 : handle.createUpdate("""
				update visits
				set pet_id = :petId, visit_date = :date, description = :description
				where id = :id
				""")
				.bindBean(visit)
				.bind("petId", petId)
				.execute();
			if(updatedRowsCount == 0) {
				var id = handle.createUpdate("""
					insert into visits (pet_id, visit_date, description) values (:petId, :date, :description)
					""")
					.bindBean(visit)
					.bind("petId", petId)
					.executeAndReturnGeneratedKeys("id")
					.mapToMap().one();
				visit.setId((Integer) id.get("id"));
			}
			return null;
		});

		return visit;
	}
	public void save(List<Pet> pets) {
		for (Pet pet : pets) {
			save(pet);
		}
	}

	public List<Vet> findAllVets() {
		return jdbi.withHandle(handle -> handle.createQuery("select * from vets").mapToBean(Vet.class).list());
	}
	public Page<Vet> findAllVetsPageable(Pageable pageable) {
		List<Vet> allVets = findAllVets();
		return new Page<>(allVets, pageable);
	}

	public Pet findPetById(int petId) {
		return jdbi.withHandle(handle -> {
			Map<String, Object> petOrNull = handle.createQuery("select pets.*, types.id as _type_id, types.name as _type_name from pets left join types on pets.type_id = types.id where pets.id = :id ")
				.bind("id", petId).mapToMap().findFirst().orElse(null);
			if(petOrNull != null) {
				var pet = new Pet();
				pet.setId((Integer) petOrNull.get("id"));
				pet.setOwnerId((Integer) petOrNull.get("owner_id"));
				pet.setBirthDate(((Date) petOrNull.get("birth_date")).toLocalDate());
				pet.setName((String) petOrNull.get("name"));
				pet.setType(new PetType() {{
					setId((Integer) petOrNull.get("_type_id"));
					setName((String) petOrNull.get("_type_name"));
				}});
				return pet;
			} else {
				return null;
			}
		});
	}

	public Vet save(Vet vet) {
		jdbi.inTransaction(handle -> {
			var updatedRowsCount = vet.getId() == null ? 0 : handle.createUpdate("""
				update vets
				set first_name = :firstName, last_name = :lastName
				where id = :id
				""")
				.bindBean(vet)
				.execute();
			if(updatedRowsCount == 0) {
				var id = handle.createUpdate("""
					insert into vets (first_name, last_name) values (:firstName, :lastName)
					""")
					.bindBean(vet)
					.executeAndReturnGeneratedKeys("id")
					.mapToMap().one();
				vet.setId((Integer) id.get("id"));
			}
			return null;
		});

		return vet;
	}

	public Specialty save(Specialty specialty) {
		jdbi.inTransaction(handle -> {
			var updatedRowsCount = specialty.getId() == null ? 0 : handle.createUpdate("""
				update specialties
				set name = :name
				where id = :id
				""")
				.bindBean(specialty)
				.execute();
			if(updatedRowsCount == 0) {
				var id = handle.createUpdate("""
					insert into specialties (name) values (:name)
					""")
					.bindBean(specialty)
					.executeAndReturnGeneratedKeys("id")
					.mapToMap().one();
				specialty.setId((Integer) id.get("id"));
			}
			return null;
		});

		return specialty;
	}

	public void saveSpecialtyFor(Vet vet, Specialty specialty) {
		jdbi.inTransaction(handle -> {
			return specialty.getId() == null ? 0 : handle.createUpdate("""
					insert into vet_specialties values (:vetId, :specialtyId)
					""")
				.bind("vetId", vet.getId())
				.bind("specialtyId", specialty.getId())
				.execute();
		});
	}

	public List<Specialty> getSpecialtiesForVet(Vet vet) {
		return jdbi.withHandle(handle -> handle.createQuery("select * from vet_specialties left join specialties on specialty_id = id where vet_id = :vetId").bind("vetId", vet.getId()).mapToBean(Specialty.class).list());
	}

	public HashMap<Pet, List<Visit>> getVisitsForPets(OwnerAndPets ownerAndPetsByOwnerId) {
		var result = new HashMap<Pet, List<Visit>>();
		for (Pet pet : ownerAndPetsByOwnerId.pets()) {
			result.put(pet, findVisitsForPet(pet.getId()));
		}
		return result;
	}

	public void executeScript(String script) {
		jdbi.withHandle(handle -> handle.createScript(script).execute());
	}
}
