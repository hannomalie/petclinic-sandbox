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

package org.springframework.samples.petclinic.vet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.samples.petclinic.BaseSpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Test class for the {@link VetController}
 */

@Testcontainers(disabledWithoutDocker = true)
class VetControllerTests extends BaseSpringBootTest {

	@ServiceConnection
	@Container
	static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("pgvector/pgvector:pg16").withDatabaseName("petclinic");

	private Vet james() {
		Vet james = new Vet();
		james.setFirstName("James");
		james.setLastName("Carter");
		james.setId(1);
		return james;
	}

	private Vet helen() {
		Vet helen = new Vet();
		helen.setFirstName("Helen");
		helen.setLastName("Leary");
		helen.setId(2);
		Specialty radiology = new Specialty();
		radiology.setId(1);
		radiology.setName("radiology");
		helen.addSpecialty(radiology);
		return helen;
	}

	@BeforeEach
	void setup() {
		database.save(james());
		database.save(helen());
	}

	@Test
	void testShowVetListHtml() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/vets?page=1", "text/html");

		assertThat(httpResponse.body().toString()).contains("<title>PetClinic :: a Spring Framework demonstration</title>");
	}

	@Test
	void testShowResourcesVetList() throws Exception {
		var vet = database.save(new Vet() {{
			setFirstName("James");
			setLastName("Carter");
		}});
		var httpResponse = get("http://localhost:" + port + "/vets", "application/json");
		var json = new ObjectMapper().readValue(httpResponse.body().toString(), HashMap.class);

		var vetList = (List<HashMap<String, Object>>) json.get("vetList");
		var expectedVetListEntry = new HashMap<String, Object>();
		expectedVetListEntry.put("id", vet.getId());
		expectedVetListEntry.put("firstName", "James");
		expectedVetListEntry.put("lastName", "Carter");
		expectedVetListEntry.put("specialties", new ArrayList<String>());
		expectedVetListEntry.put("nrOfSpecialties", 0);
		expectedVetListEntry.put("new", false);
		Assertions.assertEquals(expectedVetListEntry, vetList.get(0));
	}

	@DynamicPropertySource
	static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
		registerDataSourceProperties(registry, container);
	}
}
