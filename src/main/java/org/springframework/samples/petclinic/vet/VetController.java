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
import org.apache.catalina.connector.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.owner.Database;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.samples.petclinic.system.Templating.htmlHeaders;
import static org.springframework.samples.petclinic.system.Templating.renderView;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
class VetController {

	private final Database database;

	public VetController(Database vetRepository) {
		this.database = vetRepository;
	}

	@GetMapping("/vets")
	public ResponseEntity<String> showVets(@RequestParam(defaultValue = "1") int page, RequestEntity request, ObjectMapper mapper) throws JsonProcessingException {
		Vets vets = new Vets();
		vets.getVetList().addAll(this.database.findAllVets());

		var acceptHeader = request.getHeaders().get("Accept");
		if(!acceptHeader.isEmpty() && acceptHeader.get(0).equals("application/json")) {
			var responseBody = mapper.writeValueAsString(vets);
			var jsonHeaders = new HttpHeaders();
			jsonHeaders.add("Content-Type", "application/json");
			return new ResponseEntity<>(responseBody, jsonHeaders, Response.SC_OK);
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
			return new ResponseEntity<>(renderView("vets/vetList", model, null), htmlHeaders, Response.SC_OK);
		}
	}
}
