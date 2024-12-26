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

package org.springframework.samples.petclinic.system;

import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.BaseSpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration Test for {@link CrashController}.
 *
 * @author Alex Lutz
 */
// NOT Waiting https://github.com/spring-projects/spring-boot/issues/5574
@Testcontainers(disabledWithoutDocker = true)
class CrashControllerIntegrationTests extends BaseSpringBootTest {

	@Container
	static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("pgvector/pgvector:pg16").withDatabaseName("petclinic");

	public CrashControllerIntegrationTests() {
		super(container);
	}
	@Test
	void testTriggerExceptionJson() throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/oups"))
			.header("Accept", "application/json")
			.build();
		var httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		var json = new ObjectMapper().readValue(httpResponse.body().toString(), HashMap.class);

		assertEquals(500, httpResponse.statusCode());
		assertNotNull(json.get("timestamp"));
		assertNotNull(json.get("status"));
		assertNotNull(json.get("error"));
		assertEquals("Expected: controller used to showcase what happens when an exception is thrown", json.get("message"));
		assertEquals("/oups", json.get("path"));
	}

	@Test
	void testTriggerExceptionHtml() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/oups", "text/html");


		assertEquals(500, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsSubsequence("<body>", "<h2>", "Something happened...", "</h2>", "<p>",
				"Expected:", "controller", "used", "to", "showcase", "what", "happens", "when", "an", "exception", "is",
				"thrown", "</p>", "</body>");
		assertThat(httpResponse.body().toString()).doesNotContain("Whitelabel Error Page",
				"This application has no explicit mapping for");

	}
}
