package org.springframework.samples.petclinic;

import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.samples.petclinic.owner.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static org.springframework.samples.petclinic.PetClinicApplication.getHikariDataSource;
import static org.springframework.samples.petclinic.PetClinicApplication.startApplication;

public class BaseSpringBootTest {

	protected int port;

	protected Database database;

	// TODO: Clean constructor mess up
	public BaseSpringBootTest() {
		try {
			HikariDataSource ds = getHikariDataSource("jdbc:h2:mem:testdb", "sa", "password");
			database = new Database(ds, Database.DatabaseType.H2);
			var app = startApplication(0, database);
			port = app.port();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public BaseSpringBootTest(HikariDataSource ds, Database.DatabaseType databaseType) {
		try {
			database = new Database(ds, databaseType);
			var app = startApplication(0, database);
			port = app.port();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public BaseSpringBootTest(MySQLContainer<?> container) {
		try {
			HikariDataSource ds = getHikariDataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword());
			database = new Database(ds, Database.DatabaseType.MySQL);
			var app = startApplication(0, database);
			port = app.port();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public BaseSpringBootTest(PostgreSQLContainer<?> container) {
		try {
			HikariDataSource ds = getHikariDataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword());
			database = new Database(ds, Database.DatabaseType.Postgres);
			var app = startApplication(0, database);
			port = app.port();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeEach
	void beforeEach() {
		database.clear();
	}
	protected HttpClient client = HttpClient.newHttpClient();
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

	public Owner createOwner(
		final String _firstName, final String _lastName, final String _address, final String _city, final String _telephone
	) {
		var owner = new Owner() {{
			setFirstName(_firstName);
			setLastName(_lastName);
			setAddress(_address);
			setCity(_city);
			setTelephone(_telephone);
		}};
		return database.save(owner);
	}

	public Pet createPet(final Integer _ownerId, final String _name, final PetType _type) {
		var pet = new Pet() {{
			setOwnerId(_ownerId);
			setName(_name);
			setType(_type);
			setBirthDate(LocalDate.of(1988, 11, 5));
		}};

		return database.save(pet);
	}

	public Visit createVisit(Integer _petId, LocalDate _date) {
		Visit visit = new Visit() {{
			setDate(_date);
		}};
		return database.save(visit, _petId);
	}

	public @NotNull Owner createGeorge() {
		var george = createOwner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023");
		var milo = createPet(george.getId(), "Milo", database.findPetTypes().get(0));
		var visit = createVisit(milo.getId(), LocalDate.of(2023, 10, 2));
		return george;
	}
	// TODO: Collapse with above helper?
	public OwnerAndPets createOwnerAndPets() {
		var george = createOwner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023");
		return new OwnerAndPets(george, List.of(createPet(george.getId(), "Milo", database.findPetTypes().get(0))));
	}
}
