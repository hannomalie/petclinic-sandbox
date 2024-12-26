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

package org.springframework.samples.petclinic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.samples.petclinic.owner.*;
import org.springframework.samples.petclinic.system.CrashController;
import org.springframework.samples.petclinic.system.WelcomeController;
import org.springframework.samples.petclinic.system.ResponseEntity;
import org.springframework.samples.petclinic.vet.VetController;

import java.io.IOException;
import java.util.*;

/**
 * PetClinic Spring Boot Application.
 *
 * @author Dave Syer
 *
 */
@ImportRuntimeHints(PetClinicRuntimeHints.class)
public class PetClinicApplication {
	public static void main(String[] args) throws IOException {
		startApplication(8080, "jdbc:h2:mem:testdb", "sa", "password", Database.DatabaseType.H2);
	}
	public static Javalin startApplication(int port, String jdbcUrl, String username, String password, Database.DatabaseType databaseType) throws IOException {
		HikariDataSource ds = getHikariDataSource(jdbcUrl, username, password);
		var database = new Database(ds, databaseType);
		return startApplication(port, database);
	}
	public static Javalin startApplication(int port, Database database) throws IOException {

		database.createTables();
		database.createPetTypes();
//		database.executeScript(new String(PetClinicApplication.class.getResourceAsStream("/db/h2/data.sql").readAllBytes()));

		var objectMapper = new ObjectMapper();
		var vetController = new VetController(database, objectMapper);
		var welcomeController = new WelcomeController();
		var crashController = new CrashController();
		var ownerController = new OwnerController(database);
		var petController = new PetController(database);
		var visitController = new VisitController(database);

		var app = Javalin.create(config -> {
				config.staticFiles.enableWebjars();
				config.staticFiles.add(staticFileConfig -> {
					staticFileConfig.location = Location.CLASSPATH;
					staticFileConfig.directory = "static/resources";
					staticFileConfig.hostedPath = "resources/";
				});
			})
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
			.post("/owners/{ownerId}/pets/new", petController::processCreationForm)
			.start(port);

		return app;
	}

	public static @NotNull HikariDataSource getHikariDataSource(String jdbcUrl, String username, String password) {
		HikariConfig hikariConfig = new HikariConfig();
		HikariDataSource ds;
		hikariConfig.setJdbcUrl(jdbcUrl);
		hikariConfig.setUsername(username);
		hikariConfig.setPassword(password);
		hikariConfig.addDataSourceProperty( "cachePrepStmts" , "true" );
		hikariConfig.addDataSourceProperty( "prepStmtCacheSize" , "250" );
		hikariConfig.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
		ds = new HikariDataSource( hikariConfig );
		return ds;
	}

	public static int getPageParamOrDefault(Context ctx) {
		var page = ctx.queryParam("page");
		if(page == null) {
			page = "1";
		}
		return Integer.parseInt(page);
	}

	public static int getOwnerIdFromPath(Context ctx) {
		return Integer.parseInt(ctx.pathParam("ownerId"));
	}

	public static @NotNull Owner getOwnerFromForm(Context ctx) {
		var owner = new Owner();
		var idString = ctx.formParam("id");
		if (idString != null && !idString.isEmpty()) {
			owner.setId(Integer.parseInt(idString));
		}
		owner.setTelephone(ctx.formParam("telephone"));
		owner.setCity(ctx.formParam("city"));
		owner.setFirstName(ctx.formParam("firstName"));
		owner.setLastName(ctx.formParam("lastName"));
		owner.setAddress(ctx.formParam("address"));
		return owner;
	}

	public static void setResponse(Context ctx, ResponseEntity<String> response) {
		ctx.status(response.code());
		for (Map.Entry<String, String> entry : response.headers().entrySet()) {
			ctx.header(entry.getKey(), entry.getValue());
		}
		ctx.result(response.body());
	}
}
