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

package org.springframework.samples.petclinic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class MySqlIntegrationTests extends BaseSpringBootTest {

	@ServiceConnection
	@Container
	static MySQLContainer<?> container = new MySQLContainer<>("mysql:9.0");

	@Autowired
	private VetRepository vets;

	@Test
	void testFindAll() throws Exception {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void testOwnerDetails() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners/1");
		assertThat(httpResponse.statusCode()).isEqualTo(200);
	}

	@DynamicPropertySource
	static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
		registerDataSourceProperties(registry, container);
	}
}
