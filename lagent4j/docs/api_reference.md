# Lagent4j API 参考文档

## 目录

- [核心接口](#核心接口)
- [智能体实现](#智能体实现)
- [工具系统](#工具系统)
- [记忆管理](#记忆管理)
- [消息系统](#消息系统)

## 核心接口

### BaseLLM

大型语言模型基础接口，定义了与语言模型交互的基本方法。

```java
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
```

### StrParser

字符串解析器接口，用于解析模型输出的字符串。

```java
public interface StrParser {
    /**
     * 解析响应
     *
     * @param response 模型响应文本
     * @return 解析后的对象
     */
    Object parseResponse(String response);
}
```

## 智能体实现

### Agent

代理类，负责与LLM通信、管理内存和处理消息。

```java
public class Agent {
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
                String description);
    
    /**
     * 创建一个简化的代理
     *
     * @param llm      语言模型
     * @param template 提示模板
     */
    public Agent(BaseLLM llm, Object template);
    
    /**
     * 处理消息
     *
     * @param message   输入消息
     * @param sessionId 会话ID
     * @return 响应消息
     */
    public AgentMessage process(AgentMessage message, int sessionId);
    
    /**
     * 处理消息
     *
     * @param message 输入消息
     * @return 响应消息
     */
    public AgentMessage process(AgentMessage message);
    
    /**
     * 处理消息
     *
     * @param content 输入内容
     * @return 响应消息
     */
    public AgentMessage process(String content);
    
    /**
     * 处理消息
     *
     * @param content   输入内容
     * @param sessionId 会话ID
     * @return 响应消息
     */
    public AgentMessage process(String content, int sessionId);
    
    /**
     * 重置代理的内存
     *
     * @param sessionId 会话ID
     */
    public void reset(int sessionId);
    
    /**
     * 重置代理的内存
     */
    public void reset();
    
    /**
     * 获取代理的状态
     *
     * @return 状态字典
     */
    public Map<String, Object> stateDict();
}
```

## 工具系统

### Action

工具接口，定义了工具的基本行为。

```java
public interface Action {
    /**
     * 获取动作名称
     *
     * @return 动作名称
     */
    String getName();
    
    /**
     * 获取动作描述
     *
     * @return 动作描述
     */
    String getDescription();
    
    /**
     * 获取动作参数描述
     *
     * @return 参数描述，通常是JSON Schema格式
     */
    Map<String, Object> getParameters();
    
    /**
     * 执行动作
     *
     * @param functionCall 函数调用信息
     * @return 动作执行结果
     */
    ActionReturn execute(FunctionCall functionCall);
}
```

### ActionExecutor

动作执行器，负责管理和执行工具。

```java
public class ActionExecutor {
    /**
     * 创建一个动作执行器
     */
    public ActionExecutor();
    
    /**
     * 注册动作
     *
     * @param action 要注册的动作
     */
    public void registerAction(Action action);
    
    /**
     * 注销动作
     *
     * @param name 动作名称
     * @return 是否成功注销
     */
    public boolean unregisterAction(String name);
    
    /**
     * 获取动作描述列表
     *
     * @return 动作描述列表
     */
    public List<Map<String, Object>> getActionDescriptions();
    
    /**
     * 处理消息
     *
     * @param message 输入消息
     * @return 处理后的消息
     */
    public AgentMessage process(AgentMessage message);
}
```

### WebBrowser

网页浏览器工具，用于获取网页内容。

```java
public class WebBrowser implements Action {
    /**
     * 创建一个网页浏览器工具
     */
    public WebBrowser();
    
    /**
     * 获取动作名称
     *
     * @return 动作名称
     */
    @Override
    public String getName();
    
    /**
     * 获取动作描述
     *
     * @return 动作描述
     */
    @Override
    public String getDescription();
    
    /**
     * 获取动作参数描述
     *
     * @return 参数描述
     */
    @Override
    public Map<String, Object> getParameters();
    
    /**
     * 执行动作
     *
     * @param functionCall 函数调用信息
     * @return 动作执行结果
     */
    @Override
    public ActionReturn execute(FunctionCall functionCall);
}
```

## 记忆管理

### Memory

内存类，用于存储和管理代理的对话历史。

```java
public class Memory {
    /**
     * 创建一个无容量限制的内存
     */
    public Memory();
    
    /**
     * 创建一个指定容量的内存
     *
     * @param capacity 内存容量，-1表示无限制
     */
    public Memory(int capacity);
    
    /**
     * 添加一条消息到内存
     *
     * @param message 要添加的消息
     */
    public void add(AgentMessage message);
    
    /**
     * 添加多条消息到内存
     *
     * @param messages 要添加的消息列表
     */
    public void add(List<AgentMessage> messages);
    
    /**
     * 清空内存
     */
    public void clear();
    
    /**
     * 获取内存中的所有消息
     *
     * @return 内存中的所有消息
     */
    public List<AgentMessage> getMemory();
    
    /**
     * 获取指定发送者的消息
     *
     * @param sender 发送者名称
     * @return 指定发送者的所有消息
     */
    public List<AgentMessage> getBySender(String sender);
    
    /**
     * 获取最后一条消息
     *
     * @return 最后一条消息，如果内存为空则返回null
     */
    public AgentMessage getLast();
    
    /**
     * 获取内存大小
     *
     * @return 内存中消息的数量
     */
    public int size();
    
    /**
     * 检查内存是否为空
     *
     * @return 如果内存为空则返回true，否则返回false
     */
    public boolean isEmpty();
}
```

### MemoryManager

内存管理器，用于管理多个会话的内存。

```java
public class MemoryManager {
    /**
     * 创建一个无容量限制的内存管理器
     */
    public MemoryManager();
    
    /**
     * 创建一个指定容量的内存管理器
     *
     * @param capacity 内存容量，-1表示无限制
     */
    public MemoryManager(int capacity);
    
    /**
     * 创建一个使用配置的内存管理器
     *
     * @param memoryConfig 内存配置
     */
    public MemoryManager(Map<String, Object> memoryConfig);
    
    /**
     * 获取指定会话的内存
     *
     * @param sessionId 会话ID
     * @return 会话内存
     */
    public Memory get(int sessionId);
    
    /**
     * 添加一条消息到指定会话
     *
     * @param message   消息
     * @param sessionId 会话ID
     */
    public void add(AgentMessage message, int sessionId);
    
    /**
     * 添加多条消息到指定会话
     *
     * @param messages  消息列表
     * @param sessionId 会话ID
     */
    public void add(List<AgentMessage> messages, int sessionId);
    
    /**
     * 清空指定会话的内存
     *
     * @param sessionId 会话ID
     */
    public void clear(int sessionId);
    
    /**
     * 清空所有会话的内存
     */
    public void clearAll();
    
    /**
     * 移除指定会话
     *
     * @param sessionId 会话ID
     */
    public void remove(int sessionId);
    
    /**
     * 检查是否包含指定会话
     *
     * @param sessionId 会话ID
     * @return 是否包含指定会话
     */
    public boolean containsSession(int sessionId);
    
    /**
     * 获取所有会话ID
     *
     * @return 会话ID列表
     */
    public List<Integer> getSessions();
    
    /**
     * 获取会话数量
     *
     * @return 会话数量
     */
    public int sessionCount();
}
```

## 消息系统

### AgentMessage

代理消息类，用于表示代理之间传递的消息。

```java
public class AgentMessage {
    /**
     * 创建一个只包含发送者和内容的消息
     *
     * @param sender  发送者
     * @param content 消息内容
     */
    public AgentMessage(String sender, Object content);
    
    /**
     * 创建一个包含发送者、内容和格式化内容的消息
     *
     * @param sender    发送者
     * @param content   消息内容
     * @param formatted 格式化内容
     */
    public AgentMessage(String sender, Object content, Object formatted);
    
    /**
     * 深拷贝当前消息
     *
     * @return 拷贝后的新消息对象
     */
    public AgentMessage copy();
    
    // Getter和Setter方法
    public Object getContent();
    public void setContent(Object content);
    public String getSender();
    public void setSender(String sender);
    public Object getFormatted();
    public void setFormatted(Object formatted);
    public Object getExtraInfo();
    public void setExtraInfo(Object extraInfo);
    public String getType();
    public void setType(String type);
    public String getReceiver();
    public void setReceiver(String receiver);
    public AgentStatusCode getStreamState();
    public void setStreamState(AgentStatusCode streamState);
}
```

### FunctionCall

函数调用类，用于表示一个函数调用。

```java
public class FunctionCall {
    /**
     * 创建一个函数调用
     *
     * @param name       函数名称
     * @param parameters 函数参数
     */
    public FunctionCall(String name, Object parameters);
    
    /**
     * 检查参数是否为Map类型
     *
     * @return 如果参数是Map类型则返回true，否则返回false
     */
    public boolean isParametersMap();
    
    /**
     * 获取参数的Map形式
     *
     * @return 参数的Map形式，如果参数不是Map类型则返回null
     */
    public Map<String, Object> getParametersAsMap();
    
    /**
     * 获取参数的字符串形式
     *
     * @return 参数的字符串形式，如果参数不是字符串类型则返回toString()的结果
     */
    public String getParametersAsString();
    
    // Getter和Setter方法
    public String getName();
    public void setName(String name);
    public Object getParameters();
    public void setParameters(Object parameters);
}
```

### ActionReturn

动作返回结果类，用于表示一个动作执行的返回结果。

```java
public class ActionReturn {
    /**
     * 格式化结果为字符串
     *
     * @return 格式化后的结果字符串
     */
    public String formatResult();
    
    // Getter和Setter方法
    public Map<String, Object> getArgs();
    public void setArgs(Map<String, Object> args);
    public String getUrl();
    public void setUrl(String url);
    public String getType();
    public void setType(String type);
    public List<Map<String, String>> getResult();
    public void setResult(List<Map<String, String>> result);
    public String getErrmsg();
    public void setErrmsg(String errmsg);
    public ActionStatusCode getState();
    public void setState(ActionStatusCode state);
    public String getThought();
    public void setThought(String thought);
    public ActionValidCode getValid();
    public void setValid(ActionValidCode valid);
}
```

## 使用示例

### 1. 创建智能体

```java
// 初始化组件
AsyncLlmService llm = new AsyncOpenAILlmService("your-api-key");
Memory memory = new LRUMemory(100);
List<Action> actions = List.of(
    new HttpAction("http", 30),
    new FileSystemAction("fs", "/tmp")
);

// 创建智能体
AsyncStreamAgent agent = new AsyncStreamAgent(
    llm, memory, actions,
    new ToolParser("tool", "```json\n", "\n```"),
    "You are a helpful assistant."
);

// 设置回调
agent.setOnMessage(msg -> {
    if (msg.getStreamState() == StreamState.RUNNING) {
        System.out.print(msg.getContent());
    }
});
```

### 2. 处理消息

```java
// 创建消息
AgentMessage message = AgentMessage.builder()
    .sender("user")
    .content("查询天气")
    .type(AgentMessageType.USER)
    .sessionId(1)
    .build();

// 异步处理
agent.processAsync(message, 1)
    .thenAccept(response -> {
        System.out.println("\n完成: " + response.getContent());
    })
    .exceptionally(ex -> {
        System.err.println("错误: " + ex.getMessage());
        return null;
    });
``` 