package io.github.internlm.lagent4j.llms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 大型语言模型抽象基类
 * <p>
 * 提供BaseLLM接口的基本实现，简化子类的开发
 */
public abstract class AbstractLLM implements BaseLLM {
    /**
     * 默认参数
     */
    protected Map<String, Object> defaultParams;
    
    /**
     * 创建一个带有默认参数的LLM
     *
     * @param defaultParams 默认参数
     */
    public AbstractLLM(Map<String, Object> defaultParams) {
        this.defaultParams = defaultParams != null ? new HashMap<>(defaultParams) : new HashMap<>();
    }
    
    /**
     * 创建一个没有默认参数的LLM
     */
    public AbstractLLM() {
        this(null);
    }
    
    @Override
    public String chat(List<Map<String, String>> messages) {
        return chat(messages, new HashMap<>());
    }
    
    @Override
    public CompletableFuture<String> chatAsync(List<Map<String, String>> messages) {
        return chatAsync(messages, new HashMap<>());
    }
    
    @Override
    public CompletableFuture<String> chatAsync(List<Map<String, String>> messages, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> chat(messages, params));
    }
    
    @Override
    public  void chatStream(List<Map<String, String>> messages,
                          Consumer<String> chunkConsumer, 
                          Consumer<Throwable> errorConsumer, 
                          Runnable doneConsumer) {
        chatStream(messages, new HashMap<>(), chunkConsumer, errorConsumer, doneConsumer);
    }
    
    /**
     * 合并默认参数和用户提供的参数
     *
     * @param params 用户提供的参数
     * @return 合并后的参数
     */
    protected Map<String, Object> mergeParams(Map<String, Object> params) {
        Map<String, Object> mergedParams = new HashMap<>(defaultParams);
        if (params != null) {
            mergedParams.putAll(params);
        }
        return mergedParams;
    }
} 