package io.github.internlm.lagent4j.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代理消息类
 * <p>
 * 用于表示代理之间传递的消息，包含内容、发送者、格式化内容、额外信息、类型、接收者和流状态等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {
    /**
     * 消息内容，可以是任意类型
     */
    private Object content;
    
    /**
     * 消息发送者
     */
    @Builder.Default
    private String sender = "user";
    
    /**
     * 格式化后的内容，通常由输出格式解析器处理
     */
    private Object formatted;
    
    /**
     * 额外信息
     */
    private Object extraInfo;
    
    /**
     * 消息类型
     */
    private String type;
    
    /**
     * 消息接收者
     */
    private String receiver;
    
    /**
     * 流状态
     */
    @Builder.Default
    private AgentStatusCode streamState = AgentStatusCode.END;
    
    /**
     * 创建一个只包含发送者和内容的消息
     *
     * @param sender  发送者
     * @param content 消息内容
     */
    public AgentMessage(String sender, Object content) {
        this.sender = sender;
        this.content = content;
        this.streamState = AgentStatusCode.END;
    }
    
    /**
     * 创建一个包含发送者、内容和格式化内容的消息
     *
     * @param sender    发送者
     * @param content   消息内容
     * @param formatted 格式化内容
     */
    public AgentMessage(String sender, Object content, Object formatted) {
        this.sender = sender;
        this.content = content;
        this.formatted = formatted;
        this.streamState = AgentStatusCode.END;
    }
    
    /**
     * 深拷贝当前消息
     *
     * @return 拷贝后的新消息对象
     */
    public AgentMessage copy() {
        return AgentMessage.builder()
                .content(content)
                .sender(sender)
                .formatted(formatted)
                .extraInfo(extraInfo)
                .type(type)
                .receiver(receiver)
                .streamState(streamState)
                .build();
    }
    
    @Override
    public String toString() {
        return "AgentMessage{" +
                "content=" + content +
                ", sender='" + sender + '\'' +
                ", streamState=" + streamState +
                '}';
    }
} 