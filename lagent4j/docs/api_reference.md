# Lagent4j API 参考文档

## 目录

- [核心接口](#核心接口)
- [智能体实现](#智能体实现)
- [工具系统](#工具系统)
- [记忆管理](#记忆管理)
- [消息系统](#消息系统)

## 核心接口

### AsyncAgent

异步智能体接口，定义了基本的消息处理能力。

```java
public interface AsyncAgent {
    /**
     * 异步处理消息
     * 
     * @param message 待处理的消息
     * @param sessionId 会话ID
     * @return 处理结果的Future
     */
    CompletableFuture<AgentMessage> processAsync(AgentMessage message, Integer sessionId);
    
    /**
     * 重置会话状态
     * 
     * @param sessionId 会话ID
     * @return 完成状态的Future
     */
    CompletableFuture<Void> reset(Integer sessionId);
}
```

### AsyncLlmService

异步LLM服务接口，提供与语言模型的交互能力。

```java
public interface AsyncLlmService {
    /**
     * 异步聊天请求
     * 
     * @param messages 消息历史
     * @return 响应文本的Future
     */
    CompletableFuture<String> chatAsync(List<AgentMessage> messages);
    
    /**
     * 流式聊天请求
     * 
     * @param messages 消息历史
     * @param onResponse 响应回调
     * @param onError 错误回调
     * @return 完成状态的Future
     */
    CompletableFuture<Void> chatStreamAsync(
        List<AgentMessage> messages,
        Consumer<String> onResponse,
        Consumer<Throwable> onError
    );
}
```

## 智能体实现

### AsyncBaseAgent

异步智能体的基础实现。

```java
public abstract class AsyncBaseAgent implements AsyncAgent {
    /**
     * 构造函数
     * 
     * @param llmService LLM服务
     * @param memory 记忆管理器
     * @param actions 可用工具列表
     */
    public AsyncBaseAgent(
        AsyncLlmService llmService,
        Memory memory,
        List<Action> actions
    );
    
    /**
     * 设置消息回调
     * 
     * @param onMessage 消息处理回调
     */
    public void setOnMessage(Consumer<AgentMessage> onMessage);
    
    /**
     * 设置错误回调
     * 
     * @param onError 错误处理回调
     */
    public void setOnError(Consumer<Throwable> onError);
}
```

### AsyncStreamAgent

支持流式输出的智能体实现。

```java
public class AsyncStreamAgent extends AsyncBaseAgent {
    /**
     * 构造函数
     * 
     * @param llmService LLM服务
     * @param memory 记忆管理器
     * @param actions 可用工具列表
     * @param outputFormat 输出格式化器
     * @param systemPrompt 系统提示词
     */
    public AsyncStreamAgent(
        AsyncLlmService llmService,
        Memory memory,
        List<Action> actions,
        OutputFormat outputFormat,
        String systemPrompt
    );
}
```

## 工具系统

### Action

工具接口，定义了工具的基本能力。

```java
public interface Action {
    /**
     * 获取工具名称
     * 
     * @return 工具名称
     */
    String getName();
    
    /**
     * 异步执行工具
     * 
     * @param parameters 执行参数
     * @return 执行结果的Future
     */
    CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters);
}
```

### HttpAction

HTTP请求工具实现。

```java
public class HttpAction implements Action {
    /**
     * 构造函数
     * 
     * @param name 工具名称
     * @param timeoutSeconds 超时时间(秒)
     */
    public HttpAction(String name, int timeoutSeconds);
    
    /**
     * 支持的参数:
     * - url: 请求URL
     * - method: 请求方法(GET/POST/PUT/DELETE)
     * - headers: 请求头
     * - body: 请求体
     */
}
```

### FileSystemAction

文件系统操作工具。

```java
public class FileSystemAction implements Action {
    /**
     * 构造函数
     * 
     * @param name 工具名称
     * @param rootPath 根目录路径
     */
    public FileSystemAction(String name, String rootPath);
    
    /**
     * 支持的操作:
     * - read: 读取文件
     * - write: 写入文件
     * - delete: 删除文件
     * - list: 列出目录
     * - mkdir: 创建目录
     * - exists: 检查存在
     */
}
```

### ImageProcessingAction

图像处理工具。

```java
public class ImageProcessingAction implements Action {
    /**
     * 构造函数
     * 
     * @param name 工具名称
     */
    public ImageProcessingAction(String name);
    
    /**
     * 支持的操作:
     * - resize: 调整大小
     * - rotate: 旋转图像
     * - convert: 格式转换
     */
}
```

## 记忆管理

### Memory

记忆管理接口。

```java
public interface Memory {
    /**
     * 添加消息
     * 
     * @param sessionId 会话ID
     * @param message 消息对象
     */
    void addMessage(Integer sessionId, AgentMessage message);
    
    /**
     * 获取会话历史
     * 
     * @param sessionId 会话ID
     * @return 消息队列
     */
    Deque<AgentMessage> getMessages(Integer sessionId);
    
    /**
     * 清理会话
     * 
     * @param sessionId 会话ID
     */
    void clear(Integer sessionId);
}
```

## 消息系统

### AgentMessage

智能体消息类。

```java
@Builder
public class AgentMessage {
    /**
     * 消息属性
     */
    private String sender;        // 发送者
    private String content;       // 内容
    private Object formatted;     // 结构化数据
    private AgentMessageType type;// 消息类型
    private Integer sessionId;    // 会话ID
    private StreamState state;    // 流式状态
    
    /**
     * 消息类型枚举
     */
    public enum AgentMessageType {
        USER,   // 用户消息
        AGENT,  // 智能体消息
        TOOL    // 工具消息
    }
    
    /**
     * 流式状态枚举
     */
    public enum StreamState {
        START,   // 开始
        RUNNING, // 运行中
        END      // 结束
    }
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