package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Action {
    String getName();
    CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters);
}
