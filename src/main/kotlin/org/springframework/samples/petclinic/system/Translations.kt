package org.springframework.samples.petclinic.system

import java.util.*

object Translations {
	fun get(key: String, locale: Locale): String = ResourceBundle.getBundle("messages.messages", locale).getString(key)
}
