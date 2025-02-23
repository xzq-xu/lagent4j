package com.example.lagent4j.agent;

import com.example.lagent4j.llm.LlmService;
import com.example.lagent4j.actions.Action;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class BaseAgent {
    protected final LlmService llmService;
    protected final Memory memory;
    protected final List<Action> actions;

    public BaseAgent(LlmService llmService, Memory memory, List<Action> actions) {
        this.llmService = llmService;
        this.memory = memory;
        this.actions = actions;
    }

    public CompletableFuture<AgentMessage> processAsync(AgentMessage message, Integer sessionId) {
        memory.addMessage(sessionId, message);
        return handleMessage(message)
            .thenCompose(response -> {
                memory.addMessage(sessionId, response);
                return checkForToolCall(response, sessionId);
            });
    }

    private CompletableFuture<AgentMessage> checkForToolCall(AgentMessage response, Integer sessionId) {
        if (response.getFormatted() instanceof ToolCall toolCall) {
            return executeTool(toolCall, sessionId)
                .thenCompose(toolResult -> {
                    memory.addMessage(sessionId, toolResult);
                    return processAsync(toolResult, sessionId);
                });
        }
        return CompletableFuture.completedFuture(response);
    }

    private CompletableFuture<AgentMessage> executeTool(ToolCall toolCall, Integer sessionId) {
        return actions.stream()
            .filter(action -> toolCall.getToolName().equals(action.getName()))
            .findFirst()
            .map(action -> action.executeAsync(toolCall.getParameters())
                .thenApply(result -> createToolResultMessage(toolCall, result))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return createToolResultMessage(toolCall, 
                        ActionResult.builder()
                            .status(ActionResult.Status.FAILURE)
                            .error(ex.getMessage())
                            .build());
                }))
            .orElseGet(() -> CompletableFuture.supplyAsync(() -> 
                createToolResultMessage(toolCall, 
                    ActionResult.builder()
                        .status(ActionResult.Status.FAILURE)
                        .error("Tool not found: " + toolCall.getToolName())
                        .build())));
    }

    private AgentMessage createToolResultMessage(ToolCall toolCall, ActionResult result) {
        return AgentMessage.builder()
            .sender("Tool")
            .content(result.getOutput())
            .formatted(new ToolResult(toolCall.getToolName(), result))
            .type(AgentMessage.AgentMessageType.TOOL)
            .build();
    }

    protected abstract CompletableFuture<AgentMessage> handleMessage(AgentMessage message);
}
