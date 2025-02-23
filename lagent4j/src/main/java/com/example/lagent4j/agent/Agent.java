package com.example.lagent4j.agent;

import com.example.lagent4j.actions.Action;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Agent {
    private final Map<String, Action> actions = new HashMap<>();

    // 注册Action
    public void registerAction(String name, Action action) {
        actions.put(name, action);
    }

    // 同步执行
    public AgentMessage execute(AgentMessage message) {
        if (message.getToolName() != null && actions.containsKey(message.getToolName())) {
            Action action = actions.get(message.getToolName());
            String result = null;
            try {
                result = action.executeAsync(message.getExtraInfo()).get().getOutput();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            message.setContent(result);
        }
        return message;
    }

    // 异步执行
    public CompletableFuture<AgentMessage> executeAsync(AgentMessage message) {
        return CompletableFuture.supplyAsync(() -> execute(message));
    }
}
