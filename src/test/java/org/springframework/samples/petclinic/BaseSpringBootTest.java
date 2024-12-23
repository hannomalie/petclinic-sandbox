package org.springframework.samples.petclinic;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.samples.petclinic.owner.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)//, args = {"--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"})
@DisabledInNativeImage
@DisabledInAotMode
public class BaseSpringBootTest {

	@LocalServerPort
	protected int port;

	@Autowired
	protected Database database;

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
//	private void initDefaultData() {
//		petTypes.add(new OwnerRepository.PetType() {{ name = "cat"; }});
//		petTypes.add(new OwnerRepository.PetType() {{ name = "dog"; }});
//		petTypes.add(new OwnerRepository.PetType() {{ name = "lizard"; }});
//		petTypes.add(new OwnerRepository.PetType() {{ name = "snake"; }});
//		petTypes.add(new OwnerRepository.PetType() {{ name = "bird"; }});
//		petTypes.add(new OwnerRepository.PetType() {{ name = "hamster"; }});
//
//		owners.add(createOwner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023"));
//		owners.add(createOwner("Betty", "Davis", "638 Cardinal Ave.", "Sun Prairie", "6085551749"));
//		owners.add(createOwner("Eduardo", "Rodriquez", "2693 Commerce St.", "McFarland", "6085558763"));
//		owners.add(createOwner("Harold", "Davis", "563 Friendly St.", "Windsor", "6085553198"));
//		owners.add(createOwner("Peter", "McTavish", "2387 S. Fair Way", "Madison", "6085552765"));
//		owners.add(createOwner("Jean", "Coleman", "105 N. Lake St.", "Monona", "6085552654"));
//		owners.add(createOwner("Jeff", "Black", "1450 Oak Blvd.", "Monona", "6085555387"));
//		owners.add(createOwner("Maria", "Escobito", "345 Maple St.", "Madison", "6085557683"));
//		owners.add(createOwner("David", "Schroeder", "2749 Blackhawk Trail", "Madison", "6085559435"));
//		owners.add(createOwner("Carlos", "Estaban", "2335 Independence La.", "Waunakee", "6085555487"));
//	}
	protected static void registerDataSourceProperties(DynamicPropertyRegistry registry, PostgreSQLContainer<?> container) {
		registry.add("spring.datasource.url",
			() -> String.format("jdbc:postgresql://localhost:%d/petclinic", container.getFirstMappedPort()));
		// does also not work with container.getJdbcUrl()
		registry.add("spring.datasource.username", () -> container.getUsername());
		registry.add("spring.datasource.password", () -> container.getPassword());
		registry.add("spring.sql.init.mode", () -> "always");
		registry.add("database", () -> "postgres");
	}
	protected static void registerDataSourceProperties(DynamicPropertyRegistry registry, MySQLContainer<?> container) {
		registry.add("spring.datasource.url",
			() -> String.format("jdbc:mysql://localhost:%d/petclinic", container.getFirstMappedPort()));
		// does also not work with container.getJdbcUrl()
		registry.add("spring.datasource.username", () -> container.getUsername());
		registry.add("spring.datasource.password", () -> container.getPassword());
		registry.add("spring.sql.init.mode", () -> "always");
		registry.add("database", () -> "mysql");
	}
	protected static void registerInMemoryDataSourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url",
			() -> String.format("jdbc:h2:mem:petclinic"));
		// does also not work with container.getJdbcUrl()
		registry.add("spring.datasource.username", () -> "sa");
		registry.add("spring.datasource.password", () -> "password");
		registry.add("spring.sql.init.mode", () -> "always");
	}
}
