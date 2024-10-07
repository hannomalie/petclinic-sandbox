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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.samples.petclinic.BaseSpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link PetController}
 *
 * @author Colin But
 */
@Testcontainers(disabledWithoutDocker = true)
class PetControllerTests extends BaseSpringBootTest {

	private static final int TEST_OWNER_ID = 1;
	private static final int TEST_PET_ID = 1;

	@ServiceConnection
	@Container
	static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

	@Test
	void testInitCreationForm() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/new");

		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsIgnoringWhitespaces("<h2>New Pet</h2>");
	}


	@Test
	void testProcessCreationFormSuccess() throws Exception {
		var httpResponse = postForm(
			"http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/new",
			"name=Betty&type=hamster&birthDate=2015-02-12"
		);

		assertEquals(302, httpResponse.statusCode());
		assertTrue(httpResponse.headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessCreationFormHasErrors() throws Exception {
		var httpResponse = postForm(
			"http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/new",
			"name=Betty&birthDate=2015-02-12"
		);

		assertEquals(200, httpResponse.statusCode());
		Assertions.assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Type", "</label>", "</div>");
	}

	@Test
	void testInitUpdateForm() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/edit");

		assertEquals(200, httpResponse.statusCode());
		Assertions.assertThat(httpResponse.body().toString()).containsIgnoringWhitespaces("<button class=\"btn btn-primary\" type=\"submit\">Update Pet</button>");
	}

	@Test
	void testProcessUpdateFormSuccess() throws Exception {
		var httpResponse = postForm(
			"http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/edit",
			"name=BettyZZZ&type=hamster&birthDate=2015-02-12"
		);

		assertEquals(302, httpResponse.statusCode());
		assertTrue(httpResponse.headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	@Disabled("I don't know how to exactly resemble that case")
	void testProcessUpdateFormHasErrors() throws Exception {
		var httpResponse = postForm(
			"http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/edit",
			"id=&name=Betty&birthDate=2015-02-12"
		);

		assertEquals(200, httpResponse.statusCode());
		Assertions.assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Pet", "</label>", "</div>");
//		mockMvc
//			.perform(post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID).param("name", "Betty")
//				.param("birthDate", "2015/02/12"))
//			.andExpect(model().attributeHasNoErrors("owner"))
//			.andExpect(model().attributeHasErrors("pet"))
//			.andExpect(status().isOk())
//			.andExpect(view().name("pets/createOrUpdatePetForm"));
	}

	@DynamicPropertySource
	static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
		registerDataSourceProperties(registry, container);
	}

}
