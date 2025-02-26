package io.github.internlm.lagent4j.examples;

import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式输出示例
 * <p>
 * 展示如何使用lagent4j框架处理流式输出
 */
public class StreamExample {
    public static void main(String[] args) throws InterruptedException {
        String apiKey = System.getenv("LAGENT4J_MODEL_API_KEY");
        String baseUrl = System.getenv("LAGENT4J_MODEL_API_URL");
        String modelName = System.getenv("LAGENT4J_MODEL_NAME");

        // 创建OpenAI模型
        OpenAIModel llm = new OpenAIModel(apiKey,baseUrl,modelName);
        
        // 创建代理
        String systemPrompt = "你是一个有用的助手，能够回答用户的各种问题。";
        Agent agent = new Agent(llm, systemPrompt);
        
        // 发送消息
        String question = "请详细介绍一下Java的历史和发展。";
        AgentMessage userMsg = new AgentMessage("user", question);
        
        // 使用同步方式获取回复
        System.out.println("同步方式:");
        System.out.println("用户: " + question);
        
        AgentMessage botMsg = agent.process(userMsg);
        System.out.println("助手: " + botMsg.getContent());
        
        // 使用流式方式获取回复
        System.out.println("\n流式方式:");
        System.out.println("用户: " + question);
        System.out.print("助手: ");
        
        // 创建一个CountDownLatch来等待流式输出完成
        CountDownLatch latch = new CountDownLatch(1);
        
        // 存储完整的响应
        AtomicReference<String> fullResponse = new AtomicReference<>("");
        
        // 使用流式API
        llm.chatStream(
            // 聚合消息
            agent.getAggregator().aggregate(
                agent.getMemory().get(0), 
                agent.getName(), 
                agent.getOutputFormat(), 
                agent.getTemplate()
            ),
            // 处理每个文本块
            chunk -> {
                System.out.print(chunk);
                fullResponse.updateAndGet(current -> current + chunk);
            },
            // 处理错误
            error -> {
                System.err.println("\n发生错误: " + error.getMessage());
                latch.countDown();
            },
            // 处理完成
            () -> {
                System.out.println("\n流式输出完成");
                latch.countDown();
            }
        );
        
        // 等待流式输出完成
        latch.await();
        
        // 更新代理的内存
        AgentMessage streamMsg = new AgentMessage(agent.getName(), fullResponse.get());
        agent.updateMemory(streamMsg, 0);
        
        // 继续对话
        userMsg = new AgentMessage("user", "Java 8引入了哪些新特性？");
        
        System.out.println("\n用户: " + userMsg.getContent());
        botMsg = agent.process(userMsg);
        System.out.println("助手: " + botMsg.getContent());
    }
} 