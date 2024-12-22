package org.springframework.samples.petclinic.system;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.ResourceCodeResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Templating {

	static CodeResolver codeResolver = new ResourceCodeResolver("templates");
	static gg.jte.TemplateEngine templateEngine = gg.jte.TemplateEngine.create(codeResolver, Paths.get("jte-classes"), ContentType.Html, Templating.class.getClassLoader());
//	static gg.jte.TemplateEngine templateEngine = gg.jte.TemplateEngine.createPrecompiled(ContentType.Html);

	public static String renderView(String viewName, Map<String, Object> variables, BindingResult result) {
		var params = new HashMap<>(variables);
		params.put("fields", new CustomFields(result));

		TemplateOutput output = new StringOutput();
		templateEngine.render(viewName + ".jte", params, output);
		return output.toString();
	}

	public static HttpHeaders htmlHeaders = new HttpHeaders();

	static {
		htmlHeaders.add("Content-Type", "text/html");
	}
	public static class CustomFields {
		private final BindingResult result;

		public CustomFields(BindingResult result) {
			this.result = result;
		}

		public boolean hasErrors() {
			if(result == null) return false;

			return result.hasErrors();
		}
		public boolean hasErrors(String fieldName) {
			if(result == null) return false;

			return result.hasFieldErrors(fieldName);
		}
		public List<FieldError> getFieldErrors(String fieldName) {
			if(result == null) return new ArrayList<>();

			return result.getFieldErrors(fieldName);
		}
		public List<FieldError> getFieldErrors() {
			if(result == null) return new ArrayList<>();

			return result.getFieldErrors();
		}
	}
}
