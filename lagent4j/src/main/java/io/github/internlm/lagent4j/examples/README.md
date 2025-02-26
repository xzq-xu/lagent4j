# lagent4j 示例

本目录包含了 lagent4j 框架的各种使用示例，帮助你快速上手和理解框架的功能。

## 环境准备

在运行示例之前，请确保设置以下环境变量：

```bash
# OpenAI API密钥
export OPENAI_API_KEY=your_openai_api_key

# Bing搜索API密钥（用于网页搜索示例）
export BING_API_KEY=your_bing_api_key
```

## 示例列表

### 1. 简单对话 (SimpleExample.java)
演示如何创建一个基本的对话代理，进行简单的问答交互。

```java
OpenAIModel model = new OpenAIModel.Builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .model("gpt-3.5-turbo")
        .build();

Agent agent = new Agent(model, "你是一个有帮助的助手");
AgentMessage response = agent.process(new AgentMessage("user", "你好！"));
System.out.println(response.getContent());
```

### 2. 流式输出 (StreamExample.java)
展示如何使用流式输出功能，实时获取模型的响应。

```java
OpenAIModel model = new OpenAIModel.Builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .model("gpt-3.5-turbo")
        .build();

AsyncStreamAgent agent = new AsyncStreamAgent(model, "你是一个支持流式输出的助手");
agent.processStream(question, 1, new StreamCallback() {
    @Override
    public void onChunk(String chunk, ModelStatusCode status) {
        if (status == ModelStatusCode.GENERATING) {
            System.out.print(chunk);
        }
    }
    // ... 其他回调方法
});
```

### 3. 工具使用 (ToolExample.java)
演示如何创建和使用工具，扩展代理的能力。

```java
ActionExecutor executor = new ActionExecutor();
executor.registerAction(new WebBrowserAction());

Agent agent = new Agent(model, template, memory, parser, aggregator, executor);
AgentMessage response = agent.process(new AgentMessage("user", "查询今天的天气"));
```

### 4. 多代理协作 (MultiAgentExample.java)
展示如何创建多个专业领域的代理，并让它们协同工作。

```java
Agent pythonExpert = new Agent(model, "你是Python专家");
Agent javaExpert = new Agent(model, "你是Java专家");
Agent moderator = new Agent(model, "你是讨论主持人");

// 让专家们讨论问题
moderator.process(question);
pythonExpert.process(question);
javaExpert.process(question);
```

### 5. 异步流式处理 (AsyncStreamExample.java)
展示如何使用异步流式代理，结合工具使用进行实时信息查询。

```java
AsyncStreamAgent agent = new AsyncStreamAgent(
        model,                  // 语言模型
        systemPrompt,          // 系统提示
        memory,                // 内存配置
        new StrParser(),       // 输出格式解析器
        new DefaultAggregator(), // 消息聚合器
        executor,              // 工具执行器
        "stream_assistant",    // 代理名称
        "支持流式输出的智能助手"  // 代理描述
);

agent.processStream(question, sessionId, new StreamCallback() {
    // ... 处理回调
});
```

### 6. 网页浏览 (WebBrowserExample.java)
演示如何使用网页浏览工具进行实时信息搜索。

```java
OpenAIModel model = new OpenAIModel.Builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .model("gpt-3.5-turbo")
        .build();

ActionExecutor executor = new ActionExecutor();
executor.registerAction(new WebBrowserAction());

Agent agent = new Agent(model, "你是一个可以搜索网页的助手", executor);
agent.process(new AgentMessage("user", "查询最新的科技新闻"));
```

## 注意事项

1. 请确保在运行示例前设置好所需的环境变量。
2. 部分示例可能需要网络连接和API访问权限。
3. 建议先阅读代码注释，了解示例的具体功能和使用方法。
4. 可以根据需要修改示例代码，探索更多使用场景。

## 自定义配置

OpenAIModel 支持多种配置选项：

```java
OpenAIModel model = new OpenAIModel.Builder()
        .apiKey("your_api_key")           // API密钥
        .baseUrl("your_base_url")         // 自定义基础URL（可选）
        .model("gpt-3.5-turbo")           // 模型名称
        .temperature(0.7)                  // 温度参数
        .maxTokens(2048)                  // 最大生成长度
        .topP(1.0)                        // Top P参数
        .frequencyPenalty(0.0)            // 频率惩罚
        .presencePenalty(0.0)             // 存在惩罚
        .timeoutSeconds(60)               // 超时时间
        .build();
``` 