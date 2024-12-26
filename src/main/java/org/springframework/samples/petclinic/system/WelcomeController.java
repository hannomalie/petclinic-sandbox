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

package org.springframework.samples.petclinic.system;

import io.javalin.http.Context;
import org.eclipse.jetty.server.Response;

import java.util.HashMap;

import static org.springframework.samples.petclinic.PetClinicApplication.setResponse;
import static org.springframework.samples.petclinic.system.Templating.htmlHeaders;
import static org.springframework.samples.petclinic.system.Templating.renderView;

public class WelcomeController {
	public void welcome(Context ctx) {
		var locale = ctx.req().getLocale();
		var model = new HashMap<String, Object>();
		model.put("welcome", Translations.get("welcome", locale));
		setResponse(ctx, new ResponseEntity<>(renderView("welcome", model, null), htmlHeaders, Response.SC_OK));
	}
}
