package org.springframework.samples.petclinic.system;

import java.util.Map;

public record ResponseEntity<T>(T body, Map<String, String> headers, int code) {
}
