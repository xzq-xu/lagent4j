package com.example.lagent4j.agent;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.util.Map;
import java.util.HashMap;
import com.alibaba.fastjson2.JSON;

@Data
@Builder
@AllArgsConstructor
public class AgentMessage {
    // 基础字段
    private String sender;
    private String content;
    private Object formatted; // 用于存储解析后的工具调用参数等结构化数据
    private Map<String, Object> extraInfo;
    private AgentMessageType type;
    private String receiver;
    private StreamState streamState;
    private long sessionId;
    
    // Legacy fields
    private String toolCallId;
    private String toolName;
    private String toolArgs;

    public enum AgentMessageType {
        SYSTEM,    // 系统消息
        USER,      // 用户消息
        ASSISTANT, // 助手消息
        TOOL      // 工具消息
    }
    
    public enum StreamState {
        START,   // 开始流式响应
        RUNNING, // 响应进行中
        END      // 响应结束
    }
    
    // 深拷贝方法
    public AgentMessage copy(boolean deep) {
        return AgentMessage.builder()
            .sender(this.sender)
            .content(this.content)
            .formatted(deep ? deepCopyFormatted() : this.formatted)
            .extraInfo(deep ? new HashMap<>(this.extraInfo) : this.extraInfo)
            .type(this.type)
            .receiver(this.receiver)
            .streamState(this.streamState)
            .sessionId(this.sessionId)
            .build();
    }
    
    private Object deepCopyFormatted() {
        if (formatted == null) return null;
        return JSON.parseObject(JSON.toJSONString(formatted));
    }
}
