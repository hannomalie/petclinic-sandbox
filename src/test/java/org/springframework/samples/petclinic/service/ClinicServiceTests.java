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

package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.BaseSpringBootTest;
import org.springframework.samples.petclinic.owner.*;
import org.springframework.samples.petclinic.system.Page;
import org.springframework.samples.petclinic.system.Pageable;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.Vet;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test of the Service and the Repository layer.
 * <p>
 * ClinicServiceSpringDataJpaTests subclasses benefit from the following services provided
 * by the Spring TestContext Framework:
 * </p>
 * <ul>
 * <li><strong>Spring IoC container caching</strong> which spares us unnecessary set up
 * time between test execution.</li>
 * <li><strong>Dependency Injection</strong> of test fixture instances, meaning that we
 * don't need to perform application context lookups. See the use of
 * {@link Autowired @Autowired} on the <code> </code> instance variable, which uses
 * autowiring <em>by type</em>.
 * <li><strong>Transaction management</strong>, meaning each test method is executed in
 * its own transaction, which is automatically rolled back by default. Thus, even if tests
 * insert or otherwise change database state, there is no need for a teardown or cleanup
 * script.
 * <li>An {@link org.springframework.context.ApplicationContext ApplicationContext} is
 * also inherited and can be used for explicit bean lookup if necessary.</li>
 * </ul>
 *
 * @author Ken Krebs
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Dave Syer
 */
@Testcontainers(disabledWithoutDocker = true)
class ClinicServiceTests extends BaseSpringBootTest {


	Pageable pageable = Pageable.unpaged();

	@Container
	static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("pgvector/pgvector:pg16").withDatabaseName("petclinic");

	ClinicServiceTests() {
		super(container);
	}
	@Test
	void shouldFindOwnersByLastName() {
		createOwner("Mark", "Davis", "Foostread", "Manchaster", "0123456789");
		createOwner("Kurt", "Davis", "Foostread", "Manchaster", "0123456789");
		Page<OwnerAndPets> owners = this.database.findByLastName("Davis", pageable);
		assertThat(owners.getContent()).hasSize(2);

		owners = this.database.findByLastName("Daviss", pageable);
		assertThat(owners.getContent()).isEmpty();
	}

	@Test
	void shouldFindSingleOwnerWithPet() {
		OwnerAndPets ownerAndPets = createOwnerAndPets();
		Owner owner = ownerAndPets.owner();
		var pets = ownerAndPets.pets();
		assertThat(owner.getLastName()).startsWith("Franklin");
		assertThat(pets).hasSize(1);
		assertThat(pets.get(0).getType()).isNotNull();
		assertThat(pets.get(0).getType().getName()).isEqualTo("Bird");
	}

	@Test
	void shouldInsertOwner() {
		Page<OwnerAndPets> owners = this.database.findByLastName("Schultz", pageable);
		int found = (int) owners.getTotalElements();

		Owner owner = new Owner();
		owner.setFirstName("Sam");
		owner.setLastName("Schultz");
		owner.setAddress("4, Evans Street");
		owner.setCity("Wollongong");
		owner.setTelephone("4444444444");
		owner = this.database.save(owner);
		assertThat(owner.getId()).isNotZero();

		owners = this.database.findByLastName("Schultz", pageable);
		assertThat(owners.getTotalElements()).isEqualTo(found + 1);
	}

	@Test
	void shouldUpdateOwner() {
		var ownerAndPets = createOwnerAndPets();
		Owner owner = ownerAndPets.owner();
		String oldLastName = owner.getLastName();
		String newLastName = oldLastName + "X";

		owner.setLastName(newLastName);
		owner = this.database.save(owner);

		// retrieving new name from database
		owner = this.database.findOwnerAndPetsByOwnerId(owner.getId()).owner();
		assertThat(owner.getLastName()).isEqualTo(newLastName);
	}

	@Test
	void shouldFindAllPetTypes() {
		Collection<PetType> petTypes = this.database.findPetTypes();

		PetType petType1 = database.findPetTypes().stream().filter(it -> it.getId() == 1).findFirst().get();
		assertThat(petType1.getName()).isEqualTo("Cat");
		PetType petType4 = database.findPetTypes().stream().filter(it -> it.getId() == 4).findFirst().get();
		assertThat(petType4.getName()).isEqualTo("Snake");
	}

	@Test
	void shouldInsertPetIntoDatabaseAndGenerateId() {
		OwnerAndPets owner6BeforeAndPets = createOwnerAndPets();
		Owner ownerBefore = owner6BeforeAndPets.owner();
		int found = owner6BeforeAndPets.pets().size();

		Pet newPet = new Pet();
		newPet.setName("bowser");
		Collection<PetType> types = this.database.findPetTypes();
		newPet.setType(database.findPetTypes().stream().filter(it -> it.getId().equals(2)).findFirst().get());
		newPet.setBirthDate(LocalDate.now());
		newPet.setOwnerId(ownerBefore.getId());

		this.database.save(newPet);

		var ownerAfterAndPets = this.database.findOwnerAndPetsByOwnerId(ownerBefore.getId());
		var ownerAfter = ownerAfterAndPets.owner();
		assertThat(ownerAfterAndPets.pets()).hasSize(found + 1);
		// checks that id has been generated
		assertThat(ownerAfterAndPets.pets().stream().filter(it -> it.getName().equals("bowser")).findFirst().get().getId()).isNotNull();
	}

	@Test
	void shouldUpdatePetName() {
		OwnerAndPets ownerAndPetsBefore = createOwnerAndPets();
		var pets = ownerAndPetsBefore.pets();
		Pet pet = pets.get(0);
		String oldName = pet.getName();

		String newName = oldName + "X";
		pet.setName(newName);
		this.database.save(pet);

		var ownerAndPets = this.database.findOwnerAndPetsByOwnerId(ownerAndPetsBefore.owner().getId()).pets();
		pet = ownerAndPets.get(0);
		assertThat(pet.getName()).isEqualTo(newName);
	}

	@Test
	void shouldFindVets() {
		var specialty0 = database.save(new Specialty() {{
			setName("dentistry");
		}});
		var specialty1 = database.save(new Specialty() {{
			setName("surgery");
		}});
		var vet = database.save(new Vet() {{
			setFirstName("Arthur");
			setLastName("Douglas");
		}});
		database.saveSpecialtyFor(vet, specialty0);
		database.saveSpecialtyFor(vet, specialty1);
		Collection<Vet> vets = this.database.findAllVets();

		assertThat(vet.getLastName()).isEqualTo("Douglas");
		List<Specialty> specialties = database.getSpecialtiesForVet(vet);
		assertThat(specialties).hasSize(2);
		assertThat(specialties.get(0).getName()).isEqualTo("dentistry");
		assertThat(specialties.get(1).getName()).isEqualTo("surgery");
	}

	@Test
	void shouldAddNewVisitForPet() {
		var ownerAndPets = createOwnerAndPets();
		Owner owner6 = ownerAndPets.owner();
		Pet pet7 = ownerAndPets.pets().get(0);
		var visits = database.findVisitsForPet(pet7.getId());
		int found = visits.size();
		Visit visit = new Visit();
		visit.setDescription("test");

		database.save(visit, pet7.getId());

		var visitsAfter = database.findVisitsForPet(pet7.getId());
		assertThat(visitsAfter) //
			.hasSize(found + 1) //
			.allMatch(value -> value.getId() != null);
	}

	@Test
	void shouldFindVisitsByPetId() {
		var ownerAndPets = createOwnerAndPets();
		var pet = ownerAndPets.pets().stream().findFirst().get();
		database.save(new Visit(), pet.getId());
		database.save(new Visit(), pet.getId());

		Collection<Visit> visits = database.findVisitsForPet(pet.getId());

		assertThat(visits) //
			.hasSize(2) //
			.element(0)
			.extracting(it -> it.getDate())
			.isNotNull();
	}
}
