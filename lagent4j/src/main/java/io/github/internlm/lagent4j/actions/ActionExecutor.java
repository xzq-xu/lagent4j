package io.github.internlm.lagent4j.actions;

import io.github.internlm.lagent4j.schema.ActionReturn;
import io.github.internlm.lagent4j.schema.ActionStatusCode;
import io.github.internlm.lagent4j.schema.AgentMessage;
import io.github.internlm.lagent4j.schema.FunctionCall;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 动作执行器
 * <p>
 * 用于管理和执行工具，支持以下功能：
 * 1. 工具的注册、注销和管理
 * 2. 同步和异步执行工具
 * 3. 并行执行多个工具
 * 4. 工具执行状态监控
 * 5. 错误处理和重试机制
 * 6. 工具执行超时控制
 */
@Slf4j
public class ActionExecutor {
    /**
     * 动作映射表
     */
    private final Map<String, Action> actions;

    /**
     * 线程池执行器
     */
    private final ExecutorService executor;

    /**
     * 默认超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT = 30;

    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 默认重试延迟（毫秒）
     */
    private static final long DEFAULT_RETRY_DELAY = 1000;

    /**
     * 创建一个动作执行器
     */
    public ActionExecutor() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * 创建一个动作执行器
     *
     * @param threadPoolSize 线程池大小
     */
    public ActionExecutor(int threadPoolSize) {
        this.actions = new ConcurrentHashMap<>();
        this.executor = new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 注册动作
     *
     * @param action 动作
     */
    public void registerAction(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("动作不能为空");
        }

        String name = action.getName();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("动作名称不能为空");
        }

        actions.put(name, action);
        log.debug("注册动作: {}", name);
    }

    /**
     * 注销动作
     *
     * @param name 动作名称
     * @return 是否成功注销
     */
    public boolean unregisterAction(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        Action removed = actions.remove(name);
        if (removed != null) {
            log.debug("注销动作: {}", name);
            return true;
        }

        return false;
    }

    /**
     * 获取所有动作的描述
     *
     * @return 动作描述列表
     */
    public List<Map<String, Object>> getActionDescriptions() {
        List<Map<String, Object>> descriptions = new ArrayList<>();

        for (Action action : actions.values()) {
            Map<String, Object> description = new HashMap<>();
            description.put("name", action.getName());
            description.put("description", action.getDescription());
            description.put("parameters", action.getParameters());
            descriptions.add(description);
        }

        return descriptions;
    }

    /**
     * 处理代理消息
     *
     * @param message 代理消息
     * @return 处理结果
     */
    @SuppressWarnings("unchecked")
    public AgentMessage process(AgentMessage message) {
        return process(message, DEFAULT_TIMEOUT, DEFAULT_MAX_RETRIES);
    }

    /**
     * 处理代理消息
     *
     * @param message   代理消息
     * @param timeout   超时时间（秒）
     * @param maxRetries 最大重试次数
     * @return 处理结果
     */
    @SuppressWarnings("unchecked")
    public AgentMessage process(AgentMessage message, int timeout, int maxRetries) {
        if (message == null || message.getFormatted() == null) {
            return new AgentMessage("action_executor", "没有要执行的动作");
        }

        Map<String, Object> formatted = (Map<String, Object>) message.getFormatted();

        // 提取函数调用信息
        String actionName = null;
        Map<String, Object> parameters = null;

        if (formatted.containsKey("function_call")) {
            Map<String, Object> functionCall = (Map<String, Object>) formatted.get("function_call");
            actionName = (String) functionCall.get("name");
            parameters = (Map<String, Object>) functionCall.get("arguments");
        } else if (formatted.containsKey("action")) {
            actionName = (String) formatted.get("action");
            parameters = (Map<String, Object>) formatted.get("parameters");
        }

        if (actionName == null) {
            return new AgentMessage("action_executor", "未指定动作");
        }

        // 查找动作
        Action action = actions.get(actionName);
        if (action == null) {
            log.warn("未找到动作: {}", actionName);
            return new AgentMessage("action_executor", "未找到动作: " + actionName);
        }

        // 执行动作（带重试）
        FunctionCall functionCall = new FunctionCall(actionName, parameters);
        ActionReturn result = executeWithRetry(action, functionCall, timeout, maxRetries);

        // 构建响应消息
        String content = result.formatResult();
        if (content.isEmpty()) {
            content = "动作执行成功，但未返回结果。";
        }

        return new AgentMessage("action_executor", content);
    }

    /**
     * 异步处理代理消息
     *
     * @param message 代理消息
     * @return 处理结果的Future对象
     */
    public CompletableFuture<AgentMessage> processAsync(AgentMessage message) {
        return processAsync(message, DEFAULT_TIMEOUT, DEFAULT_MAX_RETRIES);
    }

    /**
     * 异步处理代理消息
     *
     * @param message    代理消息
     * @param timeout    超时时间（秒）
     * @param maxRetries 最大重试次数
     * @return 处理结果的Future对象
     */
    public CompletableFuture<AgentMessage> processAsync(AgentMessage message, int timeout, int maxRetries) {
        return CompletableFuture.supplyAsync(() -> process(message, timeout, maxRetries), executor);
    }

    /**
     * 并行处理多个代理消息
     *
     * @param messages 代理消息列表
     * @return 处理结果列表
     */
    public List<AgentMessage> processParallel(List<AgentMessage> messages) {
        return processParallel(messages, DEFAULT_TIMEOUT, DEFAULT_MAX_RETRIES);
    }

    /**
     * 并行处理多个代理消息
     *
     * @param messages   代理消息列表
     * @param timeout    超时时间（秒）
     * @param maxRetries 最大重试次数
     * @return 处理结果列表
     */
    public List<AgentMessage> processParallel(List<AgentMessage> messages, int timeout, int maxRetries) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<AgentMessage>> futures = messages.stream()
                .map(message -> processAsync(message, timeout, maxRetries))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 带重试的动作执行
     *
     * @param action     动作
     * @param functionCall 函数调用
     * @param timeout     超时时间（秒）
     * @param maxRetries  最大重试次数
     * @return 执行结果
     */
    private ActionReturn executeWithRetry(Action action, FunctionCall functionCall, int timeout, int maxRetries) {
        int retries = 0;
        Exception lastException = null;

        while (retries <= maxRetries) {
            try {
                Future<ActionReturn> future = executor.submit(() -> action.execute(functionCall));
                return future.get(timeout, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("动作执行超时: {}", action.getName());
                lastException = e;
            } catch (Exception e) {
                log.warn("动作执行失败: {}", action.getName(), e);
                lastException = e;
            }

            if (retries < maxRetries) {
                try {
                    Thread.sleep(DEFAULT_RETRY_DELAY * (retries + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            retries++;
        }

        // 所有重试都失败
        return ActionReturn.builder()
                .type(action.getName())
                .state(ActionStatusCode.API_ERROR)
                .errmsg("动作执行失败（重试" + maxRetries + "次后）: " +
                        (lastException != null ? lastException.getMessage() : "未知错误"))
                .build();
    }

    /**
     * 获取动作数量
     *
     * @return 动作数量
     */
    public int getActionCount() {
        return actions.size();
    }

    /**
     * 清空所有动作
     */
    public void clearActions() {
        actions.clear();
        log.debug("清空所有动作");
    }

    /**
     * 获取工具描述信息
     *
     * @return 工具描述信息
     */
    public String getToolsDescription() {
        StringBuilder description = new StringBuilder();
//        description.append("可用工具列表：\n\n");

        for (Action action : actions.values()) {
            description.append("工具名称: ").append(action.getName()).append("\n");
            description.append("描述: ").append(action.getDescription()).append("\n");
            description.append("参数说明:\n");

            Map<String, Object> params = action.getParameters();
            if (params != null && params.containsKey("properties")) {
                Map<String, Object> properties = (Map<String, Object>) params.get("properties");
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    Map<String, Object> paramInfo = (Map<String, Object>) entry.getValue();
                    description.append("  - ").append(entry.getKey())
                            .append(": ").append(paramInfo.get("description"))
                            .append("\n");
                }
            }

            description.append("\n");
        }

        return description.toString();
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 