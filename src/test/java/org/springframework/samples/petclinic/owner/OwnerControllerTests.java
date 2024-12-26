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
import org.springframework.samples.petclinic.BaseSpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link OwnerController}
 *
 * @author Colin But
 */
class OwnerControllerTests extends BaseSpringBootTest {
	@Test
	void testInitCreationForm() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners/new");
		assertThat(httpResponse.body().toString()).contains("<form class=\"form-horizontal\" id=\"add-owner-form\" method=\"post\">");
	}

	@Test
	void testProcessCreationFormSuccess() throws Exception {
		var httpResponse = postForm("http://localhost:" + port + "/owners/new", "firstName=Joe&lastName=Bloggs&address=123%20Caramel%20Street&city=London&telephone=1316761638");

		assertEquals(200, httpResponse.statusCode());
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
		assertThat(httpResponse.body().toString()).contains("<h2>Find Owners</h2>");
	}

	@Test
	void testProcessFindFormByLastName() throws Exception {
		var george = createGeorge();
		var httpResponse = get("http://localhost:" + port + "/owners?page=1&lastName=Franklin");

		assertEquals(200, httpResponse.statusCode());
	}

	@Test
	void testProcessFindFormNoOwnersFound() throws Exception {
		var httpResponse = get("http://localhost:" + port + "/owners?page=1&lastName=Unknown%20Surname");

		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"col-sm-10\">", "<input class=\"form-control\" id=\"lastName\" name=\"lastName\" value=\"Unknown Surname\" size=\"30\" maxlength=\"80\" />", "<span class=\"help-inline\">", "<div>", "<p>", "has not been found", "</p>", "</div>");
	}

	@Test
	void testInitUpdateOwnerForm() throws Exception {
		var george = createGeorge();

		var httpResponse = get("http://localhost:" + port + "/owners/" + george.getId() + "/edit");

		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).contains("<input class=\"form-control\" type=\"text\" id=\"lastName\" name=\"lastName\" value=\"Franklin\" />");
		assertThat(httpResponse.body().toString()).contains("<input class=\"form-control\" type=\"text\" id=\"firstName\" name=\"firstName\" value=\"George\" />");
		assertThat(httpResponse.body().toString()).contains("<input class=\"form-control\" type=\"text\" id=\"address\" name=\"address\" value=\"110 W. Liberty St.\" />");
		assertThat(httpResponse.body().toString()).contains("<input class=\"form-control\" type=\"text\" id=\"city\" name=\"city\" value=\"Madison\" />");
		assertThat(httpResponse.body().toString()).contains("<input class=\"form-control\" type=\"text\" id=\"telephone\" name=\"telephone\" value=\"6085551023\" />");
	}

	@Test
	void testProcessUpdateOwnerFormSuccess() throws Exception {
		var george = createGeorge();
		var httpResponse = postForm("http://localhost:" + port + "/owners/" + george.getId() + "/edit",
			"firstName=Joe&lastName=Bloggs&address=123%20Caramel%20Street&city=London&telephone=1616291589");

		assertEquals(200, httpResponse.statusCode());
	}

	@Test
	void testProcessUpdateOwnerFormUnchangedSuccess() throws Exception {
		var george = createGeorge();
		var httpResponse = postForm("http://localhost:" + port + "/owners/" + george.getId() + "/edit",
			"firstName=George&lastName=Franklin&address=110%20W.%20Liberty%20St.&city=Madison&telephone=6085551023");

		assertEquals(200, httpResponse.statusCode());
	}

	@Test
	void testProcessUpdateOwnerFormHasErrors() throws Exception {
		var george = createGeorge();
		var httpResponse = postForm("http://localhost:" + port + "/owners/" + george.getId() + "/edit",
			"firstName=Joe&lastName=Bloggs&address=&telephone=");

		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Address", "</label>", "</div>");
		assertThat(httpResponse.body().toString()).containsSubsequence("<div class=\"form-group has-error\">", "<label class=\"col-sm-2 control-label\">", "Telephone", "</label>", "</div>");
	}

	@Test
	void testShowOwner() throws Exception {
		var george = createGeorge();

		var httpResponse = get("http://localhost:" + port + "/owners/" + george.getId());

		assertEquals(200, httpResponse.statusCode());
		assertThat(httpResponse.body().toString()).containsSubsequence("<tr>", "<th>", "Name", "</th>", "<td>", "<b>", "George Franklin", "</b>", "</td>", "</tr>", "<tr>", "<th>", "Address", "</th>", "<td>", "110 W. Liberty St.", "</td>", "</tr>", "<tr>", "<th>", "City", "</th>", "<td>", "Madison", "</td>", "</tr>", "<tr>", "<th>", "Telephone", "</th>", "<td>", "6085551023", "</td>", "</tr>");
		assertThat(httpResponse.body().toString()).containsSubsequence("<thead>", "<tr>", "<th>", "Visit Date", "</th>", "<th>", "Description", "</th>", "</tr>", "</thead>", "<tr>", "<td>", "2023-10-02", "</td>", "<td>", "</td>", "</tr>");
	}
}
