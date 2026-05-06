package com.resumepilot.jobmatch.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Map;

@FeignClient(name = "ai-service", url = "${ai.service.url:http://localhost:8085}")
public interface AiServiceClient {

    @PostMapping("/ai/analyze-match")
    Map<String, Object> getMatchScoreFromAI(
            @RequestBody Map<String, Object> requestPayload,
            @RequestHeader("User-Role") String userRole,
            @RequestHeader("Subscription-Plan") String subscriptionPlan);
}
