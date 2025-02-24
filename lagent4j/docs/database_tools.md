# Lagent4j 数据库工具指南

## 目录

- [简介](#简介)
- [基本用法](#基本用法)
- [高级特性](#高级特性)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

## 简介

Lagent4j 提供了两种数据库操作工具：

1. SimpleDbAction - 轻量级连接池实现
2. DatabaseAction - 基于 HikariCP 的高性能实现

### 特性对比

| 特性 | SimpleDbAction | DatabaseAction |
|------|---------------|----------------|
| 连接池 | 基础实现 | HikariCP |
| 性能 | 一般 | 高 |
| 依赖 | 无 | HikariCP |
| 适用场景 | 开发测试 | 生产环境 |

## 基本用法

### 1. 创建数据库工具

```java
// 简单实现
SimpleDbAction simpleDb = new SimpleDbAction(
    "simpleDb",          // 工具名称
    "jdbc:mysql://...",  // JDBC URL
    "username",          // 用户名
    "password",          // 密码
    10                   // 最大连接数
);

// HikariCP 实现
DatabaseAction hikariDb = new DatabaseAction(
    "hikariDb",          // 工具名称
    "jdbc:mysql://...",  // JDBC URL
    "username",          // 用户名
    "password"           // 密码
);
```

### 2. 执行查询

```java
// 查询示例
Map<String, Object> queryParams = Map.of(
    "sql", "SELECT * FROM users WHERE id = 1",
    "type", "query"
);

dbAction.executeAsync(queryParams)
    .thenAccept(result -> {
        if (result.getStatus() == ActionResult.Status.SUCCESS) {
            System.out.println("查询结果: " + result.getOutput());
        } else {
            System.err.println("查询失败: " + result.getError());
        }
    });
```

### 3. 执行更新

```java
// 更新示例
Map<String, Object> updateParams = Map.of(
    "sql", "UPDATE users SET name = 'test' WHERE id = 1",
    "type", "update"
);

dbAction.executeAsync(updateParams)
    .thenAccept(result -> {
        if (result.getStatus() == ActionResult.Status.SUCCESS) {
            System.out.println("更新成功: " + result.getOutput());
        } else {
            System.err.println("更新失败: " + result.getError());
        }
    });
```

## 高级特性

### 1. 连接池管理

```java
// 配置 HikariCP
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(10);
config.setMinimumIdle(5);
config.setIdleTimeout(300000);
config.setConnectionTimeout(20000);

DatabaseAction dbAction = new DatabaseAction(
    "customDb",
    config
);
```

### 2. 事务处理

```java
// 在同一连接中执行多个操作
Map<String, Object> params = Map.of(
    "sql", "START TRANSACTION; " +
           "INSERT INTO orders (id, amount) VALUES (1, 100); " +
           "UPDATE inventory SET stock = stock - 1 WHERE id = 1; " +
           "COMMIT;",
    "type", "update"
);

dbAction.executeAsync(params)
    .exceptionally(ex -> {
        System.err.println("事务失败: " + ex.getMessage());
        return null;
    });
```

## 最佳实践

1. 连接池配置
```java
// 生产环境推荐配置
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(10);
config.setMinimumIdle(5);
config.setIdleTimeout(300000);
config.setConnectionTimeout(20000);
config.setValidationTimeout(5000);
config.setMaxLifetime(1800000);
```

2. 错误处理
```java
dbAction.executeAsync(params)
    .thenAccept(result -> {
        if (result.getStatus() == ActionResult.Status.FAILURE) {
            // 记录错误
            logger.error("数据库操作失败: {}", result.getError());
            // 重试逻辑
            retryOperation(params);
        }
    })
    .exceptionally(ex -> {
        logger.error("异常: ", ex);
        return null;
    });
```

3. 资源管理
```java
// 使用完毕后关闭
try {
    dbAction.close();
} catch (Exception e) {
    logger.error("关闭连接池失败", e);
}
```

## 常见问题

### Q1: 如何处理连接超时？

```java
// 设置超时时间
dbAction.executeAsync(params)
    .orTimeout(5, TimeUnit.SECONDS)
    .exceptionally(ex -> {
        if (ex instanceof TimeoutException) {
            return ActionResult.builder()
                .status(ActionResult.Status.FAILURE)
                .error("操作超时")
                .build();
        }
        throw new CompletionException(ex);
    });
```

### Q2: 如何处理大结果集？

```java
// 分页查询
for (int page = 0; page < totalPages; page++) {
    Map<String, Object> params = Map.of(
        "sql", String.format(
            "SELECT * FROM large_table LIMIT %d OFFSET %d",
            pageSize,
            page * pageSize
        ),
        "type", "query"
    );
    
    // 处理每页数据
    dbAction.executeAsync(params)
        .thenAccept(this::processPageData);
}
```

### Q3: 如何进行批量操作？

```java
// 批量插入
StringBuilder batch = new StringBuilder("INSERT INTO users (name, age) VALUES ");
for (User user : users) {
    batch.append(String.format("('%s', %d),", user.getName(), user.getAge()));
}
batch.setLength(batch.length() - 1); // 移除最后的逗号

Map<String, Object> params = Map.of(
    "sql", batch.toString(),
    "type", "update"
);

dbAction.executeAsync(params);
``` 