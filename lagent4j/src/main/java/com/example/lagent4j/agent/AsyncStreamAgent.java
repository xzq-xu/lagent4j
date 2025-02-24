package com.example.lagent4j.agent;

import com.example.lagent4j.actions.Action;
import com.example.lagent4j.llm.AsyncLlmService;
import com.example.lagent4j.prompts.OutputFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 支持流式响应的异步智能体实现
 * 
 * <p>该实现提供了以下特性：
 * <ul>
 *   <li>实时流式输出 - 支持逐字符/逐词输出</li>
 *   <li>状态追踪 - 通过 StreamState 跟踪响应状态</li>
 *   <li>消息聚合 - 自动聚合流式响应片段</li>
 *   <li>格式化处理 - 支持对完整响应进行格式化</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>{@code
 * AsyncStreamAgent agent = new AsyncStreamAgent(
 *     llmService,
 *     memory,
 *     actions,
 *     new ToolParser("python", "```python\n", "\n```"),
 *     "You are a helpful assistant."
 * );
 * 
 * // 设置流式输出回调
 * agent.setOnMessage(msg -> {
 *     if (msg.getStreamState() == StreamState.RUNNING) {
 *         System.out.print(msg.getContent()); // 实时打印
 *     }
 * });
 * 
 * // 处理消息
 * agent.processAsync(userMessage, sessionId)
 *     .thenAccept(finalResponse -> {
 *         // 处理完整响应
 *     });
 * }</pre>
 */
public class AsyncStreamAgent implements AsyncAgent {
    private final AsyncLlmService llm;
    protected final Memory memory;
    private final List<Action> tools;
    private final OutputFormat outputFormat;
    private final String systemPrompt;
    
    private Consumer<AgentMessage> onMessage = msg -> {};
    private Consumer<Throwable> onError = ex -> {};

    /**
     * 创建流式智能体实例
     *
     * @param llmService LLM服务实例，必须支持流式输出
     * @param memory 记忆管理器
     * @param actions 可用工具列表
     * @param outputFormat 输出格式化器
     * @param systemPrompt 系统提示词
     */
    public AsyncStreamAgent(
            AsyncLlmService llm,
            Memory memory,
            List<Action> actions,
            OutputFormat outputFormat,
            String systemPrompt) {
        this.llm = llm;
        this.memory = memory;
        this.tools = actions;
        this.outputFormat = outputFormat;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public CompletableFuture<AgentMessage> processAsync(AgentMessage message, long sessionId) {
        // 获取历史消息
        List<AgentMessage> history = memory.get(sessionId);
        
        // 如果是新会话，添加系统提示
        if (history.isEmpty()) {
            AgentMessage systemMsg = AgentMessage.builder()
                .sender("system")
                .content(systemPrompt)
                .type(AgentMessage.AgentMessageType.SYSTEM)
                .sessionId(sessionId)
                .build();
            memory.add(sessionId, systemMsg);
            history = memory.get(sessionId);
        }

        // 添加用户消息到历史
        memory.add(sessionId, message);

        // 创建响应Future
        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder responseBuilder = new StringBuilder();

        // 调用LLM服务
        return llm.chatStreamAsync(
            history,
            content -> {
                // 累积响应内容
                responseBuilder.append(content);
                
                // 发送流式消息
                AgentMessage streamMsg = AgentMessage.builder()
                    .sender("assistant")
                    .content(content)
                    .type(AgentMessage.AgentMessageType.ASSISTANT)
                    .streamState(AgentMessage.StreamState.RUNNING)
                    .sessionId(sessionId)
                    .build();
                onMessage.accept(streamMsg);
            },
            error -> {
                onError.accept(error);
                future.completeExceptionally(error);
            }
        ).thenRun(() -> future.complete(responseBuilder.toString()))
        .thenApply(v -> {
            String fullResponse = responseBuilder.toString();
            
            // 解析工具调用
            Object formatted = outputFormat.parseResponse(fullResponse);
            
            // 构建完整响应消息
            AgentMessage response = AgentMessage.builder()
                .sender("assistant")
                .content(fullResponse)
                .formatted(formatted)
                .type(AgentMessage.AgentMessageType.ASSISTANT)
                .streamState(AgentMessage.StreamState.END)
                .sessionId(sessionId)
                .build();
                
            // 添加到历史
            memory.add(sessionId, response);
            return response;
        });
    }

    @Override
    public void setOnMessage(Consumer<AgentMessage> callback) {
        this.onMessage = callback != null ? callback : msg -> {};
    }

    @Override
    public void setOnError(Consumer<Throwable> callback) {
        this.onError = callback != null ? callback : ex -> {};
    }

    @Override
    public void reset(long sessionId) {
        memory.clear(sessionId);
    }

    @Override
    public List<Action> getTools() {
        return Collections.unmodifiableList(tools);
    }
} 