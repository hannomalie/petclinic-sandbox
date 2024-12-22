package org.springframework.samples.petclinic.system;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class StaticResourcesConfiguration implements WebMvcConfigurer {
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/resources/**")
			.addResourceLocations("classpath:/static.resources/")
			.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)));

		registry.addResourceHandler("/webjars/**")
			.addResourceLocations("/webjars/");
	}
}
