# Lagent4j 使用指南

## 目录

- [快速开始](#快速开始)
- [核心概念](#核心概念)
- [进阶用法](#进阶用法)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

## 快速开始

### 1. 基础配置

```java
// 初始化 LLM 服务
AsyncLlmService llm = new AsyncOpenAILlmService("your-api-key");

// 创建记忆管理器
Memory memory = new LRUMemory(100);

// 注册工具
List<Action> actions = List.of(
    new HttpAction("httpTool", 30),
    new PythonInterpreterAction()
);

// 创建工具解析器
OutputFormat parser = new ToolParser(
    "http",
    "```json\n",
    "\n```"
);

// 创建智能体
AsyncStreamAgent agent = new AsyncStreamAgent(
    llm, memory, actions, parser,
    "You are a helpful assistant."
);
```

### 2. 处理消息

```java
// 创建用户消息
AgentMessage userMessage = AgentMessage.builder()
    .sender("user")
    .content("查询天气")
    .type(AgentMessageType.USER)
    .sessionId(1)
    .build();

// 设置回调
agent.setOnMessage(msg -> {
    if (msg.getStreamState() == StreamState.RUNNING) {
        System.out.print(msg.getContent()); // 流式输出
    }
});

// 异步处理
agent.processAsync(userMessage, 1)
    .thenAccept(response -> {
        System.out.println("\n处理完成: " + response.getContent());
    })
    .exceptionally(ex -> {
        System.err.println("错误: " + ex.getMessage());
        return null;
    });
```

## 核心概念

### 1. 消息系统

消息是智能体间通信的基本单位：

```java
AgentMessage message = AgentMessage.builder()
    .sender("user")           // 发送者
    .content("消息内容")      // 内容
    .type(AgentMessageType.USER) // 类型
    .sessionId(1)            // 会话ID
    .streamState(StreamState.RUNNING) // 流式状态
    .build();
```

### 2. 工具调用

工具通过 Action 接口实现：

```java
public class CustomAction implements Action {
    @Override
    public String getName() {
        return "customTool";
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            // 实现工具逻辑
            return ActionResult.builder()
                .status(ActionResult.Status.SUCCESS)
                .output("执行结果")
                .build();
        });
    }
}
```

### 3. 输出格式化

自定义输出格式解析：

```java
public class CustomParser implements OutputFormat {
    @Override
    public Object parseResponse(String response) {
        // 实现解析逻辑
        if (response.contains("调用工具")) {
            return Map.of(
                "tool_type", "custom",
                "action", "doSomething",
                "parameters", Map.of("param1", "value1")
            );
        }
        return null;
    }
}
```

## 进阶用法

### 1. 流式处理

```java
AsyncStreamAgent agent = new AsyncStreamAgent(/*...*/);

// 处理流式输出
agent.setOnMessage(msg -> {
    switch (msg.getStreamState()) {
        case START -> System.out.println("开始响应");
        case RUNNING -> System.out.print(msg.getContent());
        case END -> System.out.println("\n响应完成");
    }
});
```

### 2. 会话管理

```java
// 使用不同的会话ID隔离对话
agent.processAsync(message1, 1); // 会话1
agent.processAsync(message2, 2); // 会话2

// 清理会话记忆
agent.reset(1); // 清理会话1的历史
```

### 3. 错误处理

```java
agent.setOnError(ex -> {
    if (ex instanceof TimeoutException) {
        System.err.println("请求超时");
    } else if (ex instanceof IllegalArgumentException) {
        System.err.println("参数错误");
    } else {
        System.err.println("未知错误: " + ex.getMessage());
    }
});
```

## 最佳实践

### 1. 资源管理

```java
// 使用try-with-resources管理资源
try (Response response = httpClient.newCall(request).execute()) {
    // 处理响应
}

// 及时清理不需要的会话记忆
agent.reset(sessionId);
```

### 2. 异步处理

```java
// 使用thenCompose链接多个异步操作
agent.processAsync(message, sessionId)
    .thenCompose(response -> {
        // 处理响应
        return anotherAsyncOperation(response);
    })
    .thenAccept(finalResult -> {
        // 处理最终结果
    });
```

### 3. 超时控制

```java
// 设置超时
CompletableFuture<AgentMessage> future = agent.processAsync(message, sessionId);
AgentMessage response = future.get(30, TimeUnit.SECONDS);

// 或者使用orTimeout
future.orTimeout(30, TimeUnit.SECONDS)
    .exceptionally(ex -> {
        if (ex instanceof TimeoutException) {
            return createTimeoutResponse();
        }
        throw new CompletionException(ex);
    });
```

## 常见问题

### Q1: 如何处理工具调用超时？

```java
// 在工具实现中添加超时控制
public class TimeoutAwareAction implements Action {
    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            // 执行工具逻辑
        }).orTimeout(30, TimeUnit.SECONDS);
    }
}
```

### Q2: 如何实现自定义的记忆管理？

```java
public class CustomMemory implements Memory {
    private final Map<Integer, Queue<AgentMessage>> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void addMessage(Integer sessionId, AgentMessage message) {
        sessions.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>())
            .offer(message);
    }
    
    @Override
    public Deque<AgentMessage> getMessages(Integer sessionId) {
        return new ArrayDeque<>(sessions.getOrDefault(sessionId, new ConcurrentLinkedQueue<>()));
    }
}
```

### Q3: 如何集成自定义的LLM服务？

```java
public class CustomLlmService implements AsyncLlmService {
    @Override
    public CompletableFuture<String> chatAsync(List<AgentMessage> messages) {
        return CompletableFuture.supplyAsync(() -> {
            // 实现自定义LLM调用逻辑
            return "LLM响应";
        });
    }
}
``` 