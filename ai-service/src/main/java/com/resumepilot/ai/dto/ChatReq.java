package com.resumepilot.ai.dto;

import java.util.List;

public class ChatReq {
	private String model;
	private List<Msg> messages;
	private double temperature = 0.7;
	private int max_tokens = 150;

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	} 

	public List<Msg> getMessages() {
		return messages;
	}

	public void setMessages(List<Msg> messages) {
		this.messages = messages;
	}

	public double getTemperature() {
		return temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public int getMax_tokens() {
		return max_tokens;
	}

	public void setMax_tokens(int max_tokens) {
		this.max_tokens = max_tokens;
	}
}