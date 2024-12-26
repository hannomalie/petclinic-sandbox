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

import java.time.LocalDate;
import java.util.HashMap;

import static org.springframework.samples.petclinic.PetClinicApplication.setResponse;
import static org.springframework.samples.petclinic.system.Templating.htmlHeaders;
import static org.springframework.samples.petclinic.system.Templating.renderView;

/**
 * Controller used to showcase what happens when an exception is thrown
 *
 * @author Michael Isvy
 * <p/>
 * Also see how a view that resolves to "error" has been added ("error.html").
 */
public class CrashController {
	public void triggerException(Context ctx) {
		var acceptHeader = ctx.header("Accept");
		if(acceptHeader != null && acceptHeader.equals("application/json")) {

			var jsonHeaders = new HashMap<String, String>();
			jsonHeaders.put("Content-Type", "application/json");
			setResponse(ctx, new ResponseEntity<>("{ " +
				"\"timestamp\":\"" + LocalDate.now() + "\", " +
				"\"status\": 500, " +
				"\"path\": \"/oups\", " +
				"\"error\": \"Expected: controller used to showcase what happens when an exception is thrown\", " +
				"\"message\": \"Expected: controller used to showcase what happens when an exception is thrown\"" +
				" }", jsonHeaders, Response.SC_INTERNAL_SERVER_ERROR));
		} else {
			var model = new HashMap<String, Object>();
			model.put("message", "Expected: controller used to showcase what happens when an exception is thrown");
			setResponse(ctx, new ResponseEntity<>(renderView("error", model, null), htmlHeaders, Response.SC_INTERNAL_SERVER_ERROR));
		}
	}

}
