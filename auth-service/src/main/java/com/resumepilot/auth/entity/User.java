package com.resumepilot.auth.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "`user`") // Backticks zaroori hain reserved keyword ke liye
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id") // DB column 'user_id' se match kiya
	private Long userId;

	@Column(name = "full_name") // DB column 'full_name'
	private String fullName;

	@Column(unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false) // DB column 'password_hash'
	private String passwordHash;

	private String phone;
	private String role;

	@Column(name = "subscription_plan") // DB column 'subscription_plan'
	private String subscriptionPlan;

	@Column(name = "is_active") // DB column 'is_active'
	private boolean isActive = true;

	@Column(name = "created_at") // DB column 'created_at'
	private LocalDateTime createdAt;

	private String resetToken;
	private LocalDateTime resetTokenExpiry;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	// AuthServiceImpl ke saath purani compatibility ke liye
	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		isActive = active;
	}
}