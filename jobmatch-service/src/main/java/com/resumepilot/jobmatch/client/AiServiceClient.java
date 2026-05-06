package com.resumepilot.jobmatch.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "ai-service") 
public interface AiServiceClient {

    @PostMapping("/ai/analyze-match") 
    Map<String, Object> getMatchScoreFromAI(@RequestBody Map<String, Object> requestPayload);
}