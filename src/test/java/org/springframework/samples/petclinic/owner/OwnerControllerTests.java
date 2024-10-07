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

import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.samples.petclinic.BaseSpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link OwnerController}
 *
 * @author Colin But
 */
@Testcontainers(disabledWithoutDocker = true)
class OwnerControllerTests extends BaseSpringBootTest {

	private static final int TEST_OWNER_ID = 1;

	@ServiceConnection
	@Container
	static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

	@Test
	void testInitCreationForm() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners/new");
		assertTrue(httpResponse.body().toString().contains("<form class=\"form-horizontal\" id=\"add-owner-form\" method=\"post\">"));
	}

	@Test
	void testProcessCreationFormSuccess() throws Exception {
		var httpResponse = postForm("http://localhost:" + port + "/owners/new", "firstName=Joe&lastName=Bloggs&address=123%20Caramel%20Street&city=London&telephone=1316761638");

		assertEquals(302, httpResponse.statusCode());
		assertThat(httpResponse.headers().firstValue("location").get()).contains("/owners/" + TEST_OWNER_ID);
	}

	@Test
	void testProcessCreationFormHasErrors() throws Exception {
		var httpResponse = postForm("http://localhost:" + port + "/owners/new", "firstName=Joe&lastName=Bloggs&city=London");

		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Address", "</label>", "</div>");
		assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Telephone", "</label>", "</div>");
	}

	@Test
	void testInitFindForm() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners/find");

		assertEquals(200, httpResponse.statusCode());
		assertTrue(httpResponse.body().toString().contains("<h2>Find Owners</h2>"));
	}

	@Test
	void testProcessFindFormSuccess() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners?page=1");

		assertEquals(200, httpResponse.statusCode());
		assertTrue(httpResponse.body().toString().contains("<h2>Owners</h2>"));
	}

	@Test
	void testProcessFindFormByLastName() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners?page=1&lastName=Franklin");

		assertEquals(302, httpResponse.statusCode());
		assertTrue(httpResponse.headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessFindFormNoOwnersFound() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners?page=1&lastName=Unknown%20Surname");

		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsIgnoringWhitespaces("<div class=\"col-sm-10\"><input class=\"form-control\" size=\"30\" maxlength=\"80\" id=\"lastName\" name=\"lastName\" value=\"Unknown Surname\" /> <span class=\"help-inline\"><div><p>wurde nicht gefunden</p></div>");
	}

	@Test
	void testInitUpdateOwnerForm() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/edit");

		assertEquals(200, httpResponse.statusCode());
		assertTrue(httpResponse.body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"lastName\" name=\"lastName\" value=\"Franklin\" />"));
		assertTrue(httpResponse.body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"firstName\" name=\"firstName\" value=\"George\" />"));
		assertTrue(httpResponse.body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"address\" name=\"address\" value=\"110 W. Liberty St.\" />"));
		assertTrue(httpResponse.body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"city\" name=\"city\" value=\"Madison\" />"));
		assertTrue(httpResponse.body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"telephone\" name=\"telephone\" value=\"6085551023\" />"));
	}

	@Test
	void testProcessUpdateOwnerFormSuccess() throws Exception {
		var httpResponse = postForm("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/edit",
			"firstName=Joe&lastName=Bloggs&address=123%20Caramel%20Street&city=London&telephone=1616291589");

		assertEquals(302, httpResponse.statusCode());
		assertTrue(httpResponse.headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessUpdateOwnerFormUnchangedSuccess() throws Exception {
		var httpResponse = postForm("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/edit", null);

		assertEquals(302, httpResponse.statusCode());
		assertTrue(httpResponse.headers().firstValue("location").get().contains("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessUpdateOwnerFormHasErrors() throws Exception {
		var httpResponse = postForm("http://localhost:" + port + "/owners/" + TEST_OWNER_ID + "/edit",
			"firstName=Joe&lastName=Bloggs&address=&telephone=");

		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Address", "</label>", "</div>");
		assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Telephone", "</label>", "</div>");
	}

	@Test
	void testShowOwner() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners/" + TEST_OWNER_ID);

		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsSubsequence("<tr>","<th>","Name","</th>","<td>","<b>","George Franklin","</b>","</td>","</tr>","<tr>","<th>","Address","</th>","<td>","110 W. Liberty St.","</td>","</tr>","<tr>","<th>","City","</th>","<td>","Madison","</td>","</tr>","<tr>","<th>","Telephone","</th>","<td>","6085551023","</td>","</tr>");
		assertThat(httpResponse.body().toString()).containsSubsequence("<thead>","<tr>","<th>","Visit Date","</th>","<th>","Description","</th>","</tr>","</thead>","<tr>","<td>","2024-10-10","</td>","<td>","</td>","</tr>");
	}

	@DynamicPropertySource
	static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
		registerDataSourceProperties(registry, container);
	}

}
