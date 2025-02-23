package com.example.lagent4j;

import com.example.lagent4j.agent.*;
import com.example.lagent4j.actions.*;
import com.example.lagent4j.llm.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class App {
    public static void main(String[] args) throws Exception {
        // 初始化内存和LLM服务
        Memory memory = new LRUMemory(100);
        LlmService llmService = new OpenAILlmService("sk-d5d94cd5925f4a2fb33ad536825f124e");
        
        // 注册可用工具
        List<Action> actions = List.of(
            new HttpRequestAction(),
            new PythonInterpreterAction(),
            new FileIOAction()
        );
        
        // 创建智能体
        SimpleAgent agent = new SimpleAgent(llmService, memory, actions);

        // 构建用户消息
        AgentMessage userMessage = AgentMessage.builder()
            .sender("user")
            .content("请帮我查询天气")
            .type(AgentMessage.AgentMessageType.USER)
            .sessionId(1)
            .build();

        // 处理消息并获取响应
        CompletableFuture<AgentMessage> responseFuture = agent.processAsync(userMessage, 1);
        
        // 处理异步响应
        responseFuture.thenAccept(response -> {
            System.out.println("\nAgent Response:");
            System.out.println(response.getContent());
            
            if (response.getFormatted() instanceof ToolCall toolCall) {
                System.out.println("\n检测到工具调用:");
                System.out.println("工具名称: " + toolCall.getToolName());
                System.out.println("参数: " + toolCall.getParameters());
            }
        }).join(); // 仅用于演示，实际应用中应使用异步处理
    }
}
