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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 异步流式代理示例
 * <p>
 * 展示如何创建一个支持异步和流式输出的对话代理，包括：
 * 1. 异步工具调用：并行执行多个工具
 * 2. 实时进度反馈：显示工具执行和生成状态
 * 3. 多任务处理：同时处理多个用户请求
 * 4. 优雅的错误处理：处理超时和异常情况
 * 5. 会话管理：支持多轮对话和上下文记忆
 */
public class AsyncStreamExample {
    public static void main(String[] args) {
        // 检查环境变量
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("请设置OPENAI_API_KEY环境变量");
            return;
        }

        // 创建OpenAI模型
        OpenAIModel model = new OpenAIModel.Builder()
                .apiKey(apiKey)
                .model("gpt-3.5-turbo")
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
            你是一个专业的AI助手，擅长异步处理多个任务和实时信息查询。
            
            在处理用户请求时，请遵循以下原则：
            1. 任务处理策略：
               - 根据任务复杂度选择同步或异步处理
               - 对于复杂任务，拆分为多个子任务并行处理
               - 实时反馈任务执行进度
            2. 工具使用原则：
               - 优先使用异步方式调用工具
               - 合理组合多个工具以提高效率
               - 实时报告工具执行状态
            3. 信息处理要求：
               - 确保信息的准确性和时效性
               - 多源信息交叉验证
               - 结构化组织信息
            4. 错误处理机制：
               - 优雅处理超时和异常
               - 提供备选方案
               - 保持响应的连续性
            5. 输出格式规范：
               - 使用清晰的层次结构
               - 突出重要信息
               - 适时使用表情符号
               - 保持专业性和友好性
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
                "专业的异步任务处理助手"
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
        // 场景1：单任务异步处理
        System.out.println("=== 场景1：单任务异步处理 ===");
        CompletableFuture<Void> task1 = processAsyncMessage(agent, new AgentMessage(
                "user",
                "请查询2024年春节假期的具体安排，并总结主要信息。"
        ));

        // 场景2：多任务并行处理
        System.out.println("\n=== 场景2：多任务并行处理 ===");
        List<AgentMessage> parallelTasks = Arrays.asList(
                new AgentMessage("user", "搜索2024年人工智能的主要发展方向"),
                new AgentMessage("user", "查询Web3.0的最新进展"),
                new AgentMessage("user", "了解量子计算的商业应用现状")
        );

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (AgentMessage task : parallelTasks) {
            futures.add(processAsyncMessage(agent, task));
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 场景3：多轮对话
        System.out.println("\n=== 场景3：多轮对话 ===");
        processConversation(agent, Arrays.asList(
                new AgentMessage("user", "什么是大语言模型？"),
                new AgentMessage("user", "它们目前面临哪些主要挑战？"),
                new AgentMessage("user", "你能举例说明一些实际应用场景吗？")
        ));

        // 场景4：错误处理
        System.out.println("\n=== 场景4：错误处理 ===");
        processAsyncMessage(agent, new AgentMessage(
                "user",
                "请分析一个不存在的网站内容：https://不存在的网站.com"
        )).join();

        // 场景5：长时间运行任务
        System.out.println("\n=== 场景5：长时间运行任务 ===");
        processAsyncMessageWithTimeout(agent, new AgentMessage(
                "user",
                "请对比分析过去5年中主要编程语言的发展趋势，包括使用率、社区活跃度和就业市场需求。"
        ), 3, TimeUnit.MINUTES).join();
    }

    /**
     * 异步处理消息
     *
     * @param agent   代理
     * @param message 消息
     * @return 异步任务
     */
    private static CompletableFuture<Void> processAsyncMessage(AsyncStreamAgent agent, AgentMessage message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());
        AtomicReference<String> currentPhase = new AtomicReference<>("准备中");
        AtomicInteger progressDots = new AtomicInteger(0);

        // 创建进度显示线程
        ScheduledExecutorService progressExecutor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> progressFuture = progressExecutor.scheduleAtFixedRate(() -> {
            if (!currentPhase.get().equals("完成")) {
                System.out.print("\r" + currentPhase.get() + " " + ".".repeat(progressDots.incrementAndGet() % 4));
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        System.out.println("\n用户: " + message.getContent());

        agent.processStream(message, 1, new AsyncStreamAgent.StreamCallback() {
            @Override
            public void onChunk(String chunk, ModelStatusCode status) {
                if (status == ModelStatusCode.GENERATING) {
                    // 更新处理阶段
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
                        System.out.println(); // 换行
                    }

                    System.out.print(chunk);
                    buffer.get().append(chunk);
                } else if (status == ModelStatusCode.END) {
                    currentPhase.set("完成");
                    System.out.println("\n\n✨ 任务完成！");
                }
            }

            @Override
            public void onError(Throwable error) {
                currentPhase.set("完成");
                System.err.println("\n❌ 发生错误：" + error.getMessage());
                error.printStackTrace();
                progressFuture.cancel(true);
                progressExecutor.shutdown();
                latch.countDown();
                future.completeExceptionally(error);
            }

            @Override
            public void onComplete() {
                currentPhase.set("完成");
                System.out.println("\n🔚 任务结束\n");
                System.out.println("-------------------");
                progressFuture.cancel(true);
                progressExecutor.shutdown();
                latch.countDown();
                future.complete(null);
            }
        });

        return future;
    }

    /**
     * 处理多轮对话
     *
     * @param agent        代理
     * @param conversation 对话消息列表
     */
    private static void processConversation(AsyncStreamAgent agent, List<AgentMessage> conversation) {
        for (AgentMessage message : conversation) {
            processAsyncMessage(agent, message).join();
        }
    }

    /**
     * 带超时的异步消息处理
     *
     * @param agent    代理
     * @param message  消息
     * @param timeout  超时时间
     * @param unit     时间单位
     * @return 异步任务
     */
    private static CompletableFuture<Void> processAsyncMessageWithTimeout(
            AsyncStreamAgent agent,
            AgentMessage message,
            long timeout,
            TimeUnit unit) {
        CompletableFuture<Void> future = processAsyncMessage(agent, message);
        return future.orTimeout(timeout, unit)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        System.err.println("\n⏰ 任务执行超时！");
                    } else {
                        System.err.println("\n❌ 任务执行失败：" + throwable.getMessage());
                    }
                    return null;
                });
    }
} 