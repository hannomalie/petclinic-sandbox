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

import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.samples.petclinic.BaseAppTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Test class for the [PetController]
 *
 * @author Colin But
 */
@Testcontainers(disabledWithoutDocker = true)
internal class PetControllerTests : BaseAppTest(container) {
    @Test
    fun testInitCreationForm() {
        val george = createGeorge()
        val httpResponse = get("http://localhost:" + port + "/owners/" + george.id + "/pets/new")

        Assertions.assertEquals(200, httpResponse.statusCode())
        AssertionsForClassTypes.assertThat(httpResponse.body().toString())
            .containsIgnoringWhitespaces("<h2>New Pet</h2>")
    }


    @Test
    fun testProcessCreationFormSuccess() {
        val george = createGeorge()
        val httpResponse = postForm(
            "http://localhost:" + port + "/owners/" + george.id + "/pets/new",
            "name=Betty&type=hamster&birthDate=2015-02-12"
        )

        Assertions.assertEquals(200, httpResponse.statusCode())
    }

    @Test
    fun testProcessCreationFormHasErrors() {
        val george = createGeorge()
        val httpResponse = postForm(
            "http://localhost:" + port + "/owners/" + george.id + "/pets/new",
            "name=Betty&birthDate=2015-02-12"
        )

        Assertions.assertEquals(200, httpResponse.statusCode())
        org.assertj.core.api.Assertions.assertThat(httpResponse.body().toString()).containsSubsequence(
            "<div class=\"form-group has-error\">",
            "<label class=\"col-sm-2 control-label\">",
            "Type",
            "</label>",
            "</div>"
        )
    }

    @Test
    fun testInitUpdateForm() {
        val ownerAndPets = createOwnerAndPets()
        val httpResponse =
            get("http://localhost:" + port + "/owners/" + ownerAndPets.owner.id + "/pets/" + ownerAndPets.pets[0].id + "/edit")

        Assertions.assertEquals(200, httpResponse.statusCode())
        org.assertj.core.api.Assertions.assertThat(httpResponse.body().toString())
            .containsIgnoringWhitespaces("<button class=\"btn btn-primary\" id=\"submit-pet\" type=\"submit\">Update Pet</button>")
    }

    @Test
    fun testProcessUpdateFormSuccess() {
        val ownerAndPets = createOwnerAndPets()
        val httpResponse = postForm(
            "http://localhost:" + port + "/owners/" + ownerAndPets.owner.id + "/pets/" + ownerAndPets.pets.stream()
                .findFirst().get().id + "/edit",
            "name=BettyZZZ&type=hamster&birthDate=2015-02-12"
        )

        Assertions.assertEquals(200, httpResponse.statusCode())
    }

    @Test
    @Disabled("I don't know how to exactly resemble that case")
    @Throws(
        Exception::class
    )
    fun testProcessUpdateFormHasErrors() {
        val ownerAndPets = createOwnerAndPets()
        val httpResponse = postForm(
            "http://localhost:" + port + "/owners/" + ownerAndPets.owner.id + "/pets/" + ownerAndPets.pets.stream()
                .findFirst().get().id + "/edit",
            "id=&name=Betty&birthDate=2015-02-12"
        )

        Assertions.assertEquals(200, httpResponse.statusCode())
        org.assertj.core.api.Assertions.assertThat(httpResponse.body().toString()).containsSubsequence(
            "<div class=\"form-group has-error\">",
            "<label class=\"col-sm-2 control-label\">",
            "Pet",
            "</label>",
            "</div>"
        )
        //		mockMvc
//			.perform(post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID).param("name", "Betty")
//				.param("birthDate", "2015/02/12"))
//			.andExpect(model().attributeHasNoErrors("owner"))
//			.andExpect(model().attributeHasErrors("pet"))
//			.andExpect(status().isOk())
//			.andExpect(view().name("pets/createOrUpdatePetForm"));
    }

    companion object {
        @Container
        var container: PostgreSQLContainer<*> = PostgreSQLContainer("pgvector/pgvector:pg16")
    }
}
