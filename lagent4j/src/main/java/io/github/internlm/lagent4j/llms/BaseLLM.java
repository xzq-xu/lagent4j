package io.github.internlm.lagent4j.llms;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 大型语言模型基础接口
 * <p>
 * 定义了与大型语言模型交互的基本方法，包括同步和异步的聊天功能
 */
public interface BaseLLM {
    /**
     * 同步聊天方法
     *
     * @param messages 消息列表，通常包含角色和内容
     * @return 模型的回复文本
     */
    String chat(List<Map<String, String>> messages);
    
    /**
     * 同步聊天方法，带有额外参数
     *
     * @param messages 消息列表，通常包含角色和内容
     * @param params   额外的参数，如温度、最大生成长度等
     * @return 模型的回复文本
     */
    String chat(List<Map<String, String>> messages, Map<String, Object> params);
    
    /**
     * 异步聊天方法
     *
     * @param messages 消息列表，通常包含角色和内容
     * @return 包含模型回复文本的CompletableFuture
     */
    CompletableFuture<String> chatAsync(List<Map<String, String>> messages);
    
    /**
     * 异步聊天方法，带有额外参数
     *
     * @param messages 消息列表，通常包含角色和内容
     * @param params   额外的参数，如温度、最大生成长度等
     * @return 包含模型回复文本的CompletableFuture
     */
    CompletableFuture<String> chatAsync(List<Map<String, String>> messages, Map<String, Object> params);
    
    /**
     * 流式聊天方法，通过回调函数处理流式输出
     *
     * @param messages       消息列表，通常包含角色和内容
     * @param chunkConsumer  处理每个文本块的消费者函数
     * @param errorConsumer  处理错误的消费者函数
     * @param doneConsumer   处理完成事件的消费者函数
     */
    void chatStream(List<Map<String, String>> messages, 
                   Consumer<String> chunkConsumer, 
                   Consumer<Throwable> errorConsumer, 
                   Runnable doneConsumer);
    
    /**
     * 流式聊天方法，带有额外参数，通过回调函数处理流式输出
     *
     * @param messages       消息列表，通常包含角色和内容
     * @param params         额外的参数，如温度、最大生成长度等
     * @param chunkConsumer  处理每个文本块的消费者函数
     * @param errorConsumer  处理错误的消费者函数
     * @param doneConsumer   处理完成事件的消费者函数
     */
    void chatStream(List<Map<String, String>> messages, 
                   Map<String, Object> params,
                   Consumer<String> chunkConsumer, 
                   Consumer<Throwable> errorConsumer, 
                   Runnable doneConsumer);
} 