package com.example.lagent4j.agent;

import com.example.lagent4j.llm.AsyncLlmService;
import com.example.lagent4j.prompts.OutputFormat;
import com.example.lagent4j.actions.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AsyncSimpleAgent extends AsyncBaseAgent {
    private final OutputFormat outputFormat;
    private final String systemPrompt;
    
    public AsyncSimpleAgent(
            AsyncLlmService llmService,
            Memory memory,
            List<Action> actions,
            OutputFormat outputFormat,
            String systemPrompt) {
        super(llmService, memory, actions);
        this.outputFormat = outputFormat;
        this.systemPrompt = systemPrompt;
    }

    @Override
    protected CompletableFuture<AgentMessage> handleMessageAsync(AgentMessage message, long sessionId) {
        List<AgentMessage> history = new ArrayList<>(memory.get(sessionId));
        
        return llmService.chatAsync(history)
            .thenApply(response -> {
                Object formatted = null;
                if (outputFormat != null) {
                    formatted = outputFormat.parseResponse(response);
                }
                
                return AgentMessage.builder()
                    .sender("assistant")
                    .content(response)
                    .formatted(formatted)
                    .type(AgentMessage.AgentMessageType.ASSISTANT)
                    .sessionId(sessionId)
                    .build();
            });
    }

    @Override
    public void reset(long sessionId) {

    }

    @Override
    public List<Action> getTools() {
        return List.of();
    }
}