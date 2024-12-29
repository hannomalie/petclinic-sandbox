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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.samples.petclinic.BaseAppTest
import java.net.http.HttpResponse

/**
 * Test class for [OwnerController]
 *
 * @author Colin But
 */
internal class OwnerControllerTests : BaseAppTest() {
    @Test
    fun testInitCreationForm() {
        val httpResponse: HttpResponse<*> = get("http://localhost:$port/owners/new")
        Assertions.assertThat(httpResponse.body().toString())
            .contains("<form class=\"form-horizontal\" id=\"add-owner-form\" method=\"post\">")
    }

    @Test
    fun testProcessCreationFormSuccess() {
        val httpResponse: HttpResponse<*> = postForm(
            "http://localhost:$port/owners/new",
            "firstName=Joe&lastName=Bloggs&address=123%20Caramel%20Street&city=London&telephone=1316761638"
        )

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
    }

    @Test
    fun testProcessCreationFormHasErrors() {
        val httpResponse: HttpResponse<*> =
            postForm("http://localhost:$port/owners/new", "firstName=Joe&lastName=Bloggs&city=London")

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
        Assertions.assertThat(httpResponse.body().toString()).containsSubsequence(
            "<div class=\"form-group has-error\">",
            "<label class=\"col-sm-2 control-label\">",
            "Address",
            "</label>",
            "</div>"
        )
        Assertions.assertThat(httpResponse.body().toString()).containsSubsequence(
            "<div class=\"form-group has-error\">",
            "<label class=\"col-sm-2 control-label\">",
            "Telephone",
            "</label>",
            "</div>"
        )
    }

    @Test
    fun testInitFindForm() {
        val httpResponse: HttpResponse<*> = get("http://localhost:$port/owners/find")

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
        org.junit.jupiter.api.Assertions.assertTrue(httpResponse.body().toString().contains("<h2>Find Owners</h2>"))
    }

    @Test
    fun testProcessFindFormSuccess() {
        val httpResponse: HttpResponse<*> = get("http://localhost:$port/owners?page=1")

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
        Assertions.assertThat(httpResponse.body().toString()).contains("<h2>Find Owners</h2>")
    }

    @Test
    fun testProcessFindFormByLastName() {
        val george: Owner = createGeorge()
        val httpResponse: HttpResponse<*> = get("http://localhost:$port/owners?page=1&lastName=Franklin")

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
    }

    @Test
    fun testProcessFindFormNoOwnersFound() {
        val httpResponse: HttpResponse<*> = get("http://localhost:$port/owners?page=1&lastName=Unknown%20Surname")

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
        Assertions.assertThat(httpResponse.body().toString()).containsSubsequence(
            "<div class=\"col-sm-10\">",
            "<input class=\"form-control\" id=\"lastName\" name=\"lastName\" value=\"Unknown Surname\" size=\"30\" maxlength=\"80\" />",
            "<span class=\"help-inline\">",
            "<div>",
            "<p>",
            "has not been found",
            "</p>",
            "</div>"
        )
    }

    @Test
    fun testInitUpdateOwnerForm() {
        val george: Owner = createGeorge()

        val httpResponse: HttpResponse<*> = get("http://localhost:$port".toString() + "/owners/" + george.id + "/edit")

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
        Assertions.assertThat(httpResponse.body().toString())
            .contains("<input class=\"form-control\" type=\"text\" id=\"lastName\" name=\"lastName\" value=\"Franklin\" />")
        Assertions.assertThat(httpResponse.body().toString())
            .contains("<input class=\"form-control\" type=\"text\" id=\"firstName\" name=\"firstName\" value=\"George\" />")
        Assertions.assertThat(httpResponse.body().toString())
            .contains("<input class=\"form-control\" type=\"text\" id=\"address\" name=\"address\" value=\"110 W. Liberty St.\" />")
        Assertions.assertThat(httpResponse.body().toString())
            .contains("<input class=\"form-control\" type=\"text\" id=\"city\" name=\"city\" value=\"Madison\" />")
        Assertions.assertThat(httpResponse.body().toString())
            .contains("<input class=\"form-control\" type=\"text\" id=\"telephone\" name=\"telephone\" value=\"6085551023\" />")
    }

    @Test
    fun testProcessUpdateOwnerFormSuccess() {
        val george: Owner = createGeorge()
        val httpResponse: HttpResponse<*> = postForm(
            "http://localhost:$port".toString() + "/owners/" + george.id + "/edit",
            "firstName=Joe&lastName=Bloggs&address=123%20Caramel%20Street&city=London&telephone=1616291589"
        )

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
    }

    @Test
    fun testProcessUpdateOwnerFormUnchangedSuccess() {
        val george: Owner = createGeorge()
        val httpResponse: HttpResponse<*> = postForm(
            "http://localhost:$port".toString() + "/owners/" + george.id + "/edit",
            "firstName=George&lastName=Franklin&address=110%20W.%20Liberty%20St.&city=Madison&telephone=6085551023"
        )

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
    }

    @Test
    fun testProcessUpdateOwnerFormHasErrors() {
        val george: Owner = createGeorge()
        val httpResponse: HttpResponse<*> = postForm(
            "http://localhost:$port".toString() + "/owners/" + george.id + "/edit",
            "firstName=Joe&lastName=Bloggs&address=&telephone="
        )

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
        Assertions.assertThat(httpResponse.body().toString()).containsSubsequence(
            "<div class=\"form-group has-error\">",
            "<label class=\"col-sm-2 control-label\">",
            "Address",
            "</label>",
            "</div>"
        )
        Assertions.assertThat(httpResponse.body().toString()).containsSubsequence(
            "<div class=\"form-group has-error\">",
            "<label class=\"col-sm-2 control-label\">",
            "Telephone",
            "</label>",
            "</div>"
        )
    }

    @Test
    fun testShowOwner() {
        val george: Owner = createGeorge()

        val httpResponse: HttpResponse<*> = get("http://localhost:$port".toString() + "/owners/" + george.id)

        org.junit.jupiter.api.Assertions.assertEquals(200, httpResponse.statusCode())
        Assertions.assertThat(httpResponse.body().toString()).containsSubsequence(
            "<tr>",
            "<th>",
            "Name",
            "</th>",
            "<td>",
            "<b>",
            "George Franklin",
            "</b>",
            "</td>",
            "</tr>",
            "<tr>",
            "<th>",
            "Address",
            "</th>",
            "<td>",
            "110 W. Liberty St.",
            "</td>",
            "</tr>",
            "<tr>",
            "<th>",
            "City",
            "</th>",
            "<td>",
            "Madison",
            "</td>",
            "</tr>",
            "<tr>",
            "<th>",
            "Telephone",
            "</th>",
            "<td>",
            "6085551023",
            "</td>",
            "</tr>"
        )
        Assertions.assertThat(httpResponse.body().toString()).containsSubsequence(
            "<thead>",
            "<tr>",
            "<th>",
            "Visit Date",
            "</th>",
            "<th>",
            "Description",
            "</th>",
            "</tr>",
            "</thead>",
            "<tr>",
            "<td>",
            "2023-10-02",
            "</td>",
            "<td>",
            "</td>",
            "</tr>"
        )
    }
}
