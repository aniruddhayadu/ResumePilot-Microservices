package com.resumepilot.template.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.resumepilot.template.entity.Template;
import com.resumepilot.template.repository.TemplateRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceImpl implements TemplateService {

	private final TemplateRepository templateRepository;

	@Value("${razorpay.key}")
	private String razorpayKey;

	@Value("${razorpay.secret}")
	private String razorpaySecret;

	@Override
	@CircuitBreaker(name = "templateService", fallbackMethod = "fallbackGetTemplates")
	public List<Template> getAllTemplates() {
		log.info("🚀 Fetching all templates from DB...");
		return templateRepository.findAll();
	}

	@Override
	public Template saveTemplate(Template template) {
		log.info("💾 Saving new template: {}", template.getName());
		return templateRepository.save(template);
	}

	@Override
	public Template getTemplateById(Long id) {
		return templateRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Template not found with id: " + id));
	}

	// Razorpay Order Creation Logic
	@Override
	public String createPaymentOrder(double amount) throws Exception {
		log.info("💳 Creating Razorpay Order for amount: {}", amount);

		RazorpayClient client = new RazorpayClient(razorpayKey, razorpaySecret);

		JSONObject orderRequest = new JSONObject();
		orderRequest.put("amount", (int) (amount * 100)); // Amount in paise
		orderRequest.put("currency", "INR");
		orderRequest.put("receipt", "txn_template_resumepilot");

		Order order = client.orders.create(orderRequest);
		return order.get("id"); // Ye Order ID frontend ko jayegi checkout ke liye
	}

	// Fallback Method for Circuit Breaker
	public List<Template> fallbackGetTemplates(Throwable t) {
		log.error("⚠️ Circuit Breaker Triggered! DB issues: {}", t.getMessage());
		return Collections.emptyList();
	}
}