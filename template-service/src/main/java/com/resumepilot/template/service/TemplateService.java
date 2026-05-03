package com.resumepilot.template.service;

import com.resumepilot.template.entity.Template;
import java.util.List;

public interface TemplateService {
	List<Template> getAllTemplates();

	Template saveTemplate(Template template);

	Template getTemplateById(Long id);

	String createPaymentOrder(double amount) throws Exception;
}