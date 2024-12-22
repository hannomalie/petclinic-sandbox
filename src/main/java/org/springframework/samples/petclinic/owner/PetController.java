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
import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.springframework.samples.petclinic.system.Templating.htmlHeaders;
import static org.springframework.samples.petclinic.system.Templating.renderView;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
@RequestMapping("/owners/{ownerId}")
class PetController {

	private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

	private final Database database;

	LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();

	public PetController(Database database) {
		this.database = database;
	}

	@ModelAttribute("types")
	public Collection<PetType> populatePetTypes() {
		return database.findPetTypes();
	}

	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable("ownerId") int ownerId) {

		Owner owner = database.findOwnerAndPetsByOwnerId(ownerId).owner();
		if (owner == null) {
			throw new IllegalArgumentException("Owner ID not found: " + ownerId);
		}
		return owner;
	}

	@ModelAttribute("pet")
	public Pet findPet(@PathVariable("ownerId") int ownerId, @PathVariable(name = "petId", required = false) Integer petId) {
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

	@InitBinder("owner")
	public void initOwnerBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@InitBinder("pet")
	public void initPetBinder(WebDataBinder dataBinder) {
		dataBinder.setValidator(new PetValidator());
	}

	@GetMapping("/pets/new")
	public ResponseEntity<String> initCreationForm(Owner owner, ModelMap model) {
		List<PetType> types = database.findPetTypes();
		model.put("types", types);
		Pet pet = new Pet();
		pet.setBirthDate(LocalDate.now());
		pet.setType(types.get(0));
		model.put("pet", pet);

		return new ResponseEntity<>(renderView(VIEWS_PETS_CREATE_OR_UPDATE_FORM, model, null), htmlHeaders, HttpStatus.OK);
	}

	@PostMapping("/pets/new")
	public ResponseEntity<String> processCreationForm(Owner owner, @Valid Pet pet, BindingResult result, ModelMap model,
			RedirectAttributes redirectAttributes) {
		var persistedPets = database.findOwnerAndPetsByOwnerId(owner.getId()).pets();
		if(persistedPets.stream().anyMatch(it -> it.getName().equals(pet.getName()))) {
			result.rejectValue("name", "duplicate", "already exists");
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (result.hasErrors()) {
			model.put("pet", pet);

			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", "text/html");
			return new ResponseEntity<>(renderView(VIEWS_PETS_CREATE_OR_UPDATE_FORM, model, result), headers, HttpStatus.OK);
		}

		database.save(pet);
		redirectAttributes.addFlashAttribute("message", "New Pet has been Added");
		HttpHeaders headers = new HttpHeaders();
		headers.add("location", "/owners/" + owner.getId());
		return new ResponseEntity<>("", headers, HttpStatus.MOVED_TEMPORARILY);
	}

	@GetMapping("/pets/{petId}/edit")
	public ResponseEntity<String> initUpdateForm(Owner owner, @PathVariable("petId") int petId, ModelMap model,
			RedirectAttributes redirectAttributes) {
		Pet pet = database.findPetById(petId);
		model.put("pet", pet);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "text/html");
		return new ResponseEntity<>(renderView(VIEWS_PETS_CREATE_OR_UPDATE_FORM, model, null), headers, HttpStatus.OK);
	}

	@PostMapping("/pets/{petId}/edit")
	public ResponseEntity<String> processUpdateForm(@Valid Pet pet, BindingResult result, Owner owner, ModelMap model, RedirectAttributes redirectAttributes) {
		localValidatorFactoryBean.validate(pet, result);

		String petName = pet.getName();

		// checking if the pet name already exist for the owner
		if (StringUtils.hasText(petName)) {
			var persistedPets = database.findOwnerAndPetsByOwnerId(owner.getId()).pets();
			if(persistedPets.stream().anyMatch(it -> it.getName().equals(pet.getName()))) {
				result.rejectValue("name", "duplicate", "already exists");
			}
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (result.hasErrors()) {
			model.put("pet", pet);
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", "text/html");
			return new ResponseEntity<>(renderView(VIEWS_PETS_CREATE_OR_UPDATE_FORM, model, result), headers, HttpStatus.OK);
		}

		database.save(pet);
		redirectAttributes.addFlashAttribute("message", "Pet details has been edited");
		HttpHeaders headers = new HttpHeaders();
		headers.add("location", "/owners/" + owner.getId());
		return new ResponseEntity<>("", headers, HttpStatus.MOVED_TEMPORARILY);
	}

}
