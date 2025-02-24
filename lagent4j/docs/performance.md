# Lagent4j 性能优化指南

## 目录

- [内存管理](#内存管理)
- [并发优化](#并发优化)
- [网络优化](#网络优化)
- [监控指标](#监控指标)

## 内存管理

### 1. 记忆管理优化

```java
public class OptimizedMemory implements Memory {
    private final int maxSize;
    private final Map<Integer, Deque<AgentMessage>> sessions;
    private final LoadingCache<Integer, Deque<AgentMessage>> cache;
    
    public OptimizedMemory(int maxSize, long cacheExpireMinutes) {
        this.maxSize = maxSize;
        this.sessions = new ConcurrentHashMap<>();
        
        // 使用Guava缓存管理会话
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(cacheExpireMinutes, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public Deque<AgentMessage> load(Integer sessionId) {
                    return sessions.getOrDefault(sessionId, new ArrayDeque<>());
                }
            });
    }
    
    @Override
    public void addMessage(Integer sessionId, AgentMessage message) {
        sessions.compute(sessionId, (k, v) -> {
            Deque<AgentMessage> deque = v == null ? new ArrayDeque<>() : v;
            deque.addLast(message);
            while (deque.size() > maxSize) {
                deque.removeFirst();
            }
            return deque;
        });
        cache.invalidate(sessionId);
    }
}
```

### 2. 消息对象池

```java
public class MessagePool {
    private final ObjectPool<AgentMessage.AgentMessageBuilder> pool;
    
    public MessagePool(int maxIdle) {
        GenericObjectPoolConfig<AgentMessage.AgentMessageBuilder> config = 
            new GenericObjectPoolConfig<>();
        config.setMaxIdle(maxIdle);
        
        pool = new GenericObjectPool<>(new BasePooledObjectFactory<>() {
            @Override
            public AgentMessage.AgentMessageBuilder create() {
                return AgentMessage.builder();
            }
            
            @Override
            public PooledObject<AgentMessage.AgentMessageBuilder> wrap(
                    AgentMessage.AgentMessageBuilder builder) {
                return new DefaultPooledObject<>(builder);
            }
            
            @Override
            public void passivateObject(
                    PooledObject<AgentMessage.AgentMessageBuilder> p) {
                p.getObject().content(null).formatted(null);
            }
        }, config);
    }
    
    public AgentMessage.AgentMessageBuilder borrowBuilder() throws Exception {
        return pool.borrowObject();
    }
    
    public void returnBuilder(AgentMessage.AgentMessageBuilder builder) {
        pool.returnObject(builder);
    }
}
```

## 并发优化

### 1. 自定义线程池

```java
public class OptimizedExecutor {
    private final ThreadPoolExecutor executor;
    private final RejectedExecutionHandler rejectionHandler;
    
    public OptimizedExecutor(int coreSize, int maxSize, int queueSize) {
        rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        
        executor = new ThreadPoolExecutor(
            coreSize,
            maxSize,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueSize),
            new ThreadFactoryBuilder()
                .setNameFormat("agent-pool-%d")
                .setDaemon(true)
                .build(),
            rejectionHandler
        );
        
        executor.prestartAllCoreThreads();
    }
    
    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }
}
```

### 2. 批量处理优化

```java
public class BatchOptimizer<T, R> {
    private final int batchSize;
    private final Duration maxWait;
    private final Queue<CompletableFuture<R>> pendingFutures;
    private final List<T> currentBatch;
    private final ScheduledExecutorService scheduler;
    
    public BatchOptimizer(int batchSize, Duration maxWait) {
        this.batchSize = batchSize;
        this.maxWait = maxWait;
        this.pendingFutures = new ConcurrentLinkedQueue<>();
        this.currentBatch = Collections.synchronizedList(new ArrayList<>());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    public CompletableFuture<R> submit(T item) {
        CompletableFuture<R> future = new CompletableFuture<>();
        pendingFutures.offer(future);
        currentBatch.add(item);
        
        if (currentBatch.size() >= batchSize) {
            processBatch();
        } else if (currentBatch.size() == 1) {
            scheduler.schedule(
                this::processBatch,
                maxWait.toMillis(),
                TimeUnit.MILLISECONDS
            );
        }
        
        return future;
    }
}
```

## 网络优化

### 1. HTTP连接池配置

```java
public class OptimizedHttpClient {
    private final OkHttpClient client;
    
    public OptimizedHttpClient(int maxIdleConnections, Duration keepAliveDuration) {
        ConnectionPool connectionPool = new ConnectionPool(
            maxIdleConnections,
            keepAliveDuration.toMillis(),
            TimeUnit.MILLISECONDS
        );
        
        client = new OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }
}
```

### 2. 请求重试策略

```java
public class RetryStrategy {
    private final int maxAttempts;
    private final Duration initialDelay;
    private final double backoffMultiplier;
    
    public <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> operation) {
        return executeWithRetry(operation, 1);
    }
    
    private <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> operation,
            int attempt) {
        return operation.get()
            .exceptionally(throwable -> {
                if (attempt >= maxAttempts) {
                    throw new CompletionException(throwable);
                }
                
                Duration delay = initialDelay.multipliedBy(
                    (long) Math.pow(backoffMultiplier, attempt - 1)
                );
                
                return CompletableFuture.delayedExecutor(
                    delay.toMillis(), TimeUnit.MILLISECONDS
                ).submit(() -> executeWithRetry(
                    operation, attempt + 1
                ).join()).join();
            });
    }
}
```

## 监控指标

### 1. 性能指标收集

```java
public class PerformanceMonitor {
    private final MeterRegistry registry;
    private final Timer requestTimer;
    private final Counter errorCounter;
    private final Gauge concurrentRequests;
    
    public PerformanceMonitor() {
        registry = new SimpleMeterRegistry();
        
        requestTimer = Timer.builder("request.duration")
            .description("Request processing time")
            .register(registry);
            
        errorCounter = Counter.builder("request.errors")
            .description("Number of request errors")
            .register(registry);
            
        concurrentRequests = Gauge.builder("requests.active", 
            new AtomicInteger(0), AtomicInteger::get)
            .description("Number of active requests")
            .register(registry);
    }
    
    public <T> CompletableFuture<T> monitorAsync(
            Supplier<CompletableFuture<T>> operation) {
        Timer.Sample sample = Timer.start();
        concurrentRequests.increment();
        
        return operation.get()
            .whenComplete((result, throwable) -> {
                concurrentRequests.decrement();
                sample.stop(requestTimer);
                
                if (throwable != null) {
                    errorCounter.increment();
                }
            });
    }
}
```

### 2. 健康检查

```java
public class HealthCheck {
    private final AsyncLlmService llmService;
    private final List<Action> actions;
    
    public CompletableFuture<Map<String, HealthStatus>> checkHealth() {
        List<CompletableFuture<HealthStatus>> checks = new ArrayList<>();
        
        // 检查LLM服务
        checks.add(checkLlmService());
        
        // 检查所有工具
        for (Action action : actions) {
            checks.add(checkAction(action));
        }
        
        return CompletableFuture.allOf(
            checks.toArray(new CompletableFuture[0])
        ).thenApply(v -> {
            Map<String, HealthStatus> results = new HashMap<>();
            // 汇总检查结果
            return results;
        });
    }
}
```

这些优化措施提供了：

1. 高效的内存管理
2. 优化的并发处理
3. 可靠的网络通信
4. 完整的监控体系

建议根据实际需求选择性应用这些优化措施。 