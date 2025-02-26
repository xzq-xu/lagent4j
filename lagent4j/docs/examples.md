# Lagent4j 场景示例

## 目录

- [智能问答](#智能问答)
- [数据分析](#数据分析)
- [自动化工具](#自动化工具)
- [多轮对话](#多轮对话)
- [并发处理](#并发处理)

## 智能问答

### 1. 天气查询助手

```java
public class WeatherAssistant {
    private final AsyncStreamAgent agent;
    
    public WeatherAssistant(String apiKey) {
        // 初始化天气API工具
        HttpAction weatherApi = new HttpAction("weatherApi", 10);
        
        // 创建工具解析器
        ToolParser parser = new ToolParser(
            "weather",
            "```json\n",
            "\n```"
        );
        
        // 初始化智能体
        agent = new AsyncStreamAgent(
            new AsyncOpenAILlmService(apiKey),
            new LRUMemory(50),
            List.of(weatherApi),
            parser,
            "你是一个天气助手，可以查询全球各地的天气信息。使用weatherApi工具获取数据。"
        );
        
        // 设置响应处理
        agent.setOnMessage(msg -> {
            if (msg.getStreamState() == StreamState.RUNNING) {
                System.out.print(msg.getContent());
            }
        });
    }
    
    public CompletableFuture<String> queryWeather(String location) {
        AgentMessage query = AgentMessage.builder()
            .sender("user")
            .content("查询" + location + "的天气")
            .type(AgentMessageType.USER)
            .sessionId(1)
            .build();
            
        return agent.processAsync(query, 1)
            .thenApply(AgentMessage::getContent);
    }
}

// 使用示例
WeatherAssistant assistant = new WeatherAssistant("your-api-key");
assistant.queryWeather("上海")
    .thenAccept(System.out::println);
```

### 2. 代码助手

```java
public class CodeAssistant {
    private final AsyncStreamAgent agent;
    
    public CodeAssistant(String apiKey) {
        // 初始化Python解释器
        PythonInterpreterAction python = new PythonInterpreterAction();
        
        // 创建解析器
        ToolParser parser = new ToolParser(
            "python",
            "```python\n",
            "\n```"
        );
        
        agent = new AsyncStreamAgent(
            new AsyncOpenAILlmService(apiKey),
            new LRUMemory(50),
            List.of(python),
            parser,
            "你是一个Python编程助手，可以编写和执行Python代码。"
        );
    }
    
    public CompletableFuture<String> solveProblem(String problem) {
        return agent.processAsync(
            AgentMessage.builder()
                .sender("user")
                .content(problem)
                .type(AgentMessageType.USER)
                .sessionId(1)
                .build(),
            1
        ).thenApply(AgentMessage::getContent);
    }
}

// 使用示例
CodeAssistant assistant = new CodeAssistant("your-api-key");
assistant.solveProblem("计算斐波那契数列的前10个数")
    .thenAccept(System.out::println);
```

## 数据分析

### 1. 数据可视化助手

```java
public class DataVisualizationAssistant {
    private final AsyncStreamAgent agent;
    
    public DataVisualizationAssistant(String apiKey) {
        // 组合Python和HTTP工具
        List<Action> actions = List.of(
            new PythonInterpreterAction(),
            new HttpAction("dataApi", 30)
        );
        
        agent = new AsyncStreamAgent(
            new AsyncOpenAILlmService(apiKey),
            new LRUMemory(100),
            actions,
            new ToolParser("visualization", "```python\n", "\n```"),
            "你是一个数据可视化助手，可以获取数据并使用Python生成图表。"
        );
    }
    
    public CompletableFuture<String> createVisualization(String dataSource, String requirement) {
        String prompt = String.format(
            "数据源: %s\n可视化需求: %s\n请获取数据并生成可视化图表。",
            dataSource, requirement
        );
        
        return agent.processAsync(
            AgentMessage.builder()
                .sender("user")
                .content(prompt)
                .type(AgentMessageType.USER)
                .sessionId(1)
                .build(),
            1
        ).thenApply(AgentMessage::getContent);
    }
}

// 使用示例
DataVisualizationAssistant assistant = new DataVisualizationAssistant("your-api-key");
assistant.createVisualization(
    "https://api.example.com/sales-data",
    "生成过去12个月的销售趋势图"
).thenAccept(System.out::println);
```

## 自动化工具

### 1. 文件处理助手

```java
public class FileProcessingAssistant {
    private final AsyncStreamAgent agent;
    
    public FileProcessingAssistant(String apiKey) {
        FileAction fileAction = new FileAction();
        PythonInterpreterAction python = new PythonInterpreterAction();
        
        agent = new AsyncStreamAgent(
            new AsyncOpenAILlmService(apiKey),
            new LRUMemory(50),
            List.of(fileAction, python),
            new ToolParser("file", "```json\n", "\n```"),
            "你是一个文件处理助手，可以读取、处理和保存文件。"
        );
    }
    
    public CompletableFuture<String> processFile(String inputPath, String outputPath, String task) {
        String prompt = String.format(
            "请处理文件'%s'，执行任务：%s，并将结果保存到'%s'",
            inputPath, task, outputPath
        );
        
        return agent.processAsync(
            AgentMessage.builder()
                .sender("user")
                .content(prompt)
                .type(AgentMessageType.USER)
                .sessionId(1)
                .build(),
            1
        ).thenApply(AgentMessage::getContent);
    }
}

// 使用示例
FileProcessingAssistant assistant = new FileProcessingAssistant("your-api-key");
assistant.processFile(
    "input.csv",
    "output.csv",
    "将CSV文件中的数据按照'sales'列排序，并计算每行的总和"
).thenAccept(System.out::println);
```

## 多轮对话

### 1. 客服助手

```java
public class CustomerServiceAssistant {
    private final AsyncStreamAgent agent;
    private final Map<String, Integer> userSessions = new ConcurrentHashMap<>();
    
    public CustomerServiceAssistant(String apiKey) {
        HttpAction crm = new HttpAction("crmApi", 30);
        
        agent = new AsyncStreamAgent(
            new AsyncOpenAILlmService(apiKey),
            new LRUMemory(200),
            List.of(crm),
            new ToolParser("crm", "```json\n", "\n```"),
            "你是一个客服助手，可以查询订单信息和处理客户问题。"
        );
    }
    
    public CompletableFuture<String> chat(String userId, String message) {
        // 获取或创建会话ID
        Integer sessionId = userSessions.computeIfAbsent(
            userId,
            k -> userSessions.size() + 1
        );
        
        return agent.processAsync(
            AgentMessage.builder()
                .sender("user")
                .content(message)
                .type(AgentMessageType.USER)
                .sessionId(sessionId)
                .build(),
            sessionId
        ).thenApply(AgentMessage::getContent);
    }
    
    public void endChat(String userId) {
        Integer sessionId = userSessions.remove(userId);
        if (sessionId != null) {
            agent.reset(sessionId);
        }
    }
}

// 使用示例
CustomerServiceAssistant assistant = new CustomerServiceAssistant("your-api-key");

// 用户对话
assistant.chat("user123", "我的订单什么时候发货？")
    .thenCompose(response -> {
        System.out.println("Assistant: " + response);
        return assistant.chat("user123", "能帮我修改收货地址吗？");
    })
    .thenAccept(response -> {
        System.out.println("Assistant: " + response);
        assistant.endChat("user123");
    });
```

## 并发处理

### 1. 批量处理助手

```java
public class BatchProcessingAssistant {
    private final AsyncStreamAgent agent;
    private final ExecutorService executor;
    
    public BatchProcessingAssistant(String apiKey, int maxConcurrent) {
        agent = new AsyncStreamAgent(
            new AsyncOpenAILlmService(apiKey),
            new LRUMemory(1000),
            List.of(new HttpAction("api", 30), new PythonInterpreterAction()),
            new ToolParser("batch", "```json\n", "\n```"),
            "你是一个批量处理助手，可以并行处理多个任务。"
        );
        
        executor = Executors.newFixedThreadPool(maxConcurrent);
    }
    
    public CompletableFuture<List<String>> processBatch(List<String> tasks) {
        List<CompletableFuture<String>> futures = tasks.stream()
            .map(task -> CompletableFuture.supplyAsync(() -> {
                try {
                    return agent.processAsync(
                        AgentMessage.builder()
                            .sender("user")
                            .content(task)
                            .type(AgentMessageType.USER)
                            .sessionId(tasks.indexOf(task))
                            .build(),
                        tasks.indexOf(task)
                    ).get(60, TimeUnit.SECONDS)
                    .getContent();
                } catch (Exception e) {
                    return "Error: " + e.getMessage();
                }
            }, executor))
            .toList();
            
        return CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        ).thenApply(v -> 
            futures.stream()
                .map(CompletableFuture::join)
                .toList()
        );
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}

// 使用示例
BatchProcessingAssistant assistant = new BatchProcessingAssistant("your-api-key", 5);

List<String> tasks = List.of(
    "分析 A 公司的财报",
    "统计 B 产品的销量",
    "生成 C 项目的报告"
);

assistant.processBatch(tasks)
    .thenAccept(results -> {
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("Task %d: %s%n", i + 1, results.get(i));
        }
        assistant.shutdown();
    });
```

这些示例展示了：

1. 不同场景下的智能体配置
2. 工具组合使用方法
3. 会话管理策略
4. 并发处理方案
5. 错误处理机制

每个示例都包含完整的实现和使用方法，可以直接参考或修改使用。 