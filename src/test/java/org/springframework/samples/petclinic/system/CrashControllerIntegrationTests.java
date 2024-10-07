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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration Test for {@link CrashController}.
 *
 * @author Alex Lutz
 */
// NOT Waiting https://github.com/spring-projects/spring-boot/issues/5574
@SpringBootTest(webEnvironment = RANDOM_PORT,
		properties = { "server.error.include-message=ALWAYS", "management.endpoints.enabled-by-default=false" })
class CrashControllerIntegrationTests {

	@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
	static class TestConfiguration {

	}

	@Value(value = "${local.server.port}")
	private int port;

	@Autowired
	private TestRestTemplate rest;

	@Test
	void testTriggerExceptionJson() {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/oups"))
			.build();
		AtomicReference<HttpResponse> httpResponse = new AtomicReference<>();
		AtomicReference<Map<String, Object>> json = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept((response) -> {
				httpResponse.set(response);
				ObjectMapper mapper = new ObjectMapper();
				try {
					json.set(mapper.readValue(response.body(), HashMap.class));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			})
			.join();

		assertEquals(500, httpResponse.get().statusCode());
		assertNotNull(json.get().get("timestamp"));
		assertNotNull(json.get().get("status"));
		assertNotNull(json.get().get("error"));
		assertEquals("Expected: controller used to showcase what happens when an exception is thrown", json.get().get("message"));
		assertEquals("/oups", json.get().get("path"));
	}

	@Test
	void testTriggerExceptionHtml() {

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/oups"))
			.header("Accept", "text/html")
			.build();
		AtomicReference<HttpResponse> httpResponse = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept((response) -> {
				httpResponse.set(response);
			})
			.join();

		assertEquals(500, httpResponse.get().statusCode());
		assertThat(httpResponse.get().body().toString()).containsSubsequence("<body>", "<h2>", "Something happened...", "</h2>", "<p>",
				"Expected:", "controller", "used", "to", "showcase", "what", "happens", "when", "an", "exception", "is",
				"thrown", "</p>", "</body>");
		assertThat(httpResponse.get().body().toString()).doesNotContain("Whitelabel Error Page",
				"This application has no explicit mapping for");

	}

}
