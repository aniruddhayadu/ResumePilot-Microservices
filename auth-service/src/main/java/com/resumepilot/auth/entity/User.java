package com.resumepilot.auth.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "`user`")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "full_name")
	private String fullName;

	@Column(unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	private String phone;

	private String role;

	@Column(name = "subscription_plan")
	private String subscriptionPlan;

	@Column(name = "is_active")
	private boolean isActive = true;

	@Column(name = "is_verified", columnDefinition = "boolean default false")
	private boolean isVerified;

	@Column(name = "otp")
	private String otp;

	@Column(name = "otp_expiry")
	private LocalDateTime otpExpiry;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "reset_token")
	private String resetToken;

	@Column(name = "reset_token_expiry")
	private LocalDateTime resetTokenExpiry;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		this.isActive = active;
	}
}