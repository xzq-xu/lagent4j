package com.example.lagent4j.actions;

import com.example.lagent4j.agent.AgentMessage;
import com.example.lagent4j.agent.ActionResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.concurrent.CompletionException;

public class AsyncActionExecutor {
    private final Map<String, Action> actions;
    private Consumer<AgentMessage> onMessage;
    private Consumer<Throwable> onError;

    public AsyncActionExecutor(List<Action> actions) {
        this.actions = actions.stream()
            .collect(java.util.stream.Collectors.toMap(
                Action::getName,
                action -> action
            ));
    }

    public CompletableFuture<AgentMessage> executeAsync(AgentMessage message, Integer sessionId) {
        if (!(message.getFormatted() instanceof Map)) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Invalid tool call format"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> toolCall = (Map<String, Object>) message.getFormatted();
        String toolName = (String) toolCall.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) toolCall.get("parameters");

        Action action = actions.get(toolName);
        if (action == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Tool not found: " + toolName));
        }

        return action.executeAsync(parameters)
            .thenApply(result -> {
                AgentMessage response = AgentMessage.builder()
                    .sender("Tool")
                    .content(result.getOutput())
                    .type(AgentMessage.AgentMessageType.TOOL)
                    .sessionId(sessionId)
                    .build();
                
                if (onMessage != null) {
                    onMessage.accept(response);
                }
                
                return response;
            })
            .exceptionally(ex -> {
                if (onError != null) {
                    onError.accept(ex);
                }
                throw new CompletionException(ex);
            });
    }

    public void setOnMessage(Consumer<AgentMessage> onMessage) {
        this.onMessage = onMessage;
    }

    public void setOnError(Consumer<Throwable> onError) {
        this.onError = onError;
    }
} 