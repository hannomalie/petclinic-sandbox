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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test class for {@link VisitController}
 *
 * @author Colin But
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@DisabledInNativeImage
@DisabledInAotMode
class VisitControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	@LocalServerPort
	private int port;

	@ServiceConnection
	@Container
	static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

	@Test
	void testInitNewVisitForm() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/visits/new"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();
		Assertions.assertEquals(200, httpResponse.get().statusCode());
		assertThat(httpResponse.get().body().toString()).containsIgnoringWhitespaces("<h2>New Visit</h2>");
	}

	@Test
	void testProcessNewVisitFormSuccess() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/visits/new"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.ofString("name=George&description=Visit%20Description"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();
		Assertions.assertEquals(302, httpResponse.get().statusCode());
		assertTrue(httpResponse.get().headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessNewVisitFormHasErrors() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/pets/" + TEST_PET_ID + "/visits/new"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.ofString("name=George"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();
		Assertions.assertEquals(200, httpResponse.get().statusCode());
		org.assertj.core.api.Assertions.assertThat(httpResponse.get().body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Description", "</label>", "</div>");
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
