package com.resumepilot.jobmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class JobmatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobmatchServiceApplication.class, args);
        System.out.println("JobmatchService is running....");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}