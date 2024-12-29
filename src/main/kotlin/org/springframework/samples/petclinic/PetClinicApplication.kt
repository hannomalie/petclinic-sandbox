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
package org.springframework.samples.petclinic

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.staticfiles.Location
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.samples.petclinic.owner.*
import org.springframework.samples.petclinic.system.Database.DatabaseType
import org.springframework.samples.petclinic.system.CrashController
import org.springframework.samples.petclinic.system.Database
import org.springframework.samples.petclinic.system.ResponseEntity
import org.springframework.samples.petclinic.system.WelcomeController
import org.springframework.samples.petclinic.vet.VetController

/**
 * PetClinic Spring Boot Application.
 *
 * @author Dave Syer
 */
@ImportRuntimeHints(PetClinicRuntimeHints::class)
object PetClinicApplication {
    fun main(args: Array<String>) {
        startApplication(8080, "jdbc:h2:mem:testdb", "sa", "password", DatabaseType.H2)
    }

    fun startApplication(
        port: Int,
        jdbcUrl: String,
        username: String,
        password: String,
        databaseType: DatabaseType
    ): Javalin {
        val ds = getHikariDataSource(jdbcUrl, username, password)
        val database = Database(ds, databaseType)
        return startApplication(port, database)
    }

    fun startApplication(port: Int, database: Database): Javalin {
        database.createTables()
        database.createPetTypes()
//		database.executeScript(new String(PetClinicApplication.class.getResourceAsStream("/db/h2/data.sql").readAllBytes()));

        val objectMapper = ObjectMapper()
        val vetController = VetController(database, objectMapper)
        val welcomeController = WelcomeController()
        val crashController = CrashController()
        val ownerController = OwnerController(database)
        val petController = PetController(database)
        val visitController = VisitController(database)

        val app = Javalin.create { javalinConfig ->
                javalinConfig.staticFiles.enableWebjars()
                javalinConfig.staticFiles.add { staticFileConfig ->
                    staticFileConfig.location = Location.CLASSPATH
                    staticFileConfig.directory = "static/resources"
                    staticFileConfig.hostedPath = "resources/"
                }
            }
            .get("/owners", ownerController::processFindForm)
            .get("/owners/new", ownerController::initCreationForm)
            .get("/owners/find", ownerController::initFindForm)
            .get("/owners/{ownerId}", ownerController::showOwner)
            .get("/owners/{ownerId}/edit", ownerController::initUpdateOwnerForm)
            .get("/owners/{ownerId}/pets/{petId}/edit", petController::initUpdateForm)
            .get("/owners/{ownerId}/pets/{petId}/new", petController::initCreationForm)
            .get("/owners/{ownerId}/pets/{petId}/visits/new", visitController::initNewVisitForm)
            .get("/owners/{ownerId}/pets/new", petController::initCreationForm)
            .get("/vets", vetController::showVets)
            .get("/find", ownerController::initFindForm)
            .get("/oups", crashController::triggerException)
            .get("/", welcomeController::welcome)
            .post("/owners/new", ownerController::processCreationForm)
            .post("/owners/{ownerId}/edit", ownerController::processUpdateOwnerForm)
            .post("/owners/{ownerId}/pets/{petId}/edit", petController::processUpdateForm)
            .post("/owners/{ownerId}/pets/{petId}/visits/new", visitController::processNewVisitForm)
            .post("/owners/{ownerId}/pets/new") { ctx: Context -> petController.processCreationForm(ctx) }
            .start(port)

        return app
    }

    fun getHikariDataSource(
        jdbcUrl: String,
        username: String,
        password: String
    ): HikariDataSource = HikariDataSource(HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        this.username = username
        this.password = password
        addDataSourceProperty("cachePrepStmts", "true")
        addDataSourceProperty("prepStmtCacheSize", "250")
        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    })

	fun Context.getPageParamOrDefault(): Int {
        val page = queryParam("page") ?: "1"
        return page.toInt()
    }

	fun Context.getOwnerIdFromPath(): Int = pathParam("ownerId").toInt()

    fun Context.getOwnerFromForm(): Owner = Owner().apply {
        val idString = formParam("id")
        if (!idString.isNullOrEmpty()) {
            id = idString.toInt()
        }
        telephone = formParam("telephone")
        city = formParam("city")
        firstName = formParam("firstName")
        lastName = formParam("lastName")
        address = formParam("address")
    }

	fun Context.setResponse(response: ResponseEntity<String>) {
        status(response.code)
        for ((key, value) in response.headers) {
            header(key, value)
        }
        result(response.body)
    }
}
