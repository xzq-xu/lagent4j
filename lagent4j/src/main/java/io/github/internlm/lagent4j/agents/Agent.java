package io.github.internlm.lagent4j.agents;

import io.github.internlm.lagent4j.agents.aggregator.DefaultAggregator;
import io.github.internlm.lagent4j.llms.BaseLLM;
import io.github.internlm.lagent4j.memory.MemoryManager;
import io.github.internlm.lagent4j.prompts.StrParser;
import io.github.internlm.lagent4j.schema.AgentMessage;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代理类
 * <p>
 * 代理是框架的基本单位，负责与LLM通信、管理内存和处理消息
 */
public class Agent {
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
    protected StrParser outputFormat;
    
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
     * 创建一个代理
     *
     * @param llm          语言模型
     * @param template     提示模板
     * @param memory       内存配置
     * @param outputFormat 输出格式解析器
     * @param aggregator   消息聚合器
     * @param name         代理名称
     * @param description  代理描述
     */
    public Agent(BaseLLM llm, 
                Object template, 
                Map<String, Object> memory, 
                StrParser outputFormat, 
                DefaultAggregator aggregator, 
                String name, 
                String description) {
        this.llm = llm;
        this.template = template;
        this.memory = memory != null ? new MemoryManager(memory) : new MemoryManager();
        this.outputFormat = outputFormat;
        this.aggregator = aggregator != null ? aggregator : new DefaultAggregator();
        this.name = name != null ? name : this.getClass().getSimpleName();
        this.description = description;
    }
    
    /**
     * 创建一个简化的代理
     *
     * @param llm      语言模型
     * @param template 提示模板
     */
    public Agent(BaseLLM llm, Object template) {
        this(llm, template, null, null, null, null, null);
    }
    
    /**
     * 更新内存
     *
     * @param message   消息
     * @param sessionId 会话ID
     */
    public void updateMemory(AgentMessage message, int sessionId) {
        if (memory != null) {
            memory.add(message, sessionId);
        }
    }
    
    /**
     * 更新内存
     *
     * @param messages  消息列表
     * @param sessionId 会话ID
     */
    public void updateMemory(List<AgentMessage> messages, int sessionId) {
        if (memory != null) {
            memory.add(messages, sessionId);
        }
    }
    
    /**
     * 处理消息
     *
     * @param message   输入消息
     * @param sessionId 会话ID
     * @return 响应消息
     */
    public AgentMessage process(AgentMessage message, int sessionId) {
        // 处理输入消息
        List<AgentMessage> messages = new ArrayList<>();
        if (message instanceof AgentMessage) {
            messages.add(message);
        } else {
            messages.add(new AgentMessage("user", message));
        }
        
        // 更新内存
        updateMemory(messages, sessionId);
        
        // 处理消息并生成响应
        AgentMessage responseMessage = forward(messages, sessionId);
        
        // 如果响应不是AgentMessage类型，则包装为AgentMessage
        if (!(responseMessage instanceof AgentMessage)) {
            responseMessage = new AgentMessage(name, responseMessage);
        }
        
        // 更新内存
        updateMemory(responseMessage, sessionId);
        
        return responseMessage;
    }
    
    /**
     * 处理消息
     *
     * @param message 输入消息
     * @return 响应消息
     */
    public AgentMessage process(AgentMessage message) {
        return process(message, 0);
    }
    
    /**
     * 处理消息
     *
     * @param content 输入内容
     * @return 响应消息
     */
    public AgentMessage process(String content) {
        return process(new AgentMessage("user", content), 0);
    }
    
    /**
     * 处理消息
     *
     * @param content   输入内容
     * @param sessionId 会话ID
     * @return 响应消息
     */
    public AgentMessage process(String content, int sessionId) {
        return process(new AgentMessage("user", content), sessionId);
    }
    
    /**
     * 前向处理
     *
     * @param messages  消息列表
     * @param sessionId 会话ID
     * @return 响应消息
     */
    protected AgentMessage forward(List<AgentMessage> messages, int sessionId) {
        // 聚合消息
        List<Map<String, String>> formattedMessages = aggregator.aggregate(
                memory.get(sessionId), name, outputFormat, template);
        
        // 调用语言模型
        String llmResponse = llm.chat(formattedMessages);
        
        // 如果有输出格式解析器，则解析响应
        if (outputFormat != null) {
            Object formattedResponse = outputFormat.parseResponse(llmResponse);
            return new AgentMessage(name, llmResponse, formattedResponse);
        }
        
        return new AgentMessage(name, llmResponse);
    }
    
    /**
     * 重置代理的内存
     *
     * @param sessionId 会话ID
     */
    public void reset(int sessionId) {
        if (memory != null) {
            memory.clear(sessionId);
        }
    }
    
    /**
     * 重置代理的内存
     */
    public void reset() {
        reset(0);
    }
    
    /**
     * 获取代理的状态
     *
     * @return 状态字典
     */
    public Map<String, Object> stateDict() {
        Map<String, Object> state = new HashMap<>();
        
        // 添加内存状态
        if (memory != null) {
            Map<String, List<Map<String, Object>>> memoryState = new HashMap<>();
            for (Integer sessionId : memory.getSessions()) {
                List<Map<String, Object>> sessionMemory = new ArrayList<>();
                for (AgentMessage message : memory.get(sessionId).getMemory()) {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("content", message.getContent());
                    messageMap.put("sender", message.getSender());
                    messageMap.put("formatted", message.getFormatted());
                    messageMap.put("extraInfo", message.getExtraInfo());
                    messageMap.put("type", message.getType());
                    messageMap.put("receiver", message.getReceiver());
                    messageMap.put("streamState", message.getStreamState());
                    sessionMemory.add(messageMap);
                }
                memoryState.put(sessionId.toString(), sessionMemory);
            }
            state.put("memory", memoryState);
        }
        
        return state;
    }
} 