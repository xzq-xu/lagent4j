package com.example.lagent4j.agent;

import com.example.lagent4j.llm.LlmService;
import com.example.lagent4j.actions.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SimpleAgent extends BaseAgent {

    // 构造函数，传入llmService、memory和actions
    public SimpleAgent(LlmService llmService, Memory memory, List<Action> actions) {
        super(llmService, memory, actions);
    }

    // 重写handleMessage方法，处理消息
    @Override
    protected CompletableFuture<AgentMessage> handleMessage(AgentMessage message) {
        // 获取消息的sessionId
        long sessionId = message.getSessionId();
        // 获取消息历史
        List<AgentMessage> history = new ArrayList<>(memory.get(sessionId));
        // 调用llmService的chatAsync方法，传入消息历史，返回CompletableFuture<AgentMessage>
        return llmService.chatAsync(history)
            // 使用thenApply方法，将返回的response封装成AgentMessage对象
            .thenApply(response -> AgentMessage.builder()
                // 设置消息发送者为"assistant"
                .sender("assistant")
                // 设置消息内容为response
                .content(response)
                // 设置消息类型为AGENT
                .type(AgentMessage.AgentMessageType.ASSISTANT)
                // 设置消息sessionId
                .sessionId(sessionId)
                // 构建AgentMessage对象
                .build());
    }
}
