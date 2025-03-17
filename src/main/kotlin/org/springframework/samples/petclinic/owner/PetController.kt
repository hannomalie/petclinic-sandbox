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
import org.eclipse.jetty.http.HttpStatus
import org.springframework.samples.petclinic.BindingResult.Companion.validate
import org.springframework.samples.petclinic.PetClinicApplication.getOwnerIdFromPath
import org.springframework.samples.petclinic.PetClinicApplication.setResponse
import org.springframework.samples.petclinic.system.Database
import org.springframework.samples.petclinic.system.ResponseEntity
import org.springframework.samples.petclinic.system.Templating.htmlHeaders
import org.springframework.samples.petclinic.system.Templating.renderView
import org.springframework.samples.petclinic.system.Translations.get
import org.springframework.ui.ModelMap
import org.springframework.util.StringUtils
import java.time.LocalDate

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
class PetController(private val database: Database) {
    fun populatePetTypes(): Collection<PetType> = database.findPetTypes()

    fun findOwner(ownerId: Int): Owner = database.findOwnerAndPetsByOwnerId(ownerId)?.owner
        ?: throw IllegalArgumentException("Owner ID not found: $ownerId")

    fun findPet(ownerId: Int, petId: Int?): Pet {
        if (petId == null) return Pet()

        val ownerAndPets = database.findOwnerAndPetsByOwnerId(ownerId)
        ownerAndPets?.owner ?: throw IllegalArgumentException("Owner ID not found: $ownerId")
        return ownerAndPets.pets.firstOrNull { it.id == petId } ?: Pet()
    }

    fun initCreationForm(ctx: Context) {
        val model = ModelMap()
        val ownerId = ctx.getOwnerIdFromPath()

        val types = database.findPetTypes()
        model["types"] = types
        val pet = Pet()
        pet.birthDate = LocalDate.now()
        pet.type = types[0]
        model["pet"] = pet
        model["owner"] = database.findOwnerAndPetsByOwnerId(ownerId)?.owner

        ctx.setResponse(
            ResponseEntity(renderView("pets/createOrUpdatePetForm", model, null), htmlHeaders, HttpStatus.OK_200)
        )
    }

    fun processCreationForm(ctx: Context) {
        val locale = ctx.req().locale
        val model = ModelMap()
        val ownerId = ctx.getOwnerIdFromPath()
        val ownerAndPets = database.findOwnerAndPetsByOwnerId(ownerId)!!
        val owner = ownerAndPets.owner
        val pet = getPetFromForm(ctx, database, ownerId)
        val result = validate(pet)
        PetValidator.validate(pet, result)

        val persistedPets = ownerAndPets.pets
        if (persistedPets.any { it.name == pet.name }) {
            result.addError("name", "already exists")
        }

        val currentDate = LocalDate.now()
        if (pet.birthDate != null && pet.birthDate!!.isAfter(currentDate)) {
            result.addError("birthDate", get("typeMismatch.birthDate", locale))
        }

        model["pet"] = pet
        val types = database.findPetTypes()
        model["types"] = types
        model["owner"] = owner
        if (result.hasErrors()) {
            model["pet"] = pet
            ctx.setResponse(
                ResponseEntity(
                    renderView("pets/createOrUpdatePetForm", model, result),
                    htmlHeaders,
                    HttpStatus.OK_200
                )
            )
        } else {
            database.save(pet)
            model["message"] = "New Pet has been Added"
            val pets = database.findOwnerAndPetsByOwnerId(owner.id!!)!!.pets
            model["pets"] = pets
            model.addAttribute("visits", getVisitsForPets(pets))
            ctx.setResponse(
                ResponseEntity(renderView("owners/ownerDetails", model, result), htmlHeaders, HttpStatus.OK_200)
            )
        }
    }

    fun initUpdateForm(ctx: Context) {
        val model = ModelMap()
        val ownerId = ctx.getOwnerIdFromPath()
        val owner = database.findOwnerAndPetsByOwnerId(ownerId)!!.owner
        val petId = ctx.pathParam("petId").toInt()

        val pet = database.findPetById(petId)
        model["pet"] = pet
        val types = database.findPetTypes()
        model["types"] = types

        val headers = HashMap<String, String>()
        headers["Content-Type"] = "text/html"
        ctx.setResponse(
            ResponseEntity(renderView("pets/createOrUpdatePetForm", model, null), headers, HttpStatus.OK_200)
        )
    }

    fun processUpdateForm(ctx: Context) {
        val locale = ctx.req().locale
        val model = ModelMap()
        val ownerId = ctx.getOwnerIdFromPath()
        val pet = getPetFromForm(ctx, database, ownerId)
        val owner = database.findOwnerAndPetsByOwnerId(ownerId)!!.owner
        val result = validate(pet)

        val petName = pet.name

        // checking if the pet name already exist for the owner
        if (StringUtils.hasText(petName)) {
            val persistedPets = database.findOwnerAndPetsByOwnerId(owner.id!!)!!.pets
            if (persistedPets.stream().anyMatch { it: Pet -> it.name == pet.name }) {
                result.addError("name", get("duplicate", locale))
            }
        }

        val currentDate = LocalDate.now()
        if (pet.birthDate != null && pet.birthDate!!.isAfter(currentDate)) {
            result.addError("birthDate", get("typeMismatch.birthDate", locale))
        }

        if (result.hasErrors()) {
            model["pet"] = pet
            val types = database.findPetTypes()
            model["types"] = types
            val headers = HashMap<String, String>()
            headers["Content-Type"] = "text/html"

            ctx.setResponse(
                ResponseEntity(renderView("pets/createOrUpdatePetForm", model, result), headers, HttpStatus.OK_200)
            )
        } else {
            database.save(pet)
            model["message"] = "Pet details has been edited"
            model["owner"] = owner
            val pets = database.findOwnerAndPetsByOwnerId(owner.id!!)!!.pets
            model["pets"] = pets
            model["visits"] = getVisitsForPets(pets)

            ctx.setResponse(
                ResponseEntity(renderView("owners/ownerDetails", model, result), htmlHeaders, HttpStatus.OK_200)
            )
        }
    }

    private fun getVisitsForPets(pets: List<Pet>): Map<Pet, List<Visit>> = pets.associateWith { database.findVisitsForPet(it.id) }

    companion object {
        private fun getPetFromForm(ctx: Context, database: Database, ownerId: Int): Pet {
            val pet = Pet()
            val idString = ctx.formParam("id")
            if (!idString.isNullOrEmpty()) {
                pet.id = idString.toInt()
            }
            pet.name = ctx.formParam("name")
            pet.type = database.findPetTypes()
                .filter { petType -> petType.name.equals(ctx.formParam("type"), ignoreCase = true) }
                .firstOrNull()
            pet.birthDate = LocalDate.parse(ctx.formParam("birthDate"))
            pet.ownerId = ownerId
            return pet
        }
    }
}
