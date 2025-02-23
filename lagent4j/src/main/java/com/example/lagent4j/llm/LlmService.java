package com.example.lagent4j.llm;

import com.example.lagent4j.agent.AgentMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LlmService {
    CompletableFuture<String> chatAsync(List<AgentMessage> messages);
    void setEndpoint(String endpoint);
}
