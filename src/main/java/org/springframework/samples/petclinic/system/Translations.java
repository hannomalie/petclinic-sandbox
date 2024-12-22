package org.springframework.samples.petclinic.system;

import java.util.*;

public class Translations {
	public static String get(String key, Locale locale) {
		return ResourceBundle.getBundle("messages.messages", locale).getString(key);
	}
}
