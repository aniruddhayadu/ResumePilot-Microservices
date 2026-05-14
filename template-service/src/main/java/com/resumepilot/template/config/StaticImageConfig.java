package com.resumepilot.template.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticImageConfig implements WebMvcConfigurer {

	private static final String STATIC_IMAGE_LOCATION = "classpath:/static/images/";

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/images/**")
				.addResourceLocations(STATIC_IMAGE_LOCATION);
		registry.addResourceHandler("/templates/images/**")
				.addResourceLocations(STATIC_IMAGE_LOCATION);
	}
}
