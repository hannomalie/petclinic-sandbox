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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.BaseSpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link VisitController}
 *
 * @author Colin But
 */
@Testcontainers(disabledWithoutDocker = true)
class VisitControllerTests extends BaseSpringBootTest {

	@Container
	static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

	public VisitControllerTests() {
		super(container);
	}
	@Test
	void testInitNewVisitForm() throws Exception {
		var ownerAndPets = createOwnerAndPets();
		var httpResponse = get("http://localhost:" + port + "/owners/" + ownerAndPets.owner().getId() + "/pets/" + ownerAndPets.pets().get(0).getId() + "/visits/new");

		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsIgnoringWhitespaces("<h2>New Visit</h2>");
	}

	@Test
	void testProcessNewVisitFormSuccess() throws Exception {
		var ownerAndPets = createOwnerAndPets();
		var httpResponse = postForm("http://localhost:" + port + "/owners/" + ownerAndPets.owner().getId() + "/pets/" + ownerAndPets.pets().stream().findFirst().get().getId() + "/visits/new",
			"name=George&description=Visit%20Description");

		assertEquals(200, httpResponse.statusCode());
	}

	@Test
	void testProcessNewVisitFormHasErrors() throws Exception {
		var ownerAndPets = createOwnerAndPets();
		var httpResponse = postForm("http://localhost:" + port + "/owners/" + ownerAndPets.owner().getId() + "/pets/" + ownerAndPets.pets().stream().findFirst().get().getId() + "/visits/new",
			"name=George");
		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Description", "</label>", "</div>");
	}
}
