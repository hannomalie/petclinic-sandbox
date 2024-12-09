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

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
class VisitController {

	private final Database database;

	public VisitController(Database database) {
		this.database = database;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	/**
	 * Called before each and every @RequestMapping annotated method. 2 goals: - Make sure
	 * we always have fresh data - Since we do not use the session scope, make sure that
	 * Pet object always has an id (Even though id is not part of the form fields)
	 *
	 * @param petId
	 * @return Pet
	 */
	@ModelAttribute("visit")
	public Visit loadPetWithVisit(@PathVariable("ownerId") int ownerId, @PathVariable("petId") int petId,
														Map<String, Object> model) {
		OwnerAndPets ownerAndPets = database.findOwnerAndPetsByOwnerId(ownerId);
		var owner = ownerAndPets.owner();
		var pets = ownerAndPets.pets();

		var pet = pets.stream().filter(it -> it.getId().equals(petId)).findFirst().get();

		model.put("pet", pet);
		model.put("owner", owner);

		Visit visit = new Visit();
//		return database.save(visit, petId);
		return visit;
	}

	// Spring MVC calls method loadPetWithVisit(...) before initNewVisitForm is
	// called
	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public ResponseEntity<String> initNewVisitForm(ModelMap modelMap) {
		return new ResponseEntity<>(renderView("pets/createOrUpdateVisitForm", modelMap, null), htmlHeaders, HttpStatus.OK);
//		return "pets/createOrUpdateVisitForm";
	}

	// Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is
	// called
	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public ResponseEntity<String> processNewVisitForm(@ModelAttribute Owner owner, @PathVariable int petId, @Valid Visit visit,
													  BindingResult result, RedirectAttributes redirectAttributes, ModelMap model) {
		if (result.hasErrors()) {
			return new ResponseEntity<>(renderView("pets/createOrUpdateVisitForm", model, result), htmlHeaders, HttpStatus.OK);
//			return "pets/createOrUpdateVisitForm";
		}

		database.save(visit, petId);
		redirectAttributes.addFlashAttribute("message", "Your visit has been booked");
		HttpHeaders htmlHeaders = new HttpHeaders();
		htmlHeaders.add("Location", "/owners/" + owner.getId());
		return new ResponseEntity<>("", htmlHeaders, HttpStatus.MOVED_TEMPORARILY);
//		return "redirect:/owners/{ownerId}";
	}

}
