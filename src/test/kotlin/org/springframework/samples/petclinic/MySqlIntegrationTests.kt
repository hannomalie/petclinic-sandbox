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
package org.springframework.samples.petclinic

import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
internal class MySqlIntegrationTests : BaseAppTest(container) {
    @Test
    fun testFindAll() {
        database.findAllVets()
    }

    @Test
    fun testOwnerDetails() {
        val george = createGeorge()
        val httpResponse = get("http://localhost:" + port + "/owners/" + george.id)
        AssertionsForClassTypes.assertThat(httpResponse.statusCode()).isEqualTo(200)
    }

    companion object {
        @Container
        var container = MySQLContainer("mysql:9.0")
    }
}
