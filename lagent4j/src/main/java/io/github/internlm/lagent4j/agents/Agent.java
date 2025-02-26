package io.github.internlm.lagent4j.agents;

import io.github.internlm.lagent4j.actions.ActionExecutor;
import io.github.internlm.lagent4j.agents.aggregator.DefaultAggregator;
import io.github.internlm.lagent4j.llms.BaseLLM;
import io.github.internlm.lagent4j.memory.MemoryManager;
import io.github.internlm.lagent4j.prompts.Parser;
import io.github.internlm.lagent4j.prompts.parsers.StrParser;
import io.github.internlm.lagent4j.prompts.parsers.ToolParser;
import io.github.internlm.lagent4j.schema.AgentMessage;
import io.github.internlm.lagent4j.schema.ModelStatusCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 代理基类
 * <p>
 * 代理是框架的基本单位，负责与LLM通信、管理内存和处理消息。
 * 这是一个抽象基类，提供了基本的代理功能，子类可以实现不同的消息处理方式。
 */
public abstract class Agent {
    /**
     * 语言模型
     */
    @Getter
    protected final BaseLLM llm;
    
    /**
     * 提示模板
     */
    @Getter
    @Setter
    protected Object template;
    
    /**
     * 内存管理器
     */
    @Getter
    protected final MemoryManager memory;
    
    /**
     * 输出格式解析器
     */
    @Getter
    @Setter
    protected Parser outputFormat;

    /**
     * 用于工具输出格式化解析器
     */
    @Getter
    @Setter
    protected Parser toolOutputFormat;


    /**
     * 消息聚合器
     */
    @Getter
    protected final DefaultAggregator aggregator;
    
    /**
     * 代理名称
     */
    @Getter
    @Setter
    protected String name;
    
    /**
     * 代理描述
     */
    @Getter
    @Setter
    protected String description;

    /**
     * 工具执行器
     */
    @Getter
    protected final ActionExecutor actionExecutor;
    
    /**
     * 工具使用的提示模板
     */
    protected static final String TOOL_DECISION_TEMPLATE = """
        你是一个可以使用工具的AI助手。请仔细分析用户的问题，判断是否需要使用工具来回答。
        
        请严格按照以下格式回答：
        
        思考：[分析问题的关键点]
        
        决策：[使用/不使用] 工具
        
        如果决定使用工具，请说明需要使用的工具名称和参数。
        
        可用工具列表：
        %s
        
        用户问题：%s
        """;
    
    /**
     * 工具调用的提示模板
     */
    protected static final String TOOL_CALL_TEMPLATE = """
        你是一个可以使用工具的AI助手。请根据之前的决策，生成标准的工具调用格式。
        
        请严格按照以下JSON格式：
        <tool>
        {
            "name": "工具名称",
            "parameters": {
                "参数1": "值1",
                "参数2": "值2"
            }
        }
        </tool>
        
        可用工具列表：
        %s
        
        用户问题：%s
        之前的决策：%s
        """;
    
    /**
     * 结果总结的提示模板
     */
    protected static final String RESULT_SUMMARY_TEMPLATE = """
        你是一个可以使用工具的AI助手。请根据工具执行的结果，结合原始问题给出最终答案。
        
        请严格按照以下格式：
        
        分析：[分析工具返回的结果]
        
        总结：[给出完整的答案]
        
        原始问题：%s
        工具执行结果：%s
        """;

    /**
     * 创建一个代理
     *
     * @param llm          语言模型
     * @param template     提示模板
     * @param memory       内存配置
     * @param outputFormat 输出格式解析器
     * @param aggregator   消息聚合器
     * @param actionExecutor 工具执行器
     * @param name         代理名称
     * @param description  代理描述
     */
    public Agent(BaseLLM llm, 
                Object template, 
                Map<String, Object> memory, 
                Parser outputFormat,
                DefaultAggregator aggregator,
                ActionExecutor actionExecutor,
                String name, 
                String description) {
        this.llm = llm;
        this.template = template;
        this.memory = memory != null ? new MemoryManager(memory) : new MemoryManager();
        this.outputFormat = outputFormat;
        this.aggregator = aggregator != null ? aggregator : new DefaultAggregator();
        this.actionExecutor = actionExecutor;
        this.name = name != null ? name : this.getClass().getSimpleName();
        this.description = description;
        
        // 如果有工具执行器，更新模板中的工具信息
        if (actionExecutor != null) {
            updateTemplateWithToolInfo();
        }
    }
    
    /**
     * 创建一个简化的代理
     *
     * @param llm      语言模型
     * @param template 提示模板
     */
    public Agent(BaseLLM llm, Object template) {
        this(llm, template, null, null, null, null, null, null);
    }

    /**
     * 处理工具调用
     *
     * @param toolCalls 工具调用列表
     * @return 工具执行结果
     */
    protected String processToolCalls(List<Map<String, Object>> toolCalls) {
        if (actionExecutor == null || toolCalls == null || toolCalls.isEmpty()) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (Map<String, Object> toolCall : toolCalls) {
            String toolName = (String) toolCall.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) toolCall.get("parameters");
            
            AgentMessage toolMessage = new AgentMessage("user", "");
            toolMessage.setFormatted(Map.of(
                "function_call", Map.of(
                    "name", toolName,
                    "arguments", parameters
                )
            ));
            
            try {
                AgentMessage response = actionExecutor.process(toolMessage);
                result.append("工具 ").append(toolName).append(" 执行结果:\n")
                      .append(response.getContent()).append("\n\n");
            } catch (Exception e) {
                result.append("工具 ").append(toolName).append(" 执行失败: ")
                      .append(e.getMessage()).append("\n\n");
            }
        }
        return result.toString();
    }

    /**
     * 更新模板中的工具信息
     */
    protected void updateTemplateWithToolInfo() {
        if (actionExecutor == null) {
            return;
        }

        String toolInfo = actionExecutor.getToolsDescription();
        String promptTemplate = """
            你是一个可以使用工具的AI助手。在回答问题时，请严格按照以下格式：

            思考：[分析问题，判断是否需要使用工具]

            决策：[使用/不使用] 工具

            如果决定使用工具，请按如下格式调用：
            <tool>
            {
                "name": "工具名称",
                "parameters": {
                    "参数1": "值1",
                    "参数2": "值2"
                }
            }
            </tool>

            最后总结：[根据工具执行结果给出最终答案]

            可用工具列表：
            %s
            """.formatted(toolInfo);

        if (template instanceof String) {
            template = promptTemplate;
        } else if (template instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> templateMap = (Map<String, Object>) template;
            String systemPrompt = (String) templateMap.get("system");
            if (systemPrompt != null) {
                templateMap.put("system", promptTemplate);
            }
        }
    }

    /**
     * 更新内存
     *
     * @param message    消息
     * @param sessionId 会话ID
     */
    protected void updateMemory(AgentMessage message, int sessionId) {
        if (memory != null) {
            memory.add(message, sessionId);
        }
    }

    /**
     * 处理可能需要使用工具的消息
     *
     * @param message   用户消息
     * @param sessionId 会话ID
     * @return 处理后的响应
     */
    protected AgentMessage processWithTools(AgentMessage message, int sessionId) {
        // 第一步：判断是否需要使用工具
        String toolDecisionPrompt = String.format(TOOL_DECISION_TEMPLATE, 
            actionExecutor.getToolsDescription(),
            message.getContent());
        
        List<Map<String, String>> decisionMessages = List.of(
            Map.of("role", "system", "content", toolDecisionPrompt)
        );
        String decisionResponse = llm.chat(decisionMessages);
        
        // 如果决定不使用工具，直接返回响应
        if (!decisionResponse.contains("决策：使用工具")) {
            return new AgentMessage("assistant", decisionResponse);
        }
        
        // 第二步：生成工具调用格式
        String toolCallPrompt = String.format(TOOL_CALL_TEMPLATE,
            actionExecutor.getToolsDescription(),
            message.getContent(),
            decisionResponse);
        
        List<Map<String, String>> toolCallMessages = List.of(
            Map.of("role", "system", "content", toolCallPrompt)
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
            return new AgentMessage("system", "工具执行失败，无法获取结果");
        }
        
        // 第四步：根据工具结果生成最终答案
        String summaryPrompt = String.format(RESULT_SUMMARY_TEMPLATE,
            message.getContent(),
            toolResult);
        
        List<Map<String, String>> summaryMessages = List.of(
            Map.of("role", "system", "content", summaryPrompt)
        );
        String summaryResponse = llm.chat(summaryMessages);
        
        return new AgentMessage("assistant", summaryResponse);
    }


    protected  Parser getToolParser() {
        if (toolOutputFormat == null){
            return (toolOutputFormat=new ToolParser());
        }
        return toolOutputFormat;
    }


    /**
     * 同步处理消息
     *
     * @param message   消息
     * @param sessionId 会话ID
     * @return 响应消息
     */
    public abstract AgentMessage process(AgentMessage message, int sessionId);

    /**
     * 异步处理消息
     *
     * @param message   消息
     * @param sessionId 会话ID
     * @return 响应消息的Future
     */
    public abstract CompletableFuture<AgentMessage> processAsync(AgentMessage message, int sessionId);

    /**
     * 流式处理消息
     *
     * @param message   消息
     * @param sessionId 会话ID
     * @param callback  处理每个响应块的回调函数
     */
    public abstract void processStream(AgentMessage message, int sessionId, StreamCallback callback);

    /**
     * 流式处理回调接口
     */
    public interface StreamCallback {
        /**
         * 处理响应块
         *
         * @param chunk 响应块
         * @param state 模型状态
         */
        void onChunk(String chunk, ModelStatusCode state);

        /**
         * 处理错误
         *
         * @param error 错误信息
         */
        void onError(Throwable error);

        /**
         * 处理完成
         */
        void onComplete();
    }
} 