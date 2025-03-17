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

import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import org.springframework.samples.petclinic.model.NamedEntity
import org.springframework.samples.petclinic.model.Person
import java.util.*

/**
 * Simple JavaBean domain object representing a veterinarian.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Arjen Poutsma
 */
class Vet(firstName: String? = null,
          lastName: String? = null) : Person(firstName, lastName) {
    private val specialties: MutableSet<Specialty> = HashSet()

    @XmlElement
    fun getSpecialties(): List<Specialty> = specialties.sortedBy { it.name?.lowercase() }

    val nrOfSpecialties: Int get() = specialties.size

    fun addSpecialty(specialty: Specialty) {
        specialties.add(specialty)
    }
}

/**
 * Simple domain object representing a list of veterinarians. Mostly here to be used for
 * the 'vets' [org.springframework.web.servlet.view.xml.MarshallingView].
 *
 * @author Arjen Poutsma
 */
@XmlRootElement
class Vets {
    private var vets: MutableList<Vet> = mutableListOf()

    @get:XmlElement
    val vetList: MutableList<Vet> get() = vets
}

/**
 * Models a [Vet&#39;s][Vet] specialty (for example, dentistry).
 *
 * @author Juergen Hoeller
 */
class Specialty : NamedEntity()
