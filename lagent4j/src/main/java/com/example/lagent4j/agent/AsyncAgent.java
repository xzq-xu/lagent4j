package com.example.lagent4j.agent;

import com.example.lagent4j.actions.Action;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface AsyncAgent {
    CompletableFuture<AgentMessage> processAsync(AgentMessage message, long sessionId);
    void setOnMessage(Consumer<AgentMessage> callback);
    void setOnError(Consumer<Throwable> callback);
    void reset(long sessionId);
    List<Action> getTools();
} 