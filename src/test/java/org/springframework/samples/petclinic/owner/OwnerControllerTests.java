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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test class for {@link OwnerController}
 *
 * @author Colin But
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerControllerTests {

	private static final int TEST_OWNER_ID = 1;

	@ServiceConnection
	@Container
	static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

	@LocalServerPort
	private int port;

	@Test
	void testInitCreationForm() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/new"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertTrue(httpResponse.get().body().toString().contains("<form class=\"form-horizontal\" id=\"add-owner-form\" method=\"post\">"));
	}

	@Test
	void testProcessCreationFormSuccess() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/new"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.ofString("firstName=Joe&lastName=Bloggs&address=123%20Caramel%20Street&city=London&telephone=1316761638"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(302, httpResponse.get().statusCode());
		assertThat(httpResponse.get().headers().firstValue("location").get()).contains("/owners/" + TEST_OWNER_ID);
	}

	@Test
	void testProcessCreationFormHasErrors() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/new"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.ofString("firstName=Joe&lastName=Bloggs&city=London"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		assertThat(httpResponse.get().body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Address", "</label>", "</div>");
		assertThat(httpResponse.get().body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Telephone", "</label>", "</div>");
	}

	@Test
	void testInitFindForm() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/find"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		assertTrue(httpResponse.get().body().toString().contains("<h2>Find Owners</h2>"));
	}

	@Test
	void testProcessFindFormSuccess() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners?page=1"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		assertTrue(httpResponse.get().body().toString().contains("<h2>Owners</h2>"));
	}

	@Test
	void testProcessFindFormByLastName() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners?page=1&lastName=Franklin"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(302, httpResponse.get().statusCode());
		assertTrue(httpResponse.get().headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessFindFormNoOwnersFound() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners?page=1&lastName=Unknown%20Surname"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		assertThat(httpResponse.get().body().toString()).containsIgnoringWhitespaces("<div class=\"col-sm-10\"><input class=\"form-control\" size=\"30\" maxlength=\"80\" id=\"lastName\" name=\"lastName\" value=\"Unknown Surname\" /> <span class=\"help-inline\"><div><p>wurde nicht gefunden</p></div>");
	}

	@Test
	void testInitUpdateOwnerForm() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/edit"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		assertTrue(httpResponse.get().body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"lastName\" name=\"lastName\" value=\"Franklin\" />"));
		assertTrue(httpResponse.get().body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"firstName\" name=\"firstName\" value=\"George\" />"));
		assertTrue(httpResponse.get().body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"address\" name=\"address\" value=\"110 W. Liberty St.\" />"));
		assertTrue(httpResponse.get().body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"city\" name=\"city\" value=\"Madison\" />"));
		assertTrue(httpResponse.get().body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"telephone\" name=\"telephone\" value=\"6085551023\" />"));
	}

	@Test
	void testProcessUpdateOwnerFormSuccess() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/edit"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.ofString("firstName=Joe&lastName=Bloggs&address=123%20Caramel%20Street&city=London&telephone=1616291589"))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(302, httpResponse.get().statusCode());
		assertTrue(httpResponse.get().headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessUpdateOwnerFormUnchangedSuccess() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/edit"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.noBody())
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(302, httpResponse.get().statusCode());
		assertTrue(httpResponse.get().headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessUpdateOwnerFormHasErrors() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/edit"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", HttpRequest.BodyPublishers.ofString("firstName=Joe&lastName=Bloggs&address=&telephone="))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		assertThat(httpResponse.get().body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Address", "</label>", "</div>");
		assertThat(httpResponse.get().body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Telephone", "</label>", "</div>");
	}

	@Test
	void testShowOwner() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/owners/" + TEST_OWNER_ID))
			.build();
		AtomicReference<HttpResponse<?>> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(httpResponse::set)
			.join();

		assertEquals(200, httpResponse.get().statusCode());
		assertThat(httpResponse.get().body().toString()).containsSubsequence("<tr>","<th>","Name","</th>","<td>","<b>","George Franklin","</b>","</td>","</tr>","<tr>","<th>","Address","</th>","<td>","110 W. Liberty St.","</td>","</tr>","<tr>","<th>","City","</th>","<td>","Madison","</td>","</tr>","<tr>","<th>","Telephone","</th>","<td>","6085551023","</td>","</tr>");
		assertThat(httpResponse.get().body().toString()).containsSubsequence("<thead>","<tr>","<th>","Visit Date","</th>","<th>","Description","</th>","</tr>","</thead>","<tr>","<td>","2024-10-10","</td>","<td>","</td>","</tr>");
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
