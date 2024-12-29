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
import org.junit.jupiter.api.condition.DisabledInNativeImage
import org.springframework.samples.petclinic.system.Database
import java.text.ParseException
import java.util.*

/**
 * Test class for [PetTypeFormatter]
 *
 * @author Colin But
 */
@DisabledInNativeImage
internal class PetTypeFormatterTests {
    @Test
    fun testPrint() {
        val database = Database()
        database.createTables()
        database.createPetTypes()
        val petTypeFormatter = PetTypeFormatter(database)

        val petType = PetType()
        petType.name = "Hamster"
        val petTypeName = petTypeFormatter.print(petType, Locale.ENGLISH)
        Assertions.assertThat(petTypeName).isEqualTo("Hamster")
    }

    @Test
    @Throws(ParseException::class)
    fun shouldParse() {
        val database = Database()
        database.createTables()
        database.createPetTypes()
        val petTypeFormatter = PetTypeFormatter(database)

        val petType = petTypeFormatter.parse("Bird", Locale.ENGLISH)
        Assertions.assertThat(petType.name).isEqualTo("Bird")
    }

    @Test
    @Throws(ParseException::class)
    fun shouldThrowParseException() {
        val database = Database()
        database.createTables()
        database.createPetTypes()
        val petTypeFormatter = PetTypeFormatter(database)

        org.junit.jupiter.api.Assertions.assertThrows(ParseException::class.java) {
            petTypeFormatter.parse("Fish", Locale.ENGLISH)
        }
    }
}
