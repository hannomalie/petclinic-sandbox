package org.springframework.samples.petclinic.system

import gg.jte.CodeResolver
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.TemplateOutput
import gg.jte.output.StringOutput
import gg.jte.resolve.ResourceCodeResolver
import org.springframework.samples.petclinic.BindingResult
import java.nio.file.Paths

object Templating {
    var codeResolver: CodeResolver = ResourceCodeResolver("templates")
    var templateEngine: TemplateEngine = TemplateEngine.create(
        codeResolver,
        Paths.get("jte-classes"),
        ContentType.Html,
        Templating::class.java.classLoader
    )

    //	static gg.jte.TemplateEngine templateEngine = gg.jte.TemplateEngine.createPrecompiled(ContentType.Html);
    fun renderView(viewName: String, variables: Map<String, Any>, result: BindingResult?): String = StringOutput().let { output ->
        val params = HashMap(variables).apply {
            put("fields", CustomFields(result))
        }
        templateEngine.render("$viewName.jte", params, output)
        output.toString()
    }

	var htmlHeaders: Map<String, String> = mapOf("Content-Type" to "text/html")

    class CustomFields(private val result: BindingResult?) {
        fun hasErrors(): Boolean = result?.hasErrors() ?: false
        fun hasErrors(fieldName: String): Boolean = result?.hasFieldErrors(fieldName) ?: false
        fun getFieldErrors(fieldName: String): List<String> = result?.getFieldErrors(fieldName) ?: ArrayList()
        val fieldErrors: List<String> get() = result?.fieldErrors ?: ArrayList()
    }
}
