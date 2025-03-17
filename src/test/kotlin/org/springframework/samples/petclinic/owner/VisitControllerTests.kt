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
import org.junit.jupiter.api.Test
import org.springframework.samples.petclinic.BaseAppTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Test class for [VisitController]
 *
 * @author Colin But
 */
@Testcontainers(disabledWithoutDocker = true)
internal class VisitControllerTests : BaseAppTest(container) {
    @Test
    fun testInitNewVisitForm() {
        val ownerAndPets = createOwnerAndPets()
        val httpResponse =
            get("http://localhost:" + port + "/owners/" + ownerAndPets.owner.id + "/pets/" + ownerAndPets.pets[0].id + "/visits/new")

        Assertions.assertEquals(200, httpResponse.statusCode())
        AssertionsForClassTypes.assertThat(httpResponse.body().toString())
            .containsIgnoringWhitespaces("<h2>New Visit</h2>")
    }

    @Test
    fun testProcessNewVisitFormSuccess() {
        val ownerAndPets = createOwnerAndPets()
        val httpResponse = postForm(
            "http://localhost:" + port + "/owners/" + ownerAndPets.owner.id + "/pets/" + ownerAndPets.pets.stream()
                .findFirst().get().id + "/visits/new",
            "name=George&description=Visit%20Description"
        )

        Assertions.assertEquals(200, httpResponse.statusCode())
    }

    @Test
    fun testProcessNewVisitFormHasErrors() {
        val ownerAndPets = createOwnerAndPets()
        val httpResponse = postForm(
            "http://localhost:" + port + "/owners/" + ownerAndPets.owner.id + "/pets/" + ownerAndPets.pets.stream()
                .findFirst().get().id + "/visits/new",
            "name=George"
        )
        Assertions.assertEquals(200, httpResponse.statusCode())
        AssertionsForClassTypes.assertThat(httpResponse.body().toString()).containsSubsequence(
            "<div class=\"form-group has-error\">",
            "<label class=\"col-sm-2 control-label\">",
            "Description",
            "</label>",
            "</div>"
        )
    }

    companion object {
        @Container
        var container = PostgreSQLContainer("pgvector/pgvector:pg16")
    }
}
