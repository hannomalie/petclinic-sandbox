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

import org.assertj.core.api.AssertionsForClassTypes
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.samples.petclinic.BaseAppTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

/**
 * Test class for the [VetController]
 */
@Testcontainers(disabledWithoutDocker = true)
internal class VetControllerTests : BaseAppTest(container) {
    private val james: Vet = Vet().apply {
        firstName = "James"
        lastName = "Carter"
        id = 1
    }

    private val helen: Vet = Vet().apply {
        firstName = "Helen"
        lastName = "Leary"
        id = 2
        addSpecialty(Specialty().apply {
            id = 1
            name = "radiology"
        })
    }

    @BeforeEach
    fun setup() {
        database.save(james)
        database.save(helen)
    }

    @Test
    fun testShowVetListHtml() {
        val httpResponse = get("http://localhost:$port/vets?page=1", "text/html")

        assertThat(httpResponse.body().toString())
            .contains("<title>PetClinic :: a Spring Framework demonstration</title>")
    }

    @Test
    fun testShowResourcesVetList() {
        val httpResponse = get("http://localhost:$port/vets", "application/json")
        val json = ObjectMapper().readValue(httpResponse.body().toString(), HashMap::class.java)

        val vetList = json["vetList"] as List<HashMap<String, Any>>
        val firstVet = vetList.first()
        val expectedVetListEntry = buildMap {
            this["id"] = firstVet["id"]
            this["firstName"] = "James"
            this["lastName"] = "Carter"
            this["specialties"] = ArrayList<String>()
            this["nrOfSpecialties"] = 0
            this["new"] = false
        }
        assertThat(expectedVetListEntry).isEqualTo(firstVet)
    }

    companion object {
        @Container
        val container = PostgreSQLContainer("pgvector/pgvector:pg16").withDatabaseName("petclinic")
    }
}
