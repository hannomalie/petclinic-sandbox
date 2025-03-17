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

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.samples.petclinic.model.Person

/**
 * Simple JavaBean domain object representing an owner.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Oliver Drotbohm
 */
data class Owner(
    @get:NotBlank var address: String? = null,
    @get:NotBlank var city: String? = null,
    @get:NotBlank @get:Pattern(regexp = "\\d{10}", message = "Telephone must be a 10-digit number") var telephone: String? = null,
) : Person()

@JvmRecord
data class OwnerAndPets(val owner: Owner, val pets: List<Pet>)
