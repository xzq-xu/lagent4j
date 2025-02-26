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

import java.util.HashMap;
import java.util.Map;

/**
 * WebBrowser工具使用示例
 * <p>
 * 展示如何使用WebBrowser工具获取网页内容并进行信息提取，
 * 同时演示新的多阶段工具使用流程。
 */
public class WebBrowserExample {
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
        
        // 注册网页浏览和搜索工具
        Action webBrowser = new WebBrowser();
        Action webSearch = new WebSearch();
        executor.registerAction(webBrowser);
        executor.registerAction(webSearch);

        // 创建系统提示
        String template = """
            你是一个专业的网页内容分析助手。你可以使用以下工具来帮助用户：
            1. web_browser - 用于访问和获取网页内容
            2. web_search - 用于搜索相关信息
            
            请根据用户的问题，判断是否需要使用工具，并按照规范的格式回答。
            
            示例问题：
            - "这个网页说了什么？" -> 使用 web_browser 工具
            - "查找相关资料" -> 使用 web_search 工具
            - "总结这篇文章" -> 使用 web_browser 工具后分析
            
            请用中文回答所有问题。""";

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
                "网页分析助手",
                "专业的网页内容分析助手"
        );

        // 演示多个不同场景的工具使用

        // 场景1：直接访问网页
        System.out.println("=== 场景1：直接访问网页 ===");
        AgentMessage question1 = new AgentMessage(
                "user",
                "请访问 https://www.bilibili.com/read/cv27566475 并总结这篇文章的主要内容。"
        );
        processAndPrintResponse(agent, question1);

        // 场景2：先搜索后访问
        System.out.println("\n=== 场景2：先搜索后访问 ===");
        AgentMessage question2 = new AgentMessage(
                "user",
                "请帮我找找关于Java 21新特性的文章并总结主要内容。"
        );
        processAndPrintResponse(agent, question2);

        // 场景3：不需要使用工具
        System.out.println("\n=== 场景3：不需要使用工具 ===");
        AgentMessage question3 = new AgentMessage(
                "user",
                "你是一个什么样的助手？"
        );
        processAndPrintResponse(agent, question3);
    }

    /**
     * 处理消息并打印响应
     *
     * @param agent    代理
     * @param message  用户消息
     */
    private static void processAndPrintResponse(SyncAgent agent, AgentMessage message) {
        System.out.println("用户: " + message.getContent() + "\n");
        System.out.println("助手思考中...\n");
        
        AgentMessage response = agent.process(message, 1);
        
        // 打印完整的交互过程
        System.out.println("助手: " + response.getContent());
        
        // 如果有工具执行结果，显示结果
        if (response.getFormatted() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.getFormatted();
            if (result.containsKey("result")) {
                System.out.println("\n工具执行结果:\n" + result.get("result"));
            }
        }
        
        System.out.println("\n-------------------\n");
    }
} 