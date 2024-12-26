package org.springframework.samples.petclinic;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class BindingResult extends HashMap<String, List<String>> {

	public boolean hasErrors() {
		return !isEmpty();
	}

	public boolean hasFieldErrors(String fieldName) {
		List<String> errors = get(fieldName);
		return errors != null && !errors.isEmpty();
	}

	public List<String> getFieldErrors(String fieldName) {
		return getOrDefault(fieldName, new ArrayList<>());
	}

	public void addError(String propertyName, String message) {
		ArrayList<String> newMessages = new ArrayList<>();
		var messages = putIfAbsent(propertyName, newMessages);
		if (messages == null) {
			messages = newMessages;
		}
		messages.add(message);
	}

	public List<String> getFieldErrors() {
		return values().stream().flatMap(Collection::stream).toList();
	}


	private static Validator validator;
	static {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			validator = factory.getValidator();
		}
	}
	public static @NotNull BindingResult validate(Object object) {
		var violations = validator.validate(object);
		var bindingResult = new BindingResult();
		violations.forEach(it -> bindingResult.addError(it.getPropertyPath().toString(), it.getMessage()));
		return bindingResult;
	}
}
