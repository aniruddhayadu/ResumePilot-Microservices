package com.resumepilot.template.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

	// Sabhi controller methods ke pehle log chalega
	@Before("execution(* com.resumepilot.template.controller.*.*(..))")
	public void logBefore(JoinPoint joinPoint) {
		log.info("🚀 Method Execution Started: " + joinPoint.getSignature().getName());
	}

	// Methods khatam hone ke baad log chalega
	@After("execution(* com.resumepilot.template.controller.*.*(..))")
	public void logAfter(JoinPoint joinPoint) {
		log.info("✅ Method Execution Completed: " + joinPoint.getSignature().getName());
	}
}