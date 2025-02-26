package io.github.internlm.lagent4j.examples;

import io.github.internlm.lagent4j.actions.Action;
import io.github.internlm.lagent4j.actions.ActionExecutor;
import io.github.internlm.lagent4j.actions.WebBrowser;
import io.github.internlm.lagent4j.actions.WebSearch;
import io.github.internlm.lagent4j.agents.AsyncStreamAgent;
import io.github.internlm.lagent4j.agents.aggregator.DefaultAggregator;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.prompts.parsers.JsonParser;
import io.github.internlm.lagent4j.schema.AgentMessage;
import io.github.internlm.lagent4j.schema.ModelStatusCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式输出示例
 * <p>
 * 展示如何使用lagent4j框架创建一个支持流式输出的对话代理，包括：
 * 1. 实时流式输出：逐字显示生成内容
 * 2. 工具使用状态：显示工具调用和执行过程
 * 3. 多轮对话：支持上下文记忆
 * 4. 错误处理：优雅处理异常情况
 * 5. 进度显示：显示生成和工具执行进度
 */
public class StreamExample {
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
            你是一个专业的AI助手，擅长使用工具搜索和分析信息，并以流式方式输出结果。
            
            在处理用户请求时，请遵循以下原则：
            1. 工具使用：
               - 根据需要选择合适的工具
               - 清晰说明正在执行的操作
               - 实时反馈工具执行状态
            2. 信息处理：
               - 仔细分析获取的信息
               - 提取关键内容
               - 以结构化方式组织
            3. 输出格式：
               - 使用简单易懂的语言
               - 分步骤详细解释
               - 突出重要信息
               - 适当使用表情符号增加趣味性
            4. 错误处理：
               - 及时报告错误
               - 提供替代方案
               - 保持对话流畅
            5. 回复要求：
               - 用中文回答所有问题
               - 保持专业性和友好性
               - 适时使用表情符号
            """;

        // 创建代理配置
        Map<String, Object> memory = new HashMap<>();

        // 创建异步流式代理
        AsyncStreamAgent agent = new AsyncStreamAgent(
                model,
                template,
                memory,
                new JsonParser(),
                new DefaultAggregator(),
                executor,
                "AI助手",
                "专业的信息搜索和分析助手"
        );

        // 演示多个场景
        demonstrateScenarios(agent);
    }

    /**
     * 演示多个场景
     *
     * @param agent 代理
     */
    private static void demonstrateScenarios(AsyncStreamAgent agent) {
        // 场景1：简单问答
        System.out.println("=== 场景1：简单问答 ===");
        processStreamMessage(agent, new AgentMessage(
                "user",
                "请解释什么是流式输出，并举一些实际的例子。"
        ));

        // 场景2：使用工具搜索
        System.out.println("\n=== 场景2：使用工具搜索 ===");
        processStreamMessage(agent, new AgentMessage(
                "user",
                "请搜索并解释Go语言1.22版本的主要新特性。"
        ));

        // 场景3：多工具组合
        System.out.println("\n=== 场景3：多工具组合 ===");
        processStreamMessage(agent, new AgentMessage(
                "user",
                "请搜索2024年软件开发的主要趋势，并深入分析其中最重要的三个趋势。"
        ));

        // 场景4：错误处理
        System.out.println("\n=== 场景4：错误处理 ===");
        processStreamMessage(agent, new AgentMessage(
                "user",
                "请分析一个不存在的网页：https://example.com/不存在的页面.html"
        ));

        // 场景5：多轮对话
        System.out.println("\n=== 场景5：多轮对话 ===");
        List<AgentMessage> conversation = new ArrayList<>();
        conversation.add(new AgentMessage("user", "什么是微服务架构？"));
        conversation.add(new AgentMessage("user", "它与单体架构相比有什么优势和劣势？"));
        conversation.add(new AgentMessage("user", "你能举一个成功应用微服务架构的实际案例吗？"));

        for (AgentMessage message : conversation) {
            processStreamMessage(agent, message);
        }
    }

    /**
     * 处理流式消息
     *
     * @param agent   代理
     * @param message 消息
     */
    private static void processStreamMessage(AsyncStreamAgent agent, AgentMessage message) {
        System.out.println("\n用户: " + message.getContent());
        System.out.println("\nAI助手正在思考...\n");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());
        AtomicReference<String> currentPhase = new AtomicReference<>("思考中");

        agent.processStream(message, 1, new AsyncStreamAgent.StreamCallback() {
            @Override
            public void onChunk(String chunk, ModelStatusCode status) {
                if (status == ModelStatusCode.GENERATING) {
                    // 更新输出进度
                    String phase = currentPhase.get();
                    if (chunk.contains("正在搜索") || chunk.contains("搜索完成")) {
                        phase = "搜索中";
                    } else if (chunk.contains("正在分析") || chunk.contains("分析完成")) {
                        phase = "分析中";
                    } else if (chunk.contains("正在整理") || chunk.contains("整理完成")) {
                        phase = "整理中";
                    }
                    
                    if (!phase.equals(currentPhase.get())) {
                        currentPhase.set(phase);
                        System.out.print("\n[" + phase + "] ");
                    }

                    // 输出内容
                    System.out.print(chunk);
                    buffer.get().append(chunk);
                } else if (status == ModelStatusCode.END) {
                    System.out.println("\n\n✨ 回答完成！");
                }
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("\n❌ 发生错误：" + error.getMessage());
                error.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onComplete() {
                System.out.println("\n🔚 会话结束\n");
                System.out.println("-------------------");
                latch.countDown();
            }
        });

        try {
            // 等待处理完成，最多等待5分钟
            latch.await(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.err.println("等待响应时被中断：" + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
} 