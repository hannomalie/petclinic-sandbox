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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.samples.petclinic.BaseSpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link VisitController}
 *
 * @author Colin But
 */
@Testcontainers(disabledWithoutDocker = true)
class VisitControllerTests extends BaseSpringBootTest {

	private static final int TEST_OWNER_ID = 1;
	private static final int TEST_PET_ID = 1;

	@ServiceConnection
	@Container
	static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

	@Test
	void testInitNewVisitForm() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/visits/new");

		Assertions.assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsIgnoringWhitespaces("<h2>New Visit</h2>");
	}

	@Test
	void testProcessNewVisitFormSuccess() throws Exception {
		var httpResponse = postForm("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/visits/new",
			"name=George&description=Visit%20Description");

		Assertions.assertEquals(302, httpResponse.statusCode());
		assertTrue(httpResponse.headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessNewVisitFormHasErrors() throws Exception {
		var httpResponse = postForm("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/visits/new",
			"name=George");
		Assertions.assertEquals(200, httpResponse.statusCode());
		org.assertj.core.api.Assertions.assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Description", "</label>", "</div>");
	}

	@DynamicPropertySource
	static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
		registerDataSourceProperties(registry, container);
	}
}
