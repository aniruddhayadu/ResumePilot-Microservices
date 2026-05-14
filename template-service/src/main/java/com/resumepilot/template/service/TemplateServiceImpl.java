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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceImpl implements TemplateService {

	private final TemplateRepository templateRepository;

	@Value("${razorpay.key}")
	private String razorpayKey;

	@Value("${razorpay.secret}")
	private String razorpaySecret;

	@Value("${app.gateway-base-url:http://localhost:8080}")
	private String gatewayBaseUrl;

	@Override
	@CircuitBreaker(name = "templateService", fallbackMethod = "fallbackGetTemplates")
	public List<Template> getAllTemplates() {
		log.info("Fetching all templates from DB...");
		return templateRepository.findAll().stream()
				.map(this::withGatewayThumbnailUrl)
				.collect(Collectors.toList());
	}

	@Override
	public Template saveTemplate(Template template) {
		log.info("Saving new template: {}", template.getName());
		return templateRepository.save(template);
	}

	@Override
	public Template getTemplateById(Long id) {
		return templateRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Template not found with id: " + id));
	}

	@Override
	public String createPaymentOrder(double amount) throws Exception {
		log.info("Creating Razorpay Order for amount: {}", amount);

		RazorpayClient client = new RazorpayClient(razorpayKey, razorpaySecret);

		JSONObject orderRequest = new JSONObject();
		orderRequest.put("amount", (int) (amount * 100));
		orderRequest.put("currency", "INR");
		orderRequest.put("receipt", "txn_template_resumepilot");

		Order order = client.orders.create(orderRequest);
		return order.get("id");
	}

	public List<Template> fallbackGetTemplates(Throwable t) {
		log.error("Circuit breaker fallback for templates: {}", t.getMessage());
		return Collections.emptyList();
	}

	private Template withGatewayThumbnailUrl(Template template) {
		String thumbnailUrl = template.getThumbnailUrl();
		if (thumbnailUrl == null || thumbnailUrl.isBlank() || thumbnailUrl.startsWith("http")) {
			return template;
		}

		String baseUrl = gatewayBaseUrl.endsWith("/")
				? gatewayBaseUrl.substring(0, gatewayBaseUrl.length() - 1)
				: gatewayBaseUrl;
		String path = normalizeThumbnailPath(thumbnailUrl);

		return Template.builder()
				.id(template.getId())
				.name(template.getName())
				.thumbnailUrl(baseUrl + path)
				.type(template.getType())
				.isPremium(template.getIsPremium())
				.price(template.getPrice())
				.build();
	}

	private String normalizeThumbnailPath(String thumbnailUrl) {
		String path = thumbnailUrl.startsWith("/") ? thumbnailUrl : "/" + thumbnailUrl;
		if (path.startsWith("/templates/images/")) {
			return path;
		}
		if (path.startsWith("/images/")) {
			return "/templates" + path;
		}
		return "/templates/images" + path;
	}
}
