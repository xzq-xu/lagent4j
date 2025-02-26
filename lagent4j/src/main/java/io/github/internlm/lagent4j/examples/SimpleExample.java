package io.github.internlm.lagent4j.examples;

import io.github.internlm.lagent4j.agents.SyncAgent;
import io.github.internlm.lagent4j.agents.aggregator.DefaultAggregator;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.prompts.parsers.StrParser;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * 简单示例
 * <p>
 * 演示如何创建一个基本的代理并与之交互
 */
public class SimpleExample {
    public static void main(String[] args) {
        // 检查环境变量
        // 检查环境变量
        String apiKey = System.getenv("LAGENT4J_MODEL_API_KEY");
        String baseUrl = System.getenv("LAGENT4J_MODEL_API_URL");
        String modelName = System.getenv("LAGENT4J_MODEL_NAME");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("请设置OPENAI_API_KEY环境变量");
            return;
        }

        // 创建OpenAI模型
        OpenAIModel model = new OpenAIModel.Builder()
                .apiKey(apiKey)
                .model(modelName)
                .baseUrl(baseUrl)
                .temperature(0.7)
                .maxTokens(2000)
                .build();

        // 创建代理配置
        Map<String, Object> memory = new HashMap<>();
        String template = "你是一个友好的AI助手，请用中文回答用户的问题。";

        // 创建同步代理
        SyncAgent agent = new SyncAgent(
                model,
                template,
                memory,
                new StrParser(),
                new DefaultAggregator(),
                null,
                "AI助手",
                "一个友好的中文AI助手"
        );

        // 发送消息并获取响应
        AgentMessage response = agent.process(
                new AgentMessage("user", "你好，请介绍一下你自己。"),
                1
        );

        // 打印响应
        System.out.println("AI助手: " + response.getContent());
    }
} 