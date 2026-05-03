package com.resumepilot.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TemplateServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TemplateServiceApplication.class, args);
		System.out.println("Template Service is Running..");
	}

}
