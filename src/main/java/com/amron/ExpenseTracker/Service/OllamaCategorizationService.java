package com.amron.ExpenseTracker.Service;


import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@EnableRetry
public class OllamaCategorizationService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private final RestTemplate restTemplate;

    public OllamaCategorizationService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Cacheable("ollamaCategories")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String categorizeExpense(String description, String rawCategory) {
        String prompt = """
                [INST] Analyze this transaction and return ONLY the category name:
                Description: %s
                Bank Category: %s
                            
                Choose from: [Groceries, Dining, Utilities, Transportation, 
                Entertainment, Healthcare, Education, Shopping, Travel, Other]
                [/INST]
                """.formatted(description, rawCategory);

        Map<String, Object> request = Map.of(
                "model", "llama3",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", false,
                "options", Map.of("temperature", 0.1)
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                OLLAMA_URL,
                new HttpEntity<>(request),
                Map.class
        );

        return extractCategory(response.getBody());
    }

    private String extractCategory(Map<String, Object> response) {
        try {
            String content = ((Map<String, String>) ((List<?>) response.get("messages")).get(0))
                    .get("content")
                    .trim();

            // Case-insensitive match
            return Arrays.stream(Category.values())
                    .filter(c -> c.name().equalsIgnoreCase(content))
                    .findFirst()
                    .orElse(Category.OTHER)
                    .name();
        } catch (Exception e) {
            return "Other";
        }
    }

    private enum Category {
        GROCERIES, DINING, UTILITIES, TRANSPORTATION,
        ENTERTAINMENT, HEALTHCARE, EDUCATION, SHOPPING, TRAVEL, OTHER
    }
}