package org.springframework.samples.petclinic.system

@JvmRecord
data class ResponseEntity<T>(val body: T, val headers: Map<String, String>, val code: Int)
