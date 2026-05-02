package com.resumepilot.auth.controller;

import com.resumepilot.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

	@Autowired
	private UserRepository userRepository;

	// 🚀 Endpoint 1: Dashboard Stats
	@GetMapping("/stats")
	public ResponseEntity<?> getAdminStats() {
		Map<String, Object> stats = new HashMap<>();
		try {
			long totalUsers = userRepository.count();
			stats.put("totalUsers", totalUsers);
			stats.put("resumesBuilt", 156);
			stats.put("atsScansDone", 89);
			stats.put("activeUsers", 1);
			return ResponseEntity.ok(stats);
		} catch (Exception e) {
			// Agar DB fail hua toh error message bhejega bajaye crash hone ke
			stats.put("totalUsers", 0);
			stats.put("error", e.getMessage());
			return ResponseEntity.status(200).body(stats); // 200 bhej rahe taaki React crash na ho
		}
	}

	// 🚀 Endpoint 2: Saare Users ki List
	@GetMapping("/users")
	public ResponseEntity<?> getAllUsers() {
		try {
			return ResponseEntity.ok(userRepository.findAll());
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error fetching users");
		}
	}

	// 🚀 Endpoint 3: User ko udane (Delete) karne ke liye
	@DeleteMapping("/users/{id}")
	public ResponseEntity<?> deleteUser(@PathVariable Long id) {
		try {
			if (userRepository.existsById(id)) {
				userRepository.deleteById(id);
				return ResponseEntity.ok().body(Map.of("message", "User deleted successfully"));
			} else {
				return ResponseEntity.status(404).body(Map.of("error", "User not found"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", "Failed to delete user"));
		}
	}
}