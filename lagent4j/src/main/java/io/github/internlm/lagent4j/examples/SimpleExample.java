package io.github.internlm.lagent4j.examples;

import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.schema.AgentMessage;

/**
 * 简单示例
 * <p>
 * 展示如何使用lagent4j框架创建一个简单的对话代理
 */
public class SimpleExample {
    public static void main(String[] args) {

        String apiKey = System.getenv("LAGENT4J_MODEL_API_KEY");
        String baseUrl = System.getenv("LAGENT4J_MODEL_API_URL");
        String modelName = System.getenv("LAGENT4J_MODEL_NAME");

        // 创建OpenAI模型
        OpenAIModel llm = new OpenAIModel(apiKey,baseUrl,modelName);
        
        // 创建代理
        String systemPrompt = "你是一个有用的助手，能够回答用户的各种问题。";
        Agent agent = new Agent(llm, systemPrompt);
        
        // 发送消息并获取回复
        AgentMessage userMsg = new AgentMessage("user", "Java和Python有什么区别？");
        AgentMessage botMsg = agent.process(userMsg);
        
        // 打印回复
        System.out.println("用户: " + userMsg.getContent());
        System.out.println("助手: " + botMsg.getContent());
        
        // 继续对话
        userMsg = new AgentMessage("user", "哪一个更适合初学者？");
        botMsg = agent.process(userMsg);
        
        // 打印回复
        System.out.println("用户: " + userMsg.getContent());
        System.out.println("助手: " + botMsg.getContent());
        
        // 重置对话
        agent.reset();
        
        // 开始新的对话
        userMsg = new AgentMessage("user", "什么是人工智能？");
        botMsg = agent.process(userMsg);
        
        // 打印回复
        System.out.println("\n新对话:");
        System.out.println("用户: " + userMsg.getContent());
        System.out.println("助手: " + botMsg.getContent());
    }
} 