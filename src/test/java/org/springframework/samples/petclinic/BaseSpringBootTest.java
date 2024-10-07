package org.springframework.samples.petclinic;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@DisabledInNativeImage
@DisabledInAotMode
public class BaseSpringBootTest {

	@LocalServerPort
	protected int port;

	HttpClient client = HttpClient.newHttpClient();
	protected HttpResponse<?> get(String uri) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uri))
			.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}
	protected HttpResponse<?> get(String uri, String accept) throws IOException, InterruptedException {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(uri))
				.header("Accept", accept)
				.build();
			return client.send(request, HttpResponse.BodyHandlers.ofString());
	}
	protected HttpResponse<?> postForm(String uri, String body) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uri))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.method("POST", body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
			.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

//	 Normally we need to close client, but we can't make it static now and also can't use LifeCycle api of JUnit
//	because of how SpringBootTests work ....
//	@AfterAll
//	static void afterAll() {
//		client.close();
//	}

	protected static void registerDataSourceProperties(DynamicPropertyRegistry registry, PostgreSQLContainer<?> container) {
		registry.add("spring.datasource.url",
			() -> String.format("jdbc:postgresql://localhost:%d/petclinic", container.getFirstMappedPort()));
		registry.add("spring.datasource.username", () -> "petclinic");
		registry.add("spring.datasource.password", () -> "petclinic");
		registry.add("spring.sql.init.mode", () -> "always");
		registry.add("database", () -> "postgres");
	}
	protected static void registerDataSourceProperties(DynamicPropertyRegistry registry, MySQLContainer<?> container) {
		registry.add("spring.datasource.url",
			() -> String.format("jdbc:mysql://localhost:%d/petclinic", container.getFirstMappedPort()));
		registry.add("spring.datasource.username", () -> "petclinic");
		registry.add("spring.datasource.password", () -> "petclinic");
		registry.add("spring.sql.init.mode", () -> "always");
		registry.add("database", () -> "mysql");
	}
}
