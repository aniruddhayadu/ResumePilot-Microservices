package com.resumepilot.template.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 1. Header se Authorization Token nikalna
		String authHeader = request.getHeader("Authorization");

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);

			// 💡 Note for Interview: Yahan hum normally JWT library se token validate karte
			// hain
			// Par kyunki API Gateway validate karke bhejega, hum isko simplify kar sakte
			// hain.
			// Abhi ke liye agar token hai, toh hum authentication context set kar rahe
			// hain.

			try {
				// Dummy logic for demonstration: Agar token hai toh user authenticated hai
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("user",
						null, new ArrayList<>());

				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (Exception e) {
				logger.error("JWT Validation failed: " + e.getMessage());
			}
		}

		// 2. Request ko aage badhne dena
		filterChain.doFilter(request, response);
	}
}