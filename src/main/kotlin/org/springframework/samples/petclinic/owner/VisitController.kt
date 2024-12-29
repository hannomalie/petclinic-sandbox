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
package org.springframework.samples.petclinic.owner

import io.javalin.http.Context
import io.javalin.http.HttpStatus
import org.springframework.samples.petclinic.BindingResult.Companion.validate
import org.springframework.samples.petclinic.PetClinicApplication.getOwnerIdFromPath
import org.springframework.samples.petclinic.PetClinicApplication.setResponse
import org.springframework.samples.petclinic.system.Database
import org.springframework.samples.petclinic.system.ResponseEntity
import org.springframework.samples.petclinic.system.Templating.htmlHeaders
import org.springframework.samples.petclinic.system.Templating.renderView
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import java.time.LocalDate

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@Controller
class VisitController(private val database: Database) {
    //	@InitBinder
    //	public void setAllowedFields(WebDataBinder dataBinder) {
    //		dataBinder.setDisallowedFields("id");
    //	}
    fun initNewVisitForm(ctx: Context) {
        val modelMap = ModelMap()
        val ownerId = ctx.getOwnerIdFromPath()
        val petId = ctx.pathParam("petId").toInt()

        val ownerAndPets = database.findOwnerAndPetsByOwnerId(ownerId)!!
        val owner = ownerAndPets.owner
        val pets = ownerAndPets.pets

        val pet = pets.stream().filter { it: Pet -> it.id == petId }.findFirst().get()

        modelMap["pet"] = pet
        modelMap["owner"] = owner
        val visitsForPet = HashMap<Any, Any>()
        visitsForPet[pet] = database.findVisitsForPet(pet.id)
        modelMap.addAttribute("visitsForPet", visitsForPet)
        modelMap["visit"] = Visit()

        ctx.setResponse(
            ResponseEntity(
                renderView("pets/createOrUpdateVisitForm", modelMap, null),
                htmlHeaders,
                HttpStatus.OK.code
            )
        )
    }

    fun processNewVisitForm(ctx: Context) {
        val model = ModelMap()
        val ownerId = ctx.getOwnerIdFromPath()
        val ownerAndPets = database.findOwnerAndPetsByOwnerId(ownerId)!!
        val owner = ownerAndPets.owner
        val petId = ctx.pathParam("petId").toInt()
        val visit = getVisitFromForm(ctx)
        val result = validate(visit)

        val pets = ownerAndPets.pets

        val pet = pets.first { it.id == petId }
        model["pet"] = pet
        if (result.hasErrors()) {
            model["visit"] = visit
            model.addAttribute("visitsForPet", mapOf(pet to database.findVisitsForPet(pet.id)))
            ctx.setResponse(
                ResponseEntity(
                    renderView("pets/createOrUpdateVisitForm", model, result),
                    htmlHeaders,
                    HttpStatus.OK.code
                )
            )
        } else {
            database.save(visit, petId)
            model["message"] = "Your visit has been booked"

            model["owner"] = owner
            model["pets"] = pets
            model["visits"] = database.getVisitsForPets(ownerAndPets)
            ctx.setResponse(
                ResponseEntity(renderView("owners/ownerDetails", model, null), htmlHeaders, HttpStatus.OK.code)
            )
        }
    }
}

fun getVisitFromForm(ctx: Context): Visit = Visit().apply {
    val idString = ctx.formParam("id")
    if (!idString.isNullOrEmpty()) {
        id = idString.toInt()
    }
    description = ctx.formParam("description")
    val dateString = ctx.formParam("date")
    if (!dateString.isNullOrEmpty()) {
        date = LocalDate.parse(dateString)
    }
}
