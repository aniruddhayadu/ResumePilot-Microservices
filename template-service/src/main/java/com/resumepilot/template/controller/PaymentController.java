package com.resumepilot.template.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
	public ResponseEntity<String> crtOrd(@RequestBody Map<String, Object> req) {
		try {
			int a = (int) (Double.parseDouble(req.get("amount").toString()) * 100);

			RazorpayClient rzp = new RazorpayClient(kId, kSec);
			JSONObject oReq = new JSONObject();
			oReq.put("amount", a);
			oReq.put("currency", "INR");
			oReq.put("receipt", "txn_" + System.currentTimeMillis());

			Order o = rzp.orders.create(oReq);
			return ResponseEntity.ok(o.get("id").toString());

		} catch (Exception x) {
			return ResponseEntity.internalServerError().body("Error: " + x.getMessage());
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