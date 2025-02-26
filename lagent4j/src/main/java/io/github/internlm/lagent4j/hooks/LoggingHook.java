package io.github.internlm.lagent4j.hooks;

import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.schema.AgentMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 日志钩子
 * <p>
 * 用于记录代理的处理过程
 */
@Slf4j
public class LoggingHook implements Hook {
    /**
     * 是否记录详细信息
     */
    private final boolean verbose;
    
    /**
     * 创建一个日志钩子
     *
     * @param verbose 是否记录详细信息
     */
    public LoggingHook(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * 创建一个日志钩子，默认不记录详细信息
     */
    public LoggingHook() {
        this(false);
    }
    
    @Override
    public List<AgentMessage> beforeAgent(Agent agent, List<AgentMessage> messages, int sessionId) {
        if (messages == null || messages.isEmpty()) {
            log.info("Agent {} (session {}) received empty messages", agent.getName(), sessionId);
            return null;
        }
        
        AgentMessage lastMessage = messages.get(messages.size() - 1);
        log.info("Agent {} (session {}) received message from {}: {}", 
                agent.getName(), sessionId, lastMessage.getSender(), 
                truncate(lastMessage.getContent().toString()));
        
        if (verbose) {
            log.debug("Full message: {}", lastMessage);
        }
        
        return null;
    }
    
    @Override
    public AgentMessage afterAgent(Agent agent, AgentMessage message, int sessionId) {
        if (message == null) {
            log.info("Agent {} (session {}) returned null response", agent.getName(), sessionId);
            return null;
        }
        
        log.info("Agent {} (session {}) responded: {}", 
                agent.getName(), sessionId, truncate(message.getContent().toString()));
        
        if (verbose) {
            log.debug("Full response: {}", message);
        }
        
        return null;
    }
    
    /**
     * 截断长文本
     *
     * @param text 原始文本
     * @return 截断后的文本
     */
    private String truncate(String text) {
        if (text == null) {
            return "null";
        }
        
        int maxLength = 100;
        if (text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength) + "...";
    }
} 