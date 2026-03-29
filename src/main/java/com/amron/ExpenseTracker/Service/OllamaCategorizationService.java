package com.amron.ExpenseTracker.Service;


import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableRetry
public class OllamaCategorizationService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private final RestTemplate restTemplate;
    /** Latched to false on first connection failure — avoids retrying every call when Ollama is down. */
    private final AtomicBoolean ollamaAvailable = new AtomicBoolean(true);

    public OllamaCategorizationService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Cacheable("ollamaCategories")
    @Retryable(retryFor = {ResourceAccessException.class}, noRetryFor = {ConnectException.class},
               maxAttempts = 2, backoff = @Backoff(delay = 500))
    public String categorizeExpense(String description, String rawCategory) {
        if (!ollamaAvailable.get()) {
            return "Miscellaneous";
        }
        String prompt = """
                Analyze this Indian bank transaction and return ONLY the category name.
                Description: %s
                Bank Category: %s

                Choose exactly one from: [Groceries, Food & Dining, Utilities, Transportation,
                Entertainment, Healthcare, Insurance, Shopping, Travel, Investment, Income,
                Fuel, Gym, Subscription, Housing, Personal, Transfer In, Transfer Out, Other]

                Return only the category name, nothing else.
                """.formatted(description, rawCategory);

        Map<String, Object> request = Map.of(
                "model", "llama3",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", false,
                "options", Map.of("temperature", 0.1)
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    OLLAMA_URL,
                    new HttpEntity<>(request),
                    Map.class
            );
            return extractCategory(response.getBody());
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof ConnectException) {
                // Ollama is not running — stop trying for the rest of this import
                ollamaAvailable.set(false);
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractCategory(Map<String, Object> response) {
        try {
            // Ollama /api/chat response: { "message": { "role": "assistant", "content": "..." } }
            Map<String, String> message = (Map<String, String>) response.get("message");
            String content = message.get("content").trim();

            return Arrays.stream(Category.values())
                    .filter(c -> content.toUpperCase().contains(c.displayName.toUpperCase()))
                    .findFirst()
                    .map(c -> c.displayName)
                    .orElse("Miscellaneous");
        } catch (Exception e) {
            return "Miscellaneous";
        }
    }

    private enum Category {
        GROCERIES("Groceries"), FOOD_AND_DINING("Food & Dining"), UTILITIES("Utilities"),
        TRANSPORTATION("Transportation"), ENTERTAINMENT("Entertainment"), HEALTHCARE("Healthcare"),
        INSURANCE("Insurance"), SHOPPING("Shopping"), TRAVEL("Travel"), INVESTMENT("Investment"),
        INCOME("Income"), FUEL("Fuel"), GYM("Gym"), SUBSCRIPTION("Subscription"),
        HOUSING("Housing"), PERSONAL("Personal"), TRANSFER_IN("Transfer In"),
        TRANSFER_OUT("Transfer Out"), OTHER("Miscellaneous");

        final String displayName;
        Category(String displayName) { this.displayName = displayName; }
    }
}