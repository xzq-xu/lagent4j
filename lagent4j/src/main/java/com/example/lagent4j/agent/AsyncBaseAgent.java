package com.example.lagent4j.agent;

import com.example.lagent4j.llm.AsyncLlmService;
import com.example.lagent4j.actions.Action;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public abstract class AsyncBaseAgent implements AsyncAgent {
    protected final AsyncLlmService llmService;
    protected final Memory memory;
    protected final List<Action> actions;
    protected Consumer<AgentMessage> onMessage;
    protected Consumer<Throwable> onError;

    public AsyncBaseAgent(
            AsyncLlmService llmService,
            Memory memory,
            List<Action> actions) {
        this.llmService = llmService;
        this.memory = memory;
        this.actions = actions;
    }

    @Override
    public CompletableFuture<AgentMessage> processAsync(AgentMessage message, long sessionId) {
        memory.add(sessionId, message);
        return handleMessageAsync(message, sessionId)
            .thenCompose(response -> {
                memory.add(sessionId, response);
                if (onMessage != null) {
                    onMessage.accept(response);
                }
                return checkForToolCall(response, sessionId);
            })
            .exceptionally(ex -> {
                if (onError != null) {
                    onError.accept(ex);
                }
                throw new CompletionException(ex);
            });
    }

    @Override
    public void reset(long sessionId) {
         CompletableFuture.runAsync(() -> memory.clear(sessionId));
    }

    private CompletableFuture<AgentMessage> checkForToolCall(AgentMessage response, long sessionId) {
        if (response.getFormatted() instanceof ToolCall toolCall) {
            return executeTool(toolCall, sessionId)
                .thenCompose(toolResult -> {
                    memory.add(sessionId, toolResult);
                    if (onMessage != null) {
                        onMessage.accept(toolResult);
                    }
                    return processAsync(toolResult, sessionId);
                });
        }
        return CompletableFuture.completedFuture(response);
    }

    private CompletableFuture<AgentMessage> executeTool(ToolCall toolCall, long sessionId) {
        return actions.stream()
            .filter(action -> action.getName().equals(toolCall.getToolName()))
            .findFirst()
            .map(action -> action.executeAsync(toolCall.getParameters())
                .thenApply(result -> AgentMessage.builder()
                    .sender("Tool")
                    .content(result.getOutput())
                    .formatted(new ToolResult(toolCall.getToolName(), result))
                    .type(AgentMessage.AgentMessageType.TOOL)
                    .sessionId(sessionId)
                    .build()))
            .orElseGet(() -> CompletableFuture.failedFuture(
                new IllegalArgumentException("Tool not found: " + toolCall.getToolName())));
    }

    public void setOnMessage(Consumer<AgentMessage> onMessage) {
        this.onMessage = onMessage;
    }

    public void setOnError(Consumer<Throwable> onError) {
        this.onError = onError;
    }

    protected abstract CompletableFuture<AgentMessage> handleMessageAsync(
        AgentMessage message, 
        long sessionId
    );
} 