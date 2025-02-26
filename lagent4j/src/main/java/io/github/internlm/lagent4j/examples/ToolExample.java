package io.github.internlm.lagent4j.examples;

import io.github.internlm.lagent4j.actions.Action;
import io.github.internlm.lagent4j.actions.ActionExecutor;
import io.github.internlm.lagent4j.actions.WebBrowser;
import io.github.internlm.lagent4j.actions.WebSearch;
import io.github.internlm.lagent4j.agents.SyncAgent;
import io.github.internlm.lagent4j.agents.aggregator.DefaultAggregator;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.prompts.parsers.JsonParser;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具使用示例
 * <p>
 * 展示如何使用多个工具协同工作，包括：
 * 1. 网页搜索工具：搜索相关信息
 * 2. 网页浏览工具：获取和分析网页内容
 * 3. 工具组合使用：先搜索后浏览
 * 4. 并行工具执行：同时处理多个任务
 * 5. 错误处理：处理工具执行失败的情况
 */
public class ToolExample {
    public static void main(String[] args) {
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

        // 创建工具执行器
        ActionExecutor executor = new ActionExecutor();
        
        // 注册工具
        Action webBrowser = new WebBrowser();
        Action webSearch = new WebSearch();
        executor.registerAction(webBrowser);
        executor.registerAction(webSearch);

        // 创建系统提示
        String template = """
            你是一个专业的信息分析助手，可以帮助用户搜索、获取和分析网页信息。
            
            在处理用户请求时，请遵循以下原则：
            1. 根据需求选择合适的工具：
               - 使用web_search工具搜索相关信息
               - 使用web_browser工具访问和分析具体网页
            2. 工具使用策略：
               - 对于笼统的问题，先使用搜索工具找到相关网页
               - 对于具体的URL，直接使用浏览工具获取内容
               - 需要多个来源时，使用多个工具组合
            3. 内容分析原则：
               - 提取关键信息，去除冗余内容
               - 对比多个来源，确保信息准确性
               - 以结构化方式组织信息
               - 标注信息来源
            4. 错误处理：
               - 如果工具执行失败，说明原因并尝试其他方案
               - 如果内容不完整，说明限制并提供可用信息
            5. 回复格式：
               - 使用中文回答所有问题
               - 清晰的层次结构
               - 重要信息加粗显示
               - 适当使用列表和表格
            """;

        // 创建代理配置
        Map<String, Object> memory = new HashMap<>();

        // 创建同步代理
        SyncAgent agent = new SyncAgent(
                model,
                template,
                memory,
                new JsonParser(),
                new DefaultAggregator(),
                executor,
                "AI助手",
                "专业的信息分析助手"
        );

        // 演示多个场景
        demonstrateScenarios(agent);
    }

    /**
     * 演示多个工具使用场景
     *
     * @param agent 代理
     */
    private static void demonstrateScenarios(SyncAgent agent) {
        // 场景1：直接访问URL
        System.out.println("=== 场景1：直接访问URL ===");
        AgentMessage directUrl = new AgentMessage(
                "user",
                "请访问 https://www.bilibili.com/read/cv27566475 并总结这篇文章的主要内容。"
        );
        processAndPrintResponse(agent, directUrl);

        // 场景2：搜索相关信息
        System.out.println("\n=== 场景2：搜索相关信息 ===");
        AgentMessage searchQuery = new AgentMessage(
                "user",
                "请搜索Java 21的主要新特性，并从多个来源总结信息。"
        );
        processAndPrintResponse(agent, searchQuery);

        // 场景3：多工具组合
        System.out.println("\n=== 场景3：多工具组合 ===");
        AgentMessage combined = new AgentMessage(
                "user",
                "请搜索2024年人工智能的最新发展趋势，并选择最相关的3个网页深入分析。"
        );
        processAndPrintResponse(agent, combined);

        // 场景4：错误处理
        System.out.println("\n=== 场景4：错误处理 ===");
        AgentMessage error = new AgentMessage(
                "user",
                "请访问 https://不存在的网页.com 并分析内容。"
        );
        processAndPrintResponse(agent, error);

        // 场景5：并行处理
        System.out.println("\n=== 场景5：并行处理 ===");
        List<AgentMessage> parallelTasks = new ArrayList<>();
        parallelTasks.add(new AgentMessage("user", "搜索Python 3.12的新特性"));
        parallelTasks.add(new AgentMessage("user", "搜索TypeScript 5.0的更新"));
        parallelTasks.add(new AgentMessage("user", "搜索Kotlin 2.0的计划"));

        for (AgentMessage task : parallelTasks) {
            processAndPrintResponse(agent, task);
        }
    }

    /**
     * 处理消息并打印响应
     *
     * @param agent    代理
     * @param message  消息
     */
    private static void processAndPrintResponse(SyncAgent agent, AgentMessage message) {
        System.out.println("\n用户: " + message.getContent());
        System.out.println("\nAI助手正在处理...\n");

        try {
            AgentMessage response = agent.process(message, 1);
            System.out.println("AI助手: " + response.getContent());

            // 显示工具执行结果
            if (response.getFormatted() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> formatted = (Map<String, Object>) response.getFormatted();
                
                // 显示思考过程
                if (formatted.containsKey("thought")) {
                    System.out.println("\n思考过程:\n" + formatted.get("thought"));
                }

                // 显示工具执行结果
                if (formatted.containsKey("result")) {
                    System.out.println("\n工具执行结果:");
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> results = (List<Map<String, String>>) formatted.get("result");
                    for (Map<String, String> result : results) {
                        System.out.println("- 类型: " + result.get("type"));
                        System.out.println("  内容: " + result.get("content"));
                        if (result.containsKey("url")) {
                            System.out.println("  来源: " + result.get("url"));
                        }
                        System.out.println();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("处理消息时发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("-------------------");
    }
} 