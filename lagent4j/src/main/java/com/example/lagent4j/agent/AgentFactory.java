package com.example.lagent4j.agent;

import com.example.lagent4j.actions.Action;
import com.example.lagent4j.llm.AsyncLlmService;
import com.example.lagent4j.prompts.OutputFormat;
import java.util.List;

public class AgentFactory {
    private AgentFactory() {}

    public static AsyncStreamAgent createStreamAgent(
            AsyncLlmService llm,
            List<Action> tools,
            OutputFormat outputFormat,
            AgentConfig config) {
        return new AsyncStreamAgent(
            llm,
            new LRUMemory(100),
            tools,
            outputFormat,
            config.getSystemPrompt()
        );
    }

    public static AsyncStreamAgent createStreamAgent(
            AsyncLlmService llm,
            List<Action> tools,
            OutputFormat outputFormat,
            String systemPrompt) {
        return createStreamAgent(
            llm,
            tools,
            outputFormat,
            AgentConfig.builder()
                .name("stream")
                .description("Stream agent")
                .maxRetries(3)
                .timeoutMillis(30000)
                .streamOutput(true)
                .systemPrompt(systemPrompt)
                .build()
        );
    }
} 