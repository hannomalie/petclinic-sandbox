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

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.system.Templating;
import org.springframework.samples.petclinic.system.Translations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.springframework.samples.petclinic.system.Templating.htmlHeaders;
import static org.springframework.samples.petclinic.system.Templating.renderView;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Controller
class OwnerController {

	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";

	private final Database database;

	public OwnerController(Database database) {
		this.database = database;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable(name = "ownerId", required = false) Integer ownerId) {
		OwnerAndPets ownerAndPetsByOwnerId = database.findOwnerAndPetsByOwnerId(ownerId);
		return ownerId == null ? new Owner() : (ownerAndPetsByOwnerId == null ? new Owner() : ownerAndPetsByOwnerId.owner());
	}
	@ModelAttribute("pets")
	public List<Pet> findPets(@PathVariable(name = "ownerId", required = false) Integer ownerId) {
		OwnerAndPets ownerAndPetsByOwnerId = database.findOwnerAndPetsByOwnerId(ownerId);
		return ownerId == null ? new ArrayList<>() : (ownerAndPetsByOwnerId == null ? new ArrayList<>() : ownerAndPetsByOwnerId.pets());
	}
	@ModelAttribute("visits")
	public Map<Integer,List<Visit>> findVisits(@PathVariable(name = "ownerId", required = false) Integer ownerId) {
		OwnerAndPets ownerAndPetsByOwnerId = database.findOwnerAndPetsByOwnerId(ownerId);
		if (ownerId == null) return new HashMap<>();
		if (ownerAndPetsByOwnerId == null) return new HashMap<>();
		return getVisitsForPets(ownerAndPetsByOwnerId);
	}

	private HashMap<Integer, List<Visit>> getVisitsForPets(OwnerAndPets ownerAndPetsByOwnerId) {
		var result = new HashMap<Integer, List<Visit>>();
		for (Pet pet : ownerAndPetsByOwnerId.pets()) {
			result.put(pet.getId(), database.findVisitsForPet(pet.getId()));
		}
		return result;
	}

	@GetMapping("/owners/new")
	public ResponseEntity<String> initCreationForm(Map<String, Object> model) {
		Owner owner = new Owner();
		model.put("owner", owner);
		return new ResponseEntity<>(renderView(VIEWS_OWNER_CREATE_OR_UPDATE_FORM, model, null), htmlHeaders, HttpStatus.OK);
	}

	@PostMapping("/owners/new")
	public ResponseEntity<String> processCreationForm(@Valid Owner owner, BindingResult result,
									  RedirectAttributes redirectAttributes, ModelMap modelMap) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in creating the owner.");

			return new ResponseEntity<>(renderView(VIEWS_OWNER_CREATE_OR_UPDATE_FORM, modelMap, result), htmlHeaders, HttpStatus.OK);
		}

		owner = database.save(owner);
		redirectAttributes.addFlashAttribute("message", "New Owner Created");
		HttpHeaders htmlHeaders = new HttpHeaders();
		htmlHeaders.add("Location", "/owners/" + owner.getId());
		return new ResponseEntity<>(showOwner(owner.getId(), modelMap).getBody(), htmlHeaders, HttpStatus.MOVED_TEMPORARILY);
	}

	@GetMapping("/owners/find")
	public ResponseEntity<String> initFindForm(ModelMap modelMap) {
		return new ResponseEntity<>(renderView("owners/findOwners", modelMap, null), htmlHeaders, HttpStatus.OK);
	}

	@GetMapping("/owners")
	public ResponseEntity<String> processFindForm(@RequestParam(defaultValue = "1") int page,
												  Owner owner, BindingResult result,
												Model model, ModelMap modelMap, 	Locale locale) {
		// allow parameterless GET request for /owners to return all records
		if (owner.getLastName() == null) {
			owner.setLastName(""); // empty string signifies broadest possible search
		}

		// find owners by last name
		Page<OwnerAndPets> ownersResults = findPaginatedForOwnersLastName(page, owner.getLastName());
		if (ownersResults.isEmpty()) {
			// no owners found
			result.rejectValue("lastName", "notFound", Translations.get("notFound", locale));
			return new ResponseEntity<>(renderView("owners/findOwners", modelMap, result), htmlHeaders, HttpStatus.OK);
		}

		if (ownersResults.getTotalElements() == 1) {
			// 1 owner found
			var foundOwner = ownersResults.iterator().next().owner();
			HttpHeaders htmlHeaders = new HttpHeaders();
			htmlHeaders.add("Location", "/owners/" + foundOwner.getId());
			return new ResponseEntity<>(showOwner(foundOwner.getId(), modelMap).getBody(), htmlHeaders, HttpStatus.MOVED_TEMPORARILY);
		}

		// multiple owners found
		return addPaginationModel(page, modelMap, ownersResults);
	}

	private ResponseEntity<String> addPaginationModel(int page, ModelMap modelMap, Page<OwnerAndPets> paginated) {
		List<OwnerAndPets> listOwners = paginated.getContent();
		modelMap.addAttribute("currentPage", page);
		modelMap.addAttribute("totalPages", paginated.getTotalPages());
		modelMap.addAttribute("totalItems", paginated.getTotalElements());
		modelMap.addAttribute("listOwners", listOwners);
		var petsForOwnerId = new HashMap<>();
		var owners = new ArrayList<>();
		for (OwnerAndPets ownerAndPets : paginated.getContent()) {
			petsForOwnerId.put(ownerAndPets.owner().getId(), ownerAndPets.pets());
			owners.add(ownerAndPets.owner());
		}
		modelMap.addAttribute("petsForOwnerId", petsForOwnerId);
		modelMap.addAttribute("owners", owners);
		HttpHeaders htmlHeaders = new HttpHeaders();
		htmlHeaders.add("Location", "/owners");
		return new ResponseEntity<>(renderView("owners/ownersList", modelMap, null), htmlHeaders, HttpStatus.MOVED_TEMPORARILY);
	}

	private Page<OwnerAndPets> findPaginatedForOwnersLastName(int page, String lastname) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return database.findByLastName(lastname, pageable);
	}

	@GetMapping("/owners/{ownerId}/edit")
	public ResponseEntity<String> initUpdateOwnerForm(@PathVariable("ownerId") int ownerId, ModelMap model) {
		Owner owner = this.database.findOwnerAndPetsByOwnerId(ownerId).owner();
		model.addAttribute("owner", owner);
		return new ResponseEntity<>(renderView(VIEWS_OWNER_CREATE_OR_UPDATE_FORM, model, null), htmlHeaders, HttpStatus.OK);
	}

	@PostMapping("/owners/{ownerId}/edit")
	public ResponseEntity<String> processUpdateOwnerForm(@Valid Owner owner, BindingResult result, @PathVariable("ownerId") int ownerId,
			RedirectAttributes redirectAttributes, ModelMap model) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in updating the owner.");
			return new ResponseEntity<>(renderView(VIEWS_OWNER_CREATE_OR_UPDATE_FORM, model, result), htmlHeaders, HttpStatus.OK);
		}

		owner.setId(ownerId);
		this.database.save(owner);
		redirectAttributes.addFlashAttribute("message", "Owner Values Updated");

		HttpHeaders htmlHeaders = new HttpHeaders();
		htmlHeaders.add("Location", "/owners/" + ownerId);
		// TODO: Return empty body?
		return new ResponseEntity<>(renderView("owners/ownerDetails", model, result), htmlHeaders, HttpStatus.MOVED_TEMPORARILY);
	}

	/**
	 * Custom handler for displaying an owner.
	 * @param ownerId the ID of the owner to display
	 * @return a ModelMap with the model attributes for the view
	 */
	@GetMapping("/owners/{ownerId}")
	public ResponseEntity<String> showOwner(@PathVariable("ownerId") int ownerId, ModelMap model) {
		OwnerAndPets ownerAndPets = this.database.findOwnerAndPetsByOwnerId(ownerId);
		Owner owner = ownerAndPets.owner();
		model.addAttribute("owner", owner);
		model.addAttribute("pets", ownerAndPets.pets());
		model.addAttribute("visits", getVisitsForPets(ownerAndPets));
		return new ResponseEntity<>(renderView("owners/ownerDetails", model, null), htmlHeaders, HttpStatus.OK);
	}

}
