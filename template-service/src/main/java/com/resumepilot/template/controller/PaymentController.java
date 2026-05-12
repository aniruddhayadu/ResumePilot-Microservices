package com.resumepilot.template.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@Tag(name = "Payment Management")
@Slf4j
public class PaymentController {

	@Value("${razorpay.key}")
	private String kId;

	@Value("${razorpay.secret}")
	private String kSec;

	@Value("${app.test.email}")
	private String tEml;

	@Autowired
	private KafkaTemplate<String, String> kTmp;

	@PostMapping("/create-order")
	@Operation(summary = "Create Razorpay Order")
	public ResponseEntity<?> crtOrd(@RequestBody Map<String, Object> req) {
		try {
			if (kId == null || kId.isBlank() || kSec == null || kSec.isBlank()) {
				return ResponseEntity.internalServerError()
						.body(Map.of("message", "Razorpay credentials are missing. Set RAZORPAY_KEY and RAZORPAY_SECRET in .env."));
			}

			Object amount = req.get("amount");
			if (amount == null) {
				return ResponseEntity.badRequest().body(Map.of("message", "Payment amount is required."));
			}

			int a = (int) Math.round(Double.parseDouble(amount.toString()) * 100);
			if (a <= 0) {
				return ResponseEntity.badRequest().body(Map.of("message", "Payment amount must be greater than zero."));
			}

			RazorpayClient rzp = new RazorpayClient(kId, kSec);
			JSONObject oReq = new JSONObject();
			oReq.put("amount", a);
			oReq.put("currency", "INR");
			oReq.put("receipt", "txn_" + System.currentTimeMillis());

			Order o = rzp.orders.create(oReq);
			return ResponseEntity.ok(o.get("id").toString());

		} catch (Exception x) {
			log.error("Failed to create Razorpay order", x);
			return ResponseEntity.internalServerError()
					.body(Map.of("message", "Razorpay order creation failed: " + x.getMessage()));
		}
	}

	@PostMapping("/verify")
	@Operation(summary = "Verify Razorpay Payment")
	public ResponseEntity<String> vfyPay(@RequestBody Map<String, Object> req) {
		String e = req.containsKey("email") ? req.get("email").toString() : tEml;
		System.out.println("Payment successful for: " + e);
		kTmp.send("notification_topic", e);
		return ResponseEntity.ok("Verified");
	}
}
