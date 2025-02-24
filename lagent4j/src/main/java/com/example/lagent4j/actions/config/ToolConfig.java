package com.example.lagent4j.actions.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolConfig {
    private final String name;
    private final String description;
    private final int timeout;
    private final int maxRetries;
    private final boolean async;
    
    public static ToolConfig getDefault(String name) {
        return ToolConfig.builder()
            .name(name)
            .description("Default tool configuration")
            .timeout(30)
            .maxRetries(3)
            .async(true)
            .build();
    }
} 