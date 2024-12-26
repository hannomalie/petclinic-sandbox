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

import io.javalin.http.Context;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.samples.petclinic.BindingResult;
import org.springframework.samples.petclinic.system.ResponseEntity;
import org.springframework.samples.petclinic.system.Translations;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.springframework.samples.petclinic.PetClinicApplication.*;
import static org.springframework.samples.petclinic.system.Templating.htmlHeaders;
import static org.springframework.samples.petclinic.system.Templating.renderView;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
public class PetController {

	private final Database database;

	public PetController(Database database) {
		this.database = database;
	}

	public Collection<PetType> populatePetTypes() {
		return database.findPetTypes();
	}

	public Owner findOwner(int ownerId) {

		Owner owner = database.findOwnerAndPetsByOwnerId(ownerId).owner();
		if (owner == null) {
			throw new IllegalArgumentException("Owner ID not found: " + ownerId);
		}
		return owner;
	}

	public Pet findPet(int ownerId, Integer petId) {
		if (petId == null) {
			return new Pet();
		}

		OwnerAndPets ownerAndPets = database.findOwnerAndPetsByOwnerId(ownerId);
		Owner owner = ownerAndPets.owner();
		if (owner == null) {
			throw new IllegalArgumentException("Owner ID not found: " + ownerId);
		}
		return ownerAndPets.pets().stream().filter(it -> it.getId().equals(petId)).findFirst().orElse(new Pet());
	}

//	@InitBinder("owner")
//	public void initOwnerBinder(WebDataBinder dataBinder) {
//		dataBinder.setDisallowedFields("id");
//	}

//	@InitBinder("pet")
//	public void initPetBinder(WebDataBinder dataBinder) {
//		dataBinder.setValidator(new PetValidator());
//	}

	public void initCreationForm(Context ctx) {
		var model = new ModelMap();
		int ownerId = getOwnerIdFromPath(ctx);
		var owner = database.findOwnerAndPetsByOwnerId(ownerId).owner();

		List<PetType> types = database.findPetTypes();
		model.put("types", types);
		Pet pet = new Pet();
		pet.setBirthDate(LocalDate.now());
		pet.setType(types.get(0));
		model.put("pet", pet);
		model.put("owner", owner);

		setResponse(ctx, new ResponseEntity<>(renderView("pets/createOrUpdatePetForm", model, null), htmlHeaders, HttpStatus.OK_200));
	}

	public void processCreationForm(Context ctx) {
		var locale = ctx.req().getLocale();
		var model = new ModelMap();
		int ownerId = getOwnerIdFromPath(ctx);
		var owner = database.findOwnerAndPetsByOwnerId(ownerId).owner();
		var pet = getPetFromForm(ctx, database, ownerId);
		var result = BindingResult.validate(pet);
		PetValidator.validate(pet, result);

		var persistedPets = database.findOwnerAndPetsByOwnerId(owner.getId()).pets();
		if(persistedPets.stream().anyMatch(it -> it.getName().equals(pet.getName()))) {
			result.addError("name", "already exists");
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.addError("birthDate", Translations.get("typeMismatch.birthDate", locale));
		}

		model.put("pet", pet);
		List<PetType> types = database.findPetTypes();
		model.put("types", types);
		model.put("owner", owner);
		if (result.hasErrors()) {
			model.put("pet", pet);
			setResponse(ctx, new ResponseEntity<>(renderView("pets/createOrUpdatePetForm", model, result), htmlHeaders, HttpStatus.OK_200));
		} else {
			database.save(pet);
			model.put("message", "New Pet has been Added");
			List<Pet> pets = database.findOwnerAndPetsByOwnerId(owner.getId()).pets();
			model.put("pets", pets);
			model.addAttribute("visits", getVisitsForPets(pets));
			setResponse(ctx, new ResponseEntity<>(renderView("owners/ownerDetails", model, result), htmlHeaders, HttpStatus.OK_200));
		}
	}

	public void initUpdateForm(Context ctx) {
		var model = new ModelMap();
		int ownerId = getOwnerIdFromPath(ctx);
		var owner = database.findOwnerAndPetsByOwnerId(ownerId).owner();
		int petId = Integer.parseInt(ctx.pathParam("petId"));

		Pet pet = database.findPetById(petId);
		model.put("pet", pet);
		List<PetType> types = database.findPetTypes();
		model.put("types", types);

		var headers = new HashMap<String, String>();
		headers.put("Content-Type", "text/html");
		setResponse(ctx, new ResponseEntity<>(renderView("pets/createOrUpdatePetForm", model, null), headers, HttpStatus.OK_200));
	}

	public void processUpdateForm(@NotNull Context ctx) {
		var locale = ctx.req().getLocale();
		var model = new ModelMap();
		int ownerId = getOwnerIdFromPath(ctx);
		var pet = getPetFromForm(ctx, database, ownerId);
		var owner = database.findOwnerAndPetsByOwnerId(ownerId).owner();
		var result = BindingResult.validate(pet);

		String petName = pet.getName();

		// checking if the pet name already exist for the owner
		if (StringUtils.hasText(petName)) {
			var persistedPets = database.findOwnerAndPetsByOwnerId(owner.getId()).pets();
			if(persistedPets.stream().anyMatch(it -> it.getName().equals(pet.getName()))) {
				result.addError("name", Translations.get("duplicate", locale));
			}
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.addError("birthDate", Translations.get("typeMismatch.birthDate", locale));
		}

		if (result.hasErrors()) {
			model.put("pet", pet);
			List<PetType> types = database.findPetTypes();
			model.put("types", types);
			var headers = new HashMap<String, String>();
			headers.put("Content-Type", "text/html");

			setResponse(ctx, new ResponseEntity<>(renderView("pets/createOrUpdatePetForm", model, result), headers, HttpStatus.OK_200));
		} else {
			database.save(pet);
			model.put("message", "Pet details has been edited");
			model.put("owner", owner);
			List<Pet> pets = database.findOwnerAndPetsByOwnerId(owner.getId()).pets();
			model.put("pets", pets);
			model.put("visits", getVisitsForPets(pets));

			setResponse(ctx, new ResponseEntity<>(renderView("owners/ownerDetails", model, result), htmlHeaders, HttpStatus.OK_200));
		}
	}

	private HashMap<Pet, List<Visit>> getVisitsForPets(List<Pet> pets) {
		var result = new HashMap<Pet, List<Visit>>();
		for (Pet pet : pets) {
			result.put(pet, database.findVisitsForPet(pet.getId()));
		}
		return result;
	}
	private static @NotNull Pet getPetFromForm(Context ctx, Database database, Integer ownerId) {
		var pet = new Pet();
		var idString = ctx.formParam("id");
		if (idString != null && !idString.isEmpty()) {
			pet.setId(Integer.parseInt(idString));
		}
		pet.setName(ctx.formParam("name"));
		pet.setType(database.findPetTypes().stream().filter(it -> it.getName().equalsIgnoreCase(ctx.formParam("type"))).findFirst().orElse(null));
		pet.setBirthDate(LocalDate.parse(ctx.formParam("birthDate")));
		pet.setOwnerId(ownerId);
		return pet;
	}

}
