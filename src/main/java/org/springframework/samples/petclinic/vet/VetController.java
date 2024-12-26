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
package org.springframework.samples.petclinic.vet;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import org.eclipse.jetty.server.Response;
import org.springframework.samples.petclinic.owner.Database;
import org.springframework.samples.petclinic.system.Page;
import org.springframework.samples.petclinic.system.PageRequest;
import org.springframework.samples.petclinic.system.Pageable;
import org.springframework.samples.petclinic.system.ResponseEntity;
import org.springframework.stereotype.Controller;

import static org.springframework.samples.petclinic.PetClinicApplication.setResponse;
import static org.springframework.samples.petclinic.PetClinicApplication.getPageParamOrDefault;
import static org.springframework.samples.petclinic.system.Templating.htmlHeaders;
import static org.springframework.samples.petclinic.system.Templating.renderView;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
public class VetController {

	private final Database database;
	private final ObjectMapper mapper;

	public VetController(Database vetRepository, ObjectMapper mapper) {
		this.database = vetRepository;
		this.mapper = mapper;
	}

	public void showVets(Context ctx) throws JsonProcessingException {
		var page = getPageParamOrDefault(ctx);
		var acceptHeader = ctx.header("Accept");
		Vets vets = new Vets();
		vets.getVetList().addAll(this.database.findAllVets());

		if(acceptHeader != null && acceptHeader.equals("application/json")) {
			var responseBody = mapper.writeValueAsString(vets);
			var jsonHeaders = new HashMap<String, String>();
			jsonHeaders.put("Content-Type", "application/json");

			setResponse(ctx, new ResponseEntity<>(responseBody, jsonHeaders, Response.SC_OK));
		} else {
			var model = new HashMap<String, Object>();
			model.put("vets", vets.getVetList());
			int pageSize = 5;
			Pageable pageable = PageRequest.of(page - 1, pageSize);
			Page<Vet> paginated = database.findAllVetsPageable(pageable);
			vets.getVetList().addAll(paginated.toList());
			List<Vet> listVets = paginated.getContent();
			model.put("currentPage", page);
			model.put("totalPages", paginated.getTotalPages());
			model.put("totalItems", paginated.getTotalElements());
			model.put("listVets", listVets);

			setResponse(ctx, new ResponseEntity<>(renderView("vets/vetList", model, null), htmlHeaders, Response.SC_OK));
		}
	}
}
