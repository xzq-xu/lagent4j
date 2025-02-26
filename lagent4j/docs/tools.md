# 工具系统文档

## 内置工具

### HTTP工具

用于发送HTTP请求：

```java
HttpRequestAction httpTool = ToolFactory.createHttpTool();
```

参数：
- url：请求URL
- method：请求方法（GET/POST）

### 数据库工具

执行SQL操作：

```java
SimpleDbAction dbTool = ToolFactory.createDatabaseTool(
    "jdbc:h2:mem:testdb",
    "sa",
    "",
    5
);
```

参数：
- sql：SQL语句
- type：操作类型（query/update）

### 文件系统工具

处理文件操作：

```java
FileIOAction fsTool = ToolFactory.createFileSystemTool("./output");
```

参数：
- operation：操作类型（read/write/delete）
- path：文件路径
- content：文件内容（写入时）

## 自定义工具

### 实现步骤

1. 实现 Action 接口
2. 实现必要的方法
3. 注册到工具系统

### 示例代码

```java
public class CustomTool implements Action {
    @Override
    public String getName() {
        return "custom_tool";
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 实现工具逻辑
                return ActionResult.builder()
                    .status(ActionResult.Status.SUCCESS)
                    .output("结果")
                    .build();
            } catch (Exception e) {
                return ActionResult.builder()
                    .status(ActionResult.Status.FAILURE)
                    .error(e.getMessage())
                    .build();
            }
        });
    }
}
```

## 工具配置

### 配置选项

```java
ToolConfig config = ToolConfig.builder()
    .name("my_tool")
    .description("My custom tool")
    .timeout(30)
    .maxRetries(3)
    .async(true)
    .build();
```

### 注册工具

```java
ToolRegistry registry = ToolRegistry.getInstance();
registry.register("my_tool", new CustomTool());
```

## 最佳实践

- 实现适当的错误处理
- 添加超时机制
- 实现重试逻辑
- 添加日志记录
- 进行参数验证 