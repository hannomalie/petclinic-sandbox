package org.springframework.samples.petclinic

import jakarta.validation.Validation
import jakarta.validation.Validator

class BindingResult : HashMap<String, MutableList<String>>() {
    fun hasErrors(): Boolean {
        return !isEmpty()
    }

    fun hasFieldErrors(fieldName: String): Boolean {
        val errors = get(fieldName)
        return !errors.isNullOrEmpty()
    }

    fun getFieldErrors(fieldName: String): List<String> {
        return getOrDefault(fieldName, ArrayList())
    }

    fun addError(propertyName: String, message: String) {
        val newMessages = ArrayList<String>()
        val messages = putIfAbsent(propertyName, newMessages) ?: newMessages
        messages.add(message)
    }

    val fieldErrors: MutableList<String>
        get() = values
            .flatMap { it.toList() }
            .toMutableList()


    companion object {
        private var validator: Validator = Validation.buildDefaultValidatorFactory().use { factory ->
            factory.validator
        }

        fun validate(obj: Any): BindingResult = BindingResult().apply {
            val violations = validator.validate(obj)
            violations.forEach {
                addError(it.propertyPath.toString(), it.message)
            }
        }
    }
}
