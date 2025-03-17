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
import org.springframework.samples.petclinic.BindingResult
import org.springframework.samples.petclinic.BindingResult.Companion.validate
import org.springframework.samples.petclinic.PetClinicApplication.getOwnerFromForm
import org.springframework.samples.petclinic.PetClinicApplication.getOwnerIdFromPath
import org.springframework.samples.petclinic.PetClinicApplication.getPageParamOrDefault
import org.springframework.samples.petclinic.PetClinicApplication.setResponse
import org.springframework.samples.petclinic.system.Database
import org.springframework.samples.petclinic.system.Page
import org.springframework.samples.petclinic.system.PageRequest
import org.springframework.samples.petclinic.system.ResponseEntity
import org.springframework.samples.petclinic.system.Templating.htmlHeaders
import org.springframework.samples.petclinic.system.Templating.renderView
import org.springframework.samples.petclinic.system.Translations.get
import org.springframework.ui.ModelMap

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
class OwnerController(private val database: Database) {
    fun initCreationForm(ctx: Context) {
        val model = mapOf("owner" to Owner())
        val response = ResponseEntity(
            renderView("owners/createOrUpdateOwnerForm", model, null),
            htmlHeaders,
            HttpStatus.OK.code
        )
        ctx.setResponse(response)
    }

    fun processCreationForm(ctx: Context) {
        val owner = ctx.getOwnerFromForm()
        val result = validate(owner)
        val modelMap = HashMap<String, Any>()
        modelMap["owner"] = owner
        if (result.hasErrors()) {
            modelMap["error"] = "There was an error in creating the owner."

            ctx.setResponse(
                ResponseEntity(
                    renderView("owners/createOrUpdateOwnerForm", modelMap, result),
                    htmlHeaders,
                    HttpStatus.OK.code
                )
            )
        } else {
            database.save(owner)
            modelMap["pets"] = database.findOwnerAndPetsByOwnerId(owner.id!!)!!.pets
            modelMap["message"] = "New Owner Created"
            ctx.setResponse(
                ResponseEntity(
                    renderView("owners/ownerDetails", modelMap, result),
                    htmlHeaders,
                    HttpStatus.OK.code
                )
            )
        }
    }

    fun initFindForm(ctx: Context) {
        val response =
            ResponseEntity(renderView("owners/findOwners", ModelMap(), null), htmlHeaders, HttpStatus.OK.code)
        ctx.setResponse(response)
    }

    fun processFindForm(ctx: Context) {
        val page = ctx.getPageParamOrDefault()
        val locale = ctx.req().locale
        // allow parameterless GET request for /owners to return all records
        val lastName = ctx.queryParam("lastName") ?: "" // empty string signifies broadest possible search
        val modelMap = ModelMap()

        // find owners by last name
        val ownersResults = findPaginatedForOwnersLastName(page, lastName)
        val res = if (ownersResults.isEmpty) {
            modelMap["lastName"] = lastName
            // no owners found
            val result = BindingResult()
            result.addError("lastName", get("notFound", locale))
            ResponseEntity(renderView("owners/findOwners", modelMap, result), htmlHeaders, HttpStatus.OK.code)
        } else if (ownersResults.totalElements == 1) {
            // 1 owner found
            val foundOwner = ownersResults.iterator().next().owner

            modelMap.addAttribute("owner", foundOwner)
            val ownerAndPets = database.findOwnerAndPetsByOwnerId(foundOwner.id!!)!!
            modelMap.addAttribute("pets", ownerAndPets.pets)
            modelMap.addAttribute("visits", database.getVisitsForPets(ownerAndPets))
            ResponseEntity(renderView("owners/ownerDetails", modelMap, null), htmlHeaders, HttpStatus.OK.code)
        } else {
            // multiple owners found

            modelMap.addAttribute("currentPage", page)
            modelMap.addAttribute("totalPages", ownersResults.totalPages)
            modelMap.addAttribute("totalItems", ownersResults.totalElements)
            modelMap.addAttribute("listOwners", ownersResults.content)
            val petsForOwnerId = HashMap<Any, Any>()
            val owners = ArrayList<Any>()
            for ((owner, pets) in ownersResults.content) {
                petsForOwnerId[owner.id!!] = pets
                owners.add(owner)
            }
            modelMap.addAttribute("petsForOwnerId", petsForOwnerId)
            modelMap.addAttribute("owners", owners)
            ResponseEntity(
                renderView("owners/ownersList", modelMap, null),
                htmlHeaders,
                HttpStatus.OK.code
            )
        }
        ctx.setResponse(res)
    }

    private fun findPaginatedForOwnersLastName(
        page: Int, lastname: String
    ) = database.findByLastName(lastname, PageRequest.of(page - 1, pageSize = 5))

    fun initUpdateOwnerForm(ctx: Context) {
        val ownerId = ctx.getOwnerIdFromPath()
        val owner = database.findOwnerAndPetsByOwnerId(ownerId)!!.owner
        val model = HashMap<String, Any>()
        model["owner"] = owner
        val response = ResponseEntity(
            renderView("owners/createOrUpdateOwnerForm", model, null),
            htmlHeaders,
            HttpStatus.OK.code
        )
        ctx.setResponse(response)
    }

    fun processUpdateOwnerForm(ctx: Context) {
        val model = ModelMap()
        val ownerId = ctx.getOwnerIdFromPath()
        val owner = ctx.getOwnerFromForm()
        val result = validate(owner)
        model["owner"] = owner
        if (result.hasErrors()) {
            model["error"] = "There was an error in updating the owner."
            ctx.setResponse(
                ResponseEntity(
                    renderView("owners/createOrUpdateOwnerForm", model, result),
                    htmlHeaders,
                    HttpStatus.OK.code
                )
            )
        } else {
            owner.id = ownerId
            database.save(owner)
            model["message"] = "Owner Values Updated"
            val ownerAndPets = database.findOwnerAndPetsByOwnerId(owner.id!!)!!
            model["pets"] = ownerAndPets.pets
            model["visits"] = database.getVisitsForPets(ownerAndPets)

            ctx.setResponse(
                ResponseEntity(
                    renderView("owners/ownerDetails", model, result),
                    htmlHeaders,
                    HttpStatus.OK.code
                )
            )
        }
    }

    /**
     * Custom handler for displaying an owner.
     *
     * @param ctx
     * @return a ModelMap with the model attributes for the view
     */
    fun showOwner(ctx: Context?) {
        val ownerId = ctx!!.getOwnerIdFromPath()
        val ownerAndPets = database.findOwnerAndPetsByOwnerId(ownerId)!!
        val owner = ownerAndPets.owner
        val model = mapOf(
            "owner" to owner,
            "pets" to ownerAndPets.pets,
            "visits" to database.getVisitsForPets(ownerAndPets),
        )
        val response = ResponseEntity(renderView("owners/ownerDetails", model, null), htmlHeaders, HttpStatus.OK.code)
        ctx.setResponse(response)
    }
}
