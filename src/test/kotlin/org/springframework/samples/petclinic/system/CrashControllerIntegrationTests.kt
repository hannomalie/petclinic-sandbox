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
package org.springframework.samples.petclinic.system

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.samples.petclinic.BaseAppTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Integration Test for [CrashController].
 *
 * @author Alex Lutz
 */
// NOT Waiting https://github.com/spring-projects/spring-boot/issues/5574
@Testcontainers(disabledWithoutDocker = true)
internal class CrashControllerIntegrationTests : BaseAppTest(container) {
    @Test
    fun testTriggerExceptionJson() {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/oups"))
            .header("Accept", "application/json")
            .build()
        val httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString())
        val json = ObjectMapper().readValue(httpResponse.body().toString(), HashMap::class.java)

        assertEquals(500, httpResponse.statusCode())
        assertNotNull(json["timestamp"])
        assertNotNull(json["status"])
        assertNotNull(json["error"])
        assertEquals("Expected: controller used to showcase what happens when an exception is thrown", json["message"])
        assertEquals("/oups", json["path"])
    }

    @Test
    fun testTriggerExceptionHtml() {
        val httpResponse = get("http://localhost:$port/oups", "text/html")

        assertEquals(500, httpResponse.statusCode())
        assertThat(httpResponse.body().toString()).containsSubsequence(
            "<body>", "<h2>", "Something happened...", "</h2>", "<p>",
            "Expected:", "controller", "used", "to", "showcase", "what", "happens", "when", "an", "exception", "is",
            "thrown", "</p>", "</body>"
        )
        assertThat(httpResponse.body().toString()).doesNotContain(
            "Whitelabel Error Page",
            "This application has no explicit mapping for"
        )
    }

    companion object {
        @Container
        val container = PostgreSQLContainer("pgvector/pgvector:pg16").withDatabaseName("petclinic")
    }
}
