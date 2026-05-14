package com.resumepilot.template.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private String thumbnailUrl;
	private String type;

	@JsonProperty("isPremium")
	@Column(name = "is_premium")
	private Boolean isPremium;

	private double price;
}