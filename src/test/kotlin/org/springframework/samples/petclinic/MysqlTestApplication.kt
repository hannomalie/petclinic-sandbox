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

import org.springframework.samples.petclinic.PetClinicApplication.startApplication
import org.springframework.samples.petclinic.system.Database
import org.springframework.samples.petclinic.system.Database.DatabaseType
import org.testcontainers.containers.MySQLContainer
import java.time.Duration

/**
 * PetClinic Spring Boot Application.
 *
 * @author Dave Syer
 */
object MysqlTestApplication {
    fun main(args: Array<String>) {
        MySQLContainer("mysql:9.0").withMinimumRunningDuration(Duration.ofSeconds(5L)).use { container ->
            container.start()
            startApplication(8080, container.jdbcUrl, container.username, container.password, DatabaseType.MySQL)
        }
    }
}
