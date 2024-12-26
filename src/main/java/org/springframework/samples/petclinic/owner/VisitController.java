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

import java.time.LocalDate;
import java.util.HashMap;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.samples.petclinic.BindingResult;
import org.springframework.samples.petclinic.system.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;

import static org.springframework.samples.petclinic.PetClinicApplication.*;
import static org.springframework.samples.petclinic.system.Templating.htmlHeaders;
import static org.springframework.samples.petclinic.system.Templating.renderView;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@Controller
public class VisitController {

	private final Database database;

	public VisitController(Database database) {
		this.database = database;
	}

//	@InitBinder
//	public void setAllowedFields(WebDataBinder dataBinder) {
//		dataBinder.setDisallowedFields("id");
//	}

	public void initNewVisitForm(Context ctx) {
		var modelMap = new ModelMap();
		int ownerId = getOwnerIdFromPath(ctx);
		int petId = Integer.parseInt(ctx.pathParam("petId"));

		OwnerAndPets ownerAndPets = database.findOwnerAndPetsByOwnerId(ownerId);
		var owner = ownerAndPets.owner();
		var pets = ownerAndPets.pets();

		var pet = pets.stream().filter(it -> it.getId().equals(petId)).findFirst().get();

		modelMap.put("pet", pet);
		modelMap.put("owner", owner);
		var visitsForPet = new HashMap<>();
		visitsForPet.put(pet, database.findVisitsForPet(pet.getId()));
		modelMap.addAttribute("visitsForPet", visitsForPet);
		modelMap.put("visit", new Visit());

		setResponse(ctx, new ResponseEntity<>(renderView("pets/createOrUpdateVisitForm", modelMap, null), htmlHeaders, HttpStatus.OK.getCode()));
	}

	public void processNewVisitForm(Context ctx) {
		var model = new ModelMap();
		int ownerId = getOwnerIdFromPath(ctx);
		var owner = database.findOwnerAndPetsByOwnerId(ownerId).owner();
		int petId = Integer.parseInt(ctx.pathParam("petId"));
		var visit = getVisitFromForm(ctx);
		var result = BindingResult.validate(visit);

		OwnerAndPets ownerAndPets = database.findOwnerAndPetsByOwnerId(owner.getId());
		var pets = ownerAndPets.pets();

		model.put("pet", pets.stream().filter(it -> it.getId().equals(petId)).findFirst().get());
		if (result.hasErrors()) {
			var pet = pets.stream().filter(it -> it.getId().equals(petId)).findFirst().get();

			model.put("visit", visit);
			var visitsForPet = new HashMap<>();
			visitsForPet.put(pet, database.findVisitsForPet(pet.getId()));
			model.addAttribute("visitsForPet", visitsForPet);
			setResponse(ctx, new ResponseEntity<>(renderView("pets/createOrUpdateVisitForm", model, result), htmlHeaders, HttpStatus.OK.getCode()));
		} else {
			database.save(visit, petId);
			model.put("message", "Your visit has been booked");

			model.put("owner", owner);
			model.put("pets", pets);
			model.put("visits", database.getVisitsForPets(ownerAndPets));
			setResponse(ctx, new ResponseEntity<>(renderView("owners/ownerDetails", model, null), htmlHeaders, HttpStatus.OK.getCode()));
		}
	}

	public static @NotNull Visit getVisitFromForm(Context ctx) {
		var visit = new Visit();
		var idString = ctx.formParam("id");
		if (idString != null && !idString.isEmpty()) {
			visit.setId(Integer.parseInt(idString));
		}
		visit.setDescription(ctx.formParam("description"));
		String dateString = ctx.formParam("date");
		if (dateString != null && !dateString.isEmpty()) {
			visit.setDate(LocalDate.parse(dateString));
		}
		return visit;
	}
}
