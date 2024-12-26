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
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.samples.petclinic.BindingResult;
import org.springframework.samples.petclinic.system.Translations;
import org.springframework.samples.petclinic.system.Page;
import org.springframework.samples.petclinic.system.PageRequest;
import org.springframework.samples.petclinic.system.Pageable;
import org.springframework.samples.petclinic.system.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;

import java.util.*;

import static org.springframework.samples.petclinic.PetClinicApplication.*;
import static org.springframework.samples.petclinic.system.Templating.htmlHeaders;
import static org.springframework.samples.petclinic.system.Templating.renderView;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Controller
public class OwnerController {

	private final Database database;

	public OwnerController(Database database) {
		this.database = database;
	}

	public void initCreationForm(@NotNull Context ctx) {
		var model = new HashMap<String, Object>();
		Owner owner = new Owner();
		model.put("owner", owner);
		var response = new ResponseEntity<>(renderView("owners/createOrUpdateOwnerForm", model, null), htmlHeaders, HttpStatus.OK.getCode());
		setResponse(ctx, response);
	}

	public void processCreationForm(@NotNull Context ctx) {
		var owner = getOwnerFromForm(ctx);
		var result = BindingResult.validate(owner);
		var modelMap = new HashMap<String, Object>();
		modelMap.put("owner", owner);
		if (result.hasErrors()) {
			modelMap.put("error", "There was an error in creating the owner.");

			setResponse(ctx, new ResponseEntity<>(renderView("owners/createOrUpdateOwnerForm", modelMap, result), htmlHeaders, HttpStatus.OK.getCode()));
		} else {
			owner = database.save(owner);
			modelMap.put("pets", database.findOwnerAndPetsByOwnerId(owner.getId()).pets());
			modelMap.put("message", "New Owner Created");
			setResponse(ctx, new ResponseEntity<>(renderView("owners/ownerDetails", modelMap, result), htmlHeaders, org.eclipse.jetty.http.HttpStatus.OK_200));
		}
	}

	public void initFindForm(Context ctx) {
		var modelMap = new ModelMap();
		var response = new ResponseEntity<>(renderView("owners/findOwners", modelMap, null), htmlHeaders, HttpStatus.OK.getCode());
		setResponse(ctx, response);
	}

	public void processFindForm(@NotNull Context ctx) {
		ResponseEntity<String> res;
		var page = getPageParamOrDefault(ctx);
		var locale = ctx.req().getLocale();
		String lastName = ctx.queryParam("lastName");
		var modelMap = new ModelMap();
		// allow parameterless GET request for /owners to return all records
		if (lastName == null) {
			lastName = ""; // empty string signifies broadest possible search
		}

		// find owners by last name
		Page<OwnerAndPets> ownersResults = findPaginatedForOwnersLastName(page, lastName);
		if (ownersResults.isEmpty()) {
			modelMap.put("lastName", lastName);
			// no owners found
			var result = new BindingResult();
			result.addError("lastName", Translations.get("notFound", locale));
			res = new ResponseEntity<>(renderView("owners/findOwners", modelMap, result), htmlHeaders, HttpStatus.OK.getCode());
		} else if (ownersResults.getTotalElements() == 1) {
			// 1 owner found
			var foundOwner = ownersResults.iterator().next().owner();

			modelMap.addAttribute("owner", foundOwner);
			OwnerAndPets ownerAndPets = database.findOwnerAndPetsByOwnerId(foundOwner.getId());
			List<Pet> pets = ownerAndPets.pets();
			modelMap.addAttribute("pets", pets);
			modelMap.addAttribute("visits", database.getVisitsForPets(ownerAndPets));
			res = new ResponseEntity<>(renderView("owners/ownerDetails", modelMap, null), htmlHeaders, HttpStatus.OK.getCode());
		} else {

			// multiple owners found
			List<OwnerAndPets> listOwners = ownersResults.getContent();
			modelMap.addAttribute("currentPage", page);
			modelMap.addAttribute("totalPages", ownersResults.getTotalPages());
			modelMap.addAttribute("totalItems", ownersResults.getTotalElements());
			modelMap.addAttribute("listOwners", listOwners);
			var petsForOwnerId = new HashMap<>();
			var owners = new ArrayList<>();
			for (OwnerAndPets ownerAndPets : ownersResults.getContent()) {
				petsForOwnerId.put(ownerAndPets.owner().getId(), ownerAndPets.pets());
				owners.add(ownerAndPets.owner());
			}
			modelMap.addAttribute("petsForOwnerId", petsForOwnerId);
			modelMap.addAttribute("owners", owners);
			res = new ResponseEntity<>(renderView("owners/ownersList", modelMap, null), htmlHeaders, org.eclipse.jetty.http.HttpStatus.OK_200);
		}
		setResponse(ctx, res);
	}

	private Page<OwnerAndPets> findPaginatedForOwnersLastName(int page, String lastname) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return database.findByLastName(lastname, pageable);
	}

	public void initUpdateOwnerForm(@NotNull Context ctx) {
		var ownerId = getOwnerIdFromPath(ctx);
		Owner owner = this.database.findOwnerAndPetsByOwnerId(ownerId).owner();
		var model = new HashMap<String, Object>();
		model.put("owner", owner);
		var response = new ResponseEntity<>(renderView("owners/createOrUpdateOwnerForm", model, null), htmlHeaders, HttpStatus.OK.getCode());
		setResponse(ctx, response);
	}

	public void processUpdateOwnerForm(Context ctx) {
		var model = new ModelMap();
		var ownerId = getOwnerIdFromPath(ctx);
		var owner = getOwnerFromForm(ctx);
		var result = BindingResult.validate(owner);
		model.put("owner", owner);
		if (result.hasErrors()) {
			model.put("error", "There was an error in updating the owner.");
			setResponse(ctx, new ResponseEntity<>(renderView("owners/createOrUpdateOwnerForm", model, result), htmlHeaders, HttpStatus.OK.getCode()));
		} else {
			owner.setId(ownerId);
			this.database.save(owner);
			model.put("message", "Owner Values Updated");
			OwnerAndPets ownerAndPets = database.findOwnerAndPetsByOwnerId(owner.getId());
			model.put("pets", ownerAndPets.pets());
			model.put("visits", database.getVisitsForPets(ownerAndPets));

			setResponse(ctx, new ResponseEntity<>(renderView("owners/ownerDetails", model, result), htmlHeaders, org.eclipse.jetty.http.HttpStatus.OK_200));
		}
	}

	/**
	 * Custom handler for displaying an owner.
	 *
	 * @param ctx
	 * @return a ModelMap with the model attributes for the view
	 */
	public void showOwner(Context ctx) {
		var ownerId = getOwnerIdFromPath(ctx);
		var model = new ModelMap();
		OwnerAndPets ownerAndPets = this.database.findOwnerAndPetsByOwnerId(ownerId);
		Owner owner = ownerAndPets.owner();
		model.addAttribute("owner", owner);
		model.addAttribute("pets", ownerAndPets.pets());
		model.addAttribute("visits", database.getVisitsForPets(ownerAndPets));
		var response = new ResponseEntity<>(renderView("owners/ownerDetails", model, null), htmlHeaders, HttpStatus.OK.getCode());
		setResponse(ctx, response);
	}

}
