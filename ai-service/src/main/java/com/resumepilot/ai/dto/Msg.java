package com.resumepilot.ai.dto;

public class Msg {
	private String role;
	private String content;

	public Msg() {
	}

	public Msg(String role, String content) {
		this.role = role;
		this.content = content;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}