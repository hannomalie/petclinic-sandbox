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
package org.springframework.samples.petclinic.vet

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.http.Context
import org.eclipse.jetty.server.Response
import org.springframework.samples.petclinic.PetClinicApplication.getPageParamOrDefault
import org.springframework.samples.petclinic.PetClinicApplication.setResponse
import org.springframework.samples.petclinic.system.Database
import org.springframework.samples.petclinic.system.PageRequest
import org.springframework.samples.petclinic.system.ResponseEntity
import org.springframework.samples.petclinic.system.Templating

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
class VetController(private val database: Database, private val mapper: ObjectMapper) {
    fun showVets(ctx: Context) {
        val page = ctx.getPageParamOrDefault()
        val acceptHeader = ctx.header("Accept")
        val vets = Vets()
        vets.vetList.addAll(database.findAllVets())

        if (acceptHeader != null && acceptHeader == "application/json") {
            val responseBody = mapper.writeValueAsString(vets)
            val jsonHeaders = HashMap<String, String>()
            jsonHeaders["Content-Type"] = "application/json"

            ctx.setResponse(ResponseEntity(responseBody, jsonHeaders, Response.SC_OK))
        } else {
            val model = HashMap<String, Any>()
            model["vets"] = vets.vetList
            val pageSize = 5
            val pageable = PageRequest.of(page - 1, pageSize)!! // TODO: This will blow up
            val paginated = database.findAllVetsPageable(pageable)
            vets.vetList.addAll(paginated.toList())
            val listVets = paginated.content
            model["currentPage"] = page
            model["totalPages"] = paginated.totalPages
            model["totalItems"] = paginated.totalElements
            model["listVets"] = listVets

            ctx.setResponse(
                ResponseEntity(
                    Templating.renderView("vets/vetList", model, null),
                    Templating.htmlHeaders,
                    Response.SC_OK
                )
            )
        }
    }
}
