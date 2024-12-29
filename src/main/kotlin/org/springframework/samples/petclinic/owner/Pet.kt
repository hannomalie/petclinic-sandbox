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

import org.springframework.format.Formatter
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.samples.petclinic.model.NamedEntity
import org.springframework.samples.petclinic.system.Database
import org.springframework.util.StringUtils
import java.text.ParseException
import java.time.LocalDate
import java.util.*
import kotlin.collections.LinkedHashSet

/**
 * Simple business object representing a pet.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class Pet : NamedEntity() {
	var ownerId: Int? = null
	@DateTimeFormat(pattern = "yyyy-MM-dd")
    var birthDate: LocalDate? = null
	var type: PetType? = null

    //	@OrderBy("visit_date ASC") TODO: Add order to queries
    val visits: MutableSet<Visit> = LinkedHashSet()

    fun addVisit(visit: Visit) {
        visits.add(visit)
    }
}

/**
 * @author Juergen Hoeller Can be Cat, Dog, Hamster...
 */
class PetType : NamedEntity()

/**
 * Instructs Spring MVC on how to parse and print elements of type 'PetType'. Starting
 * from Spring 3.0, Formatters have come as an improvement in comparison to legacy
 * PropertyEditors. See the following links for more details: - The Spring ref doc:
 * https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#format
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Michael Isvy
 */
class PetTypeFormatter(private val database: Database) : Formatter<PetType> {
    override fun print(petType: PetType, locale: Locale): String = petType.name!!

    override fun parse(text: String, locale: Locale): PetType = database.findPetTypes().firstOrNull {
        it.name == text
    } ?: throw ParseException("type not found: $text", 0)
}

/**
 * `Validator` for `Pet` forms.
 *
 *
 * We're not using Bean Validation annotations here because it is easier to define such
 * validation rule in Java.
 *
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 */
object PetValidator {
    private const val REQUIRED = "required"

    fun validate(pet: Pet, errors: HashMap<String, MutableList<String>>) {
        val name = pet.name
        // name validation
        if (!StringUtils.hasText(name)) {
            errors["name"] = java.util.List.of(REQUIRED)
        }

        // type validation
        if (pet.isNew && pet.type == null) {
            errors["type"] = java.util.List.of(REQUIRED)
        }

        // birth date validation
        if (pet.birthDate == null) {
            errors["birthDate"] = java.util.List.of(REQUIRED)
        }
    }
}
