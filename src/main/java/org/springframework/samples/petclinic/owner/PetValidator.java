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
package org.springframework.samples.petclinic.owner;

import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * <code>Validator</code> for <code>Pet</code> forms.
 * <p>
 * We're not using Bean Validation annotations here because it is easier to define such
 * validation rule in Java.
 * </p>
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 */
public class PetValidator {

	private static final String REQUIRED = "required";

	public static void validate(Pet pet, HashMap<String, List<String>> errors) {
		String name = pet.getName();
		// name validation
		if (!StringUtils.hasText(name)) {
			errors.put("name", List.of(REQUIRED));
		}

		// type validation
		if (pet.isNew() && pet.getType() == null) {
			errors.put("type", List.of(REQUIRED));
		}

		// birth date validation
		if (pet.getBirthDate() == null) {
			errors.put("birthDate", List.of(REQUIRED));
		}
	}
}
