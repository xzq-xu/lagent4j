package io.github.internlm.lagent4j.agents;

import io.github.internlm.lagent4j.actions.ActionExecutor;
import io.github.internlm.lagent4j.agents.aggregator.DefaultAggregator;
import io.github.internlm.lagent4j.llms.BaseLLM;
import io.github.internlm.lagent4j.prompts.Parser;
import io.github.internlm.lagent4j.schema.AgentMessage;
import io.github.internlm.lagent4j.schema.ModelStatusCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 异步流式代理实现
 * <p>
 * 提供异步和流式消息处理功能的代理实现
 */
@Slf4j
public class AsyncStreamAgent extends Agent {
    
    /**
     * 创建一个异步流式代理
     *
     * @param llm           语言模型
     * @param template      提示模板
     * @param memory        内存配置
     * @param outputFormat  输出格式解析器
     * @param aggregator    消息聚合器
     * @param actionExecutor 工具执行器
     * @param name          代理名称
     * @param description   代理描述
     */
    public AsyncStreamAgent(BaseLLM llm,
                          Object template,
                          Map<String, Object> memory,
                          Parser outputFormat,
                          DefaultAggregator aggregator,
                          ActionExecutor actionExecutor,
                          String name,
                          String description) {
        super(llm, template, memory, outputFormat, aggregator, actionExecutor, name, description);
    }

    /**
     * 创建一个简化的异步流式代理
     *
     * @param llm      语言模型
     * @param template 提示模板
     */
    public AsyncStreamAgent(BaseLLM llm, Object template) {
        super(llm, template);
    }

    @Override
    public AgentMessage process(AgentMessage message, int sessionId) {
        return processAsync(message, sessionId).join();
    }

    @Override
    public CompletableFuture<AgentMessage> processAsync(AgentMessage message, int sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            // 处理输入消息
            List<AgentMessage> messages = new ArrayList<>();
            messages.add(message);
            
            // 更新内存
            updateMemory(message, sessionId);
            
            // 处理消息并生成响应
            AgentMessage responseMessage = forwardAsync(messages, sessionId).join();
            
            // 更新内存
            updateMemory(responseMessage, sessionId);
            
            return responseMessage;
        });
    }

    @Override
    public void processStream(AgentMessage message, int sessionId, StreamCallback callback) {
        // 处理输入消息
        List<AgentMessage> messages = new ArrayList<>();
        messages.add(message);
        
        // 更新内存
        updateMemory(message, sessionId);
        
        // 如果没有工具执行器，直接流式调用语言模型
        if (actionExecutor == null) {
            streamToLLM(messages, sessionId, callback);
            return;
        }

        // 第一步：判断是否需要使用工具
        List<Map<String, String>> decisionMessages = aggregator.aggregate(
            memory.get(sessionId), name, outputFormat, TOOL_DECISION_TEMPLATE.formatted(
                actionExecutor.getToolsDescription(),
                message.getContent()
            )
        );
        
        // 用于累积决策内容和状态
        StringBuilder decisionBuilder = new StringBuilder();
        class DecisionState {
            boolean made = false;
        }
        DecisionState state = new DecisionState();

        llm.chatStream(decisionMessages,
            decisionChunk -> {
                log.debug("思考中");
                callback.onChunk(decisionChunk, ModelStatusCode.GENERATING);
                
                // 累积决策内容
                decisionBuilder.append(decisionChunk);
                String fullDecision = decisionBuilder.toString();
                
                // 如果已经做出决策，不再继续判断
                if (state.made) {
                    return;
                }
                
                // 判断是否已经收到完整的决策
                if (fullDecision.contains("决策：不使用工具")) {
                    state.made = true;
                    log.debug("决策：不使用工具");
                    log.debug("最终答案：\n{}", fullDecision);
                    callback.onChunk(fullDecision, ModelStatusCode.END);
                    return;
                }
                
                // 判断是否需要使用工具
                if (fullDecision.contains("决策：使用工具")) {
                    state.made = true;
                    List<Map<String, String>> toolCallMessages = aggregator.aggregate(
                        memory.get(sessionId), name, outputFormat, TOOL_CALL_TEMPLATE.formatted(
                            actionExecutor.getToolsDescription(),
                            message.getContent(),
                            fullDecision
                        )
                    );
                    
                    String toolCallResponse = llm.chat(toolCallMessages);
                    log.debug("准备调用工具");
                    log.debug("工具调用格式：\n{}", toolCallResponse);
                    
                    // 第三步：解析并执行工具调用
                    String toolResult = null;
                    if (getToolParser() != null) {
                        Object parsed = getToolParser().parseResponse(toolCallResponse);
                        if (parsed instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> toolCall = (Map<String, Object>) parsed;
                            List<Map<String, Object>> toolCalls = List.of(toolCall);
                            toolResult = processToolCalls(toolCalls);
                            log.debug("工具执行结果：\n{}", toolResult);
                        }
                    }
                    
                    // 如果工具执行失败，返回错误信息
                    if (toolResult == null || toolResult.isEmpty()) {
                        log.error("工具执行失败，无法获取结果");
                        callback.onChunk("工具执行失败，无法获取结果", ModelStatusCode.END);
                        return;
                    }
                    
                    // 第四步：根据工具结果生成最终答案
                    List<Map<String, String>> summaryMessages = aggregator.aggregate(
                        memory.get(sessionId), name, outputFormat, RESULT_SUMMARY_TEMPLATE.formatted(
                            message.getContent(),
                            toolResult
                        )
                    );
                    
                    log.debug("生成最终答案");
                    llm.chatStream(summaryMessages,
                        summaryChunk -> callback.onChunk(summaryChunk, ModelStatusCode.GENERATING),
                        callback::onError,
                        () -> callback.onChunk("", ModelStatusCode.END)
                    );
                }
            },
            callback::onError,
            callback::onComplete
        );
    }

    /**
     * 异步前向处理
     *
     * @param messages  消息列表
     * @param sessionId 会话ID
     * @return 响应消息的Future
     */
    protected CompletableFuture<AgentMessage> forwardAsync(List<AgentMessage> messages, int sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            // 如果没有工具执行器，直接调用语言模型
            if (actionExecutor == null) {
                return forwardToLLM(messages, sessionId);
            }

            // 第一步：判断是否需要使用工具
            List<Map<String, String>> decisionMessages = aggregator.aggregate(
                memory.get(sessionId), name, outputFormat, TOOL_DECISION_TEMPLATE.formatted(
                    actionExecutor.getToolsDescription(),
                    messages.get(messages.size() - 1).getContent()
                )
            );
            String decisionResponse = llm.chat(decisionMessages);
            
            // 如果决定不使用工具，直接返回响应
            if (!decisionResponse.contains("决策：使用工具")) {
                return new AgentMessage(name, decisionResponse);
            }
            
            // 第二步：生成工具调用格式
            List<Map<String, String>> toolCallMessages = aggregator.aggregate(
                memory.get(sessionId), name, outputFormat, TOOL_CALL_TEMPLATE.formatted(
                    actionExecutor.getToolsDescription(),
                    messages.get(messages.size() - 1).getContent(),
                    decisionResponse
                )
            );
            String toolCallResponse = llm.chat(toolCallMessages);
            
            // 第三步：解析并执行工具调用
            String toolResult = null;
            if (outputFormat != null) {
                Object parsed = outputFormat.parseResponse(toolCallResponse);
                if (parsed instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> toolCall = (Map<String, Object>) parsed;
                    List<Map<String, Object>> toolCalls = List.of(toolCall);
                    toolResult = processToolCalls(toolCalls);
                }
            }
            
            // 如果工具执行失败，返回错误信息
            if (toolResult == null || toolResult.isEmpty()) {
                return new AgentMessage(name, "工具执行失败，无法获取结果");
            }
            
            // 第四步：根据工具结果生成最终答案
            List<Map<String, String>> summaryMessages = aggregator.aggregate(
                memory.get(sessionId), name, outputFormat, RESULT_SUMMARY_TEMPLATE.formatted(
                    messages.get(messages.size() - 1).getContent(),
                    toolResult
                )
            );
            String summaryResponse = llm.chat(summaryMessages);
            
            return new AgentMessage(name, summaryResponse);
        });
    }

    /**
     * 直接流式调用语言模型
     *
     * @param messages  消息列表
     * @param sessionId 会话ID
     * @param callback  回调函数
     */
    private void streamToLLM(List<AgentMessage> messages, int sessionId, StreamCallback callback) {
        List<Map<String, String>> formattedMessages = aggregator.aggregate(
            memory.get(sessionId), name, outputFormat, template);
        
        llm.chatStream(formattedMessages,
            chunk -> callback.onChunk(chunk, ModelStatusCode.GENERATING),
            callback::onError,
            () -> callback.onChunk("", ModelStatusCode.END)
        );
    }

    /**
     * 直接调用语言模型
     *
     * @param messages  消息列表
     * @param sessionId 会话ID
     * @return 响应消息
     */
    private AgentMessage forwardToLLM(List<AgentMessage> messages, int sessionId) {
        List<Map<String, String>> formattedMessages = aggregator.aggregate(
            memory.get(sessionId), name, outputFormat, template);
        String llmResponse = llm.chat(formattedMessages);
        return new AgentMessage(name, llmResponse);
    }
} 