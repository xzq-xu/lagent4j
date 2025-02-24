# Lagent4j 开发文档

## 架构概述

Lagent4j 采用模块化设计，主要包含以下核心组件：

### 1. 消息系统 (Message System)

消息是智能体间通信的基本单位，由 `AgentMessage` 类表示：

```java
public class AgentMessage {
    private String sender;        // 发送者
    private String content;       // 消息内容
    private Object formatted;     // 结构化数据
    private StreamState state;    // 流式状态
    // ...
}
```

### 2. 记忆管理 (Memory Management)

`Memory` 接口定义了会话隔离的记忆管理：

```java
public interface Memory {
    void addMessage(Integer sessionId, AgentMessage message);
    Deque<AgentMessage> getMessages(Integer sessionId);
    void clear(Integer sessionId);
}
```

### 3. LLM 服务 (LLM Services)

支持同步和异步两种调用模式：

```java
public interface AsyncLlmService {
    CompletableFuture<String> chatAsync(List<AgentMessage> messages);
    CompletableFuture<Void> chatStreamAsync(
        List<AgentMessage> messages,
        Consumer<String> onResponse,
        Consumer<Throwable> onError
    );
}
```

### 4. 工具系统 (Tool System)

工具通过 `Action` 接口实现：

```java
public interface Action {
    String getName();
    CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters);
}
```

## 异步支持

### 1. 基本原理

- 使用 CompletableFuture 实现异步操作
- 采用回调机制处理中间状态
- 支持流式响应和工具调用

### 2. 实现流程

1. 消息处理：
```java
CompletableFuture<AgentMessage> processAsync(message, sessionId)
    -> 存储消息
    -> 处理消息
    -> 检查工具调用
    -> 返回响应
```

2. 工具调用：
```java
CompletableFuture<AgentMessage> executeTool(toolCall, sessionId)
    -> 查找工具
    -> 执行工具
    -> 处理结果
    -> 继续对话
```

3. 流式响应：
```java
CompletableFuture<Void> chatStreamAsync(messages, onResponse, onError)
    -> 发送请求
    -> 处理数据流
    -> 回调响应
    -> 完成处理
```

## 最佳实践

### 1. 错误处理

```java
.exceptionally(ex -> {
    if (onError != null) {
        onError.accept(ex);
    }
    throw new CompletionException(ex);
})
```

### 2. 资源管理

```java
try (ResponseBody body = response.body()) {
    // 处理响应
} catch (Exception e) {
    // 处理错误
}
```

### 3. 会话隔离

```java
// 使用 sessionId 隔离不同会话
memory.addMessage(sessionId, message);
memory.getMessages(sessionId);
```

### 4. 消息回调

```java
agent.setOnMessage(msg -> {
    switch (msg.getStreamState()) {
        case START -> System.out.println("开始响应");
        case RUNNING -> System.out.print(msg.getContent());
        case END -> System.out.println("\n响应完成");
    }
});
```

## 扩展开发

### 1. 添加新工具

```java
public class CustomAction implements Action {
    @Override
    public String getName() {
        return "customAction";
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            // 实现工具逻辑
            return ActionResult.builder()
                .status(ActionResult.Status.SUCCESS)
                .output("结果")
                .build();
        });
    }
}
```

### 2. 自定义输出格式化

```java
public class CustomOutputFormat implements OutputFormat {
    @Override
    public Object parseResponse(String response) {
        // 实现自定义解析逻辑
        return parsedResult;
    }
}
```

### 3. 实现新的智能体

```java
public class CustomAgent extends AsyncBaseAgent {
    @Override
    protected CompletableFuture<AgentMessage> handleMessageAsync(
            AgentMessage message,
            Integer sessionId) {
        // 实现消息处理逻辑
        return processResult;
    }
}
```

## 性能优化

1. 使用连接池管理 HTTP 连接
2. 实现请求重试和超时控制
3. 采用适当的缓存策略
4. 优化并发处理

## 测试指南

1. 单元测试覆盖核心逻辑
2. 集成测试验证组件交互
3. 性能测试评估系统表现
4. 异常测试确保错误处理

# 开发指南

## 项目结构

```
lagent4j/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── example/
│   │               └── lagent4j/
│   │                   ├── actions/     # 工具实现
│   │                   ├── agent/       # 智能体核心
│   │                   ├── llm/         # LLM服务
│   │                   ├── prompts/     # 提示词和解析器
│   │                   └── examples/    # 示例实现
│   └── test/
│       └── java/      # 测试用例
└── docs/             # 文档
```

## 核心概念

### Agent

Agent 是框架的核心，负责：
- 管理对话历史
- 调用 LLM 服务
- 执行工具调用
- 处理流式响应

### 工具系统

工具系统包括：
- Action 接口：定义工具操作
- ActionResult：工具执行结果
- ToolFactory：工具创建工厂
- ToolRegistry：工具注册表

### 内存管理

Memory 接口定义了会话记忆管理：
- get：获取会话历史
- add：添加新消息
- clear：清除会话

### 消息系统

AgentMessage 类型包含：
- 基本信息（发送者、内容等）
- 消息类型（系统、用户、助手、工具）
- 流式状态（开始、进行中、结束）
- 格式化数据（工具调用参数等）

## 最佳实践

### 异步处理

```java
// 推荐：使用异步方法
CompletableFuture<String> future = agent.processAsync(message, sessionId)
    .thenApply(response -> response.getContent());

// 不推荐：同步等待
String result = agent.processAsync(message, sessionId).get();
```

### 错误处理

```java
// 推荐：使用 exceptionally 处理错误
agent.processAsync(message, sessionId)
    .exceptionally(ex -> {
        logger.error("处理失败", ex);
        return null;
    });

// 不推荐：直接抛出异常
try {
    agent.processAsync(message, sessionId).get();
} catch (Exception e) {
    // 处理错误
}
```

### 资源管理

```java
// 推荐：使用 try-with-resources
try (WeatherAssistant assistant = new WeatherAssistant(apiKey)) {
    assistant.checkWeather("Shanghai");
}

// 不推荐：手动关闭
WeatherAssistant assistant = new WeatherAssistant(apiKey);
try {
    assistant.checkWeather("Shanghai");
} finally {
    assistant.close();
}
```

## 测试指南

### 单元测试

- 使用 Mock 工具替代真实服务
- 测试各个组件的独立功能
- 验证错误处理逻辑

### 集成测试

- 测试完整的工作流程
- 验证组件间的交互
- 测试并发和性能

## 性能优化

- 使用连接池管理数据库连接
- 实现请求缓存
- 优化内存使用
- 控制并发数量

## 安全建议

- 验证工具调用参数
- 限制文件操作范围
- 加密敏感配置
- 实现访问控制 