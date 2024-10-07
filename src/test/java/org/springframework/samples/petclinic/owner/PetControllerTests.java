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
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test class for the {@link PetController}
 *
 * @author Colin But
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@DisabledInNativeImage
@DisabledInAotMode
class PetControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	@LocalServerPort
	private int port;

	@ServiceConnection
	@Container
	static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

	@Test
	void testInitCreationForm() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/new"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		assertThat(httpResponse.get().body().toString()).containsIgnoringWhitespaces("<h2>New Pet</h2>");
	}

	@Test
	void testProcessCreationFormSuccess() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/new"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.ofString("name=Betty&type=hamster&birthDate=2015-02-12"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(302, httpResponse.get().statusCode());
		assertTrue(httpResponse.get().headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessCreationFormHasErrors() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/new"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.ofString("name=Betty&birthDate=2015-02-12"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		Assertions.assertThat(httpResponse.get().body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Type", "</label>", "</div>");
	}

	@Test
	void testInitUpdateForm() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/edit"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		Assertions.assertThat(httpResponse.get().body().toString()).containsIgnoringWhitespaces("<button class=\"btn btn-primary\" type=\"submit\">Update Pet</button>");
	}

	@Test
	void testProcessUpdateFormSuccess() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/edit"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.ofString("name=BettyZZZ&type=hamster&birthDate=2015-02-12"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(302, httpResponse.get().statusCode());
		assertTrue(httpResponse.get().headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	@Disabled("I don't know how to exactly resemble that case")
	void testProcessUpdateFormHasErrors() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/edit"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.ofString("id=&name=Betty&birthDate=2015-02-12")) // I had to pass in empty id, although it wasn't necessary before
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		Assertions.assertThat(httpResponse.get().body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Pet", "</label>", "</div>");
//		mockMvc
//			.perform(post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID).param("name", "Betty")
//				.param("birthDate", "2015/02/12"))
//			.andExpect(model().attributeHasNoErrors("owner"))
//			.andExpect(model().attributeHasErrors("pet"))
//			.andExpect(status().isOk())
//			.andExpect(view().name("pets/createOrUpdatePetForm"));
	}

	@DynamicPropertySource
	static void registerPgProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url",
			() -> String.format("jdbc:postgresql://localhost:%d/petclinic", container.getFirstMappedPort()));
		registry.add("spring.datasource.username", () -> "petclinic");
		registry.add("spring.datasource.password", () -> "petclinic");
		registry.add("spring.sql.init.mode", () -> "always");
		registry.add("database", () -> "postgres");
	}

}
