package org.springframework.samples.petclinic

import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.BeforeEach
import org.springframework.samples.petclinic.PetClinicApplication.getHikariDataSource
import org.springframework.samples.petclinic.PetClinicApplication.startApplication
import org.springframework.samples.petclinic.owner.*
import org.springframework.samples.petclinic.system.Database
import org.springframework.samples.petclinic.system.Database.DatabaseType
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.util.List

open class BaseAppTest(
    ds: HikariDataSource = getHikariDataSource("jdbc:h2:mem:testdb", "sa", "password"),
    databaseType: DatabaseType = DatabaseType.H2,
    port: Int = 0
) {
    val database = Database(ds, databaseType)
    val app = startApplication(port, database)
    val port = app.port()

    constructor(container: MySQLContainer<*>): this(getHikariDataSource(container.jdbcUrl, container.username, container.password), DatabaseType.MySQL)
    constructor(container: PostgreSQLContainer<*>): this(getHikariDataSource(container.jdbcUrl, container.username, container.password), DatabaseType.Postgres)

    @BeforeEach
    fun beforeEach() {
        database.clear()
    }

	val client: HttpClient = HttpClient.newHttpClient()

    fun get(uri: String): HttpResponse<*> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun get(uri: String, accept: String): HttpResponse<*> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("Accept", accept)
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun postForm(uri: String, body: String): HttpResponse<*> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .method(
                "POST",
                HttpRequest.BodyPublishers.ofString(body)
            )
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    //	 Normally we need to close client, but we can't make it static now and also can't use LifeCycle api of JUnit
    //	because of how SpringBootTests work ....
    //	@AfterAll
    //	static void afterAll() {
    //		client.close();
    //	}
    fun createOwner(
        _firstName: String, _lastName: String, _address: String, _city: String, _telephone: String
    ): Owner {
        val owner = Owner().apply {
                firstName = _firstName
                lastName = _lastName
                address = _address
                city = _city
                telephone = _telephone
        }
        database.save(owner)
        return owner
    }

    fun createPet(_ownerId: Int, _name: String, _type: PetType): Pet {
        val pet: Pet = Pet().apply {
            ownerId = _ownerId
            name = _name
            type = _type
            birthDate = LocalDate.of(1988, 11, 5)
        }
        database.save(pet)
        return pet
    }

    fun createVisit(_petId: Int, _date: LocalDate): Visit {
        val visit: Visit = Visit().apply {
            date = _date
        }
        database.save(visit, _petId)
        return visit
    }

    fun createGeorge(): Owner {
        val george = createOwner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023")
        val milo = createPet(george.id!!, "Milo", database.findPetTypes()[0])
        val visit = createVisit(milo.id!!, LocalDate.of(2023, 10, 2))
        return george
    }

    // TODO: Collapse with above helper?
    fun createOwnerAndPets(): OwnerAndPets {
        val george = createOwner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023")
        return OwnerAndPets(george, List.of(createPet(george.id!!, "Milo", database.findPetTypes()[0])))
    }
}
