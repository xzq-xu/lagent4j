package com.example.lagent4j.llm;

import com.example.lagent4j.agent.AgentMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface AsyncLlmService {
    /**
     * 异步聊天，返回完整响应
     */
    CompletableFuture<String> chatAsync(List<AgentMessage> messages);
    
    /**
     * 异步流式聊天，通过回调返回部分响应
     */
    CompletableFuture<Void> chatStreamAsync(
        List<AgentMessage> messages,
        Consumer<String> onResponse,
        Consumer<Throwable> onError
    );
    
    /**
     * 设置API端点
     */
    void setEndpoint(String endpoint);

    /**
     * 设置超时时间
     */
    void setTimeout(long timeoutMillis);
} 