package com.resumepilot.template.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@Tag(name = "Payment Management")
public class PaymentController {

	// 🚀 Tere Environment Variables / application.yml se direct value aayegi!
	@Value("${razorpay.key}")
	private String keyId;

	@Value("${razorpay.secret}")
	private String keySecret;

	@PostMapping("/create-order")
	@Operation(summary = "Create Razorpay Order")
	public ResponseEntity<String> crtOrd(@RequestBody Map<String, Object> req) {
		try {
			int amt = (int) (Double.parseDouble(req.get("amount").toString()) * 100);

			RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", amt);
			orderRequest.put("currency", "INR");
			orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

			Order order = razorpay.orders.create(orderRequest);
			return ResponseEntity.ok(order.get("id").toString());

		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
		}
	}

	@PostMapping("/verify")
	@Operation(summary = "Verify Razorpay Payment")
	public ResponseEntity<String> verifyPayment(@RequestBody Map<String, Object> data) {
		return ResponseEntity.ok("Payment Verified Successfully");
	}
}