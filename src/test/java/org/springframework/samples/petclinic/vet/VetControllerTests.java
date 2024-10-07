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

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the {@link VetController}
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisabledInNativeImage
@DisabledInAotMode
class VetControllerTests {

	@LocalServerPort
	int port;

	@MockBean
	private VetRepository vets;

	private Vet james() {
		Vet james = new Vet();
		james.setFirstName("James");
		james.setLastName("Carter");
		james.setId(1);
		return james;
	}

	private Vet helen() {
		Vet helen = new Vet();
		helen.setFirstName("Helen");
		helen.setLastName("Leary");
		helen.setId(2);
		Specialty radiology = new Specialty();
		radiology.setId(1);
		radiology.setName("radiology");
		helen.addSpecialty(radiology);
		return helen;
	}

	@BeforeEach
	void setup() {
		given(this.vets.findAll()).willReturn(Lists.newArrayList(james(), helen()));
		given(this.vets.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<Vet>(Lists.newArrayList(james(), helen())));

	}

	@Test
	void testShowVetListHtml() throws Exception {

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/vets?page=1"))
			.header("Accept", "text/html")
			.build();
		AtomicReference<String> html = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept((response) -> {
				html.set(response.body());
			})
			.join();

		var containsTitle = html.get().contains("<title>PetClinic :: a Spring Framework demonstration</title>");
		Assertions.assertTrue(containsTitle);
	}

	@Test
	void testShowResourcesVetList() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/vets"))
			.header("Accept", "application/json")
			.build();
		AtomicReference<HashMap<String, HashMap<String, Object>>> json = new AtomicReference<>();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept((response) -> {
				ObjectMapper mapper = new ObjectMapper();
				try {
					json.set(mapper.readValue(response.body(), HashMap.class));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			})
			.join();

		var vetList = (List<HashMap<String, Object>>) json.get().get("vetList");
		var expectedVetListEntry = new HashMap<String, Object>();
		expectedVetListEntry.put("id", 1);
		expectedVetListEntry.put("firstName", "James");
		expectedVetListEntry.put("lastName", "Carter");
		expectedVetListEntry.put("specialties", new ArrayList<String>());
		expectedVetListEntry.put("nrOfSpecialties", 0);
		expectedVetListEntry.put("new", false);
		Assertions.assertEquals(expectedVetListEntry, vetList.get(0));
	}
}
