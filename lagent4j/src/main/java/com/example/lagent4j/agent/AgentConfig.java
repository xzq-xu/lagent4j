package com.example.lagent4j.agent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentConfig {
    private final String name;
    private final String description;
    private final int maxRetries;
    private final long timeoutMillis;
    private final boolean streamOutput;
    private final String systemPrompt;

    public static AgentConfig getDefault() {
        return AgentConfig.builder()
            .name("default")
            .description("Default agent configuration")
            .maxRetries(3)
            .timeoutMillis(30000)
            .streamOutput(true)
            .systemPrompt("You are a helpful assistant.")
            .build();
    }
} 