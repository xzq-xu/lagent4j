package com.example.lagent4j.monitoring;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 指标注册表
 * 用于收集和管理系统运行时指标
 */
public class MetricsRegistry {
    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Timer> timers;
    private final ConcurrentHashMap<String, Counter> counters;
    private final ConcurrentHashMap<String, AtomicInteger> gauges;

    public MetricsRegistry() {
        this.registry = new SimpleMeterRegistry();
        this.timers = new ConcurrentHashMap<>();
        this.counters = new ConcurrentHashMap<>();
        this.gauges = new ConcurrentHashMap<>();
        
        // 初始化基础指标
        initializeMetrics();
    }

    private void initializeMetrics() {
        // 请求计数器
        createCounter("requests.total", "Total number of requests");
        createCounter("requests.success", "Successful requests");
        createCounter("requests.failed", "Failed requests");
        
        // 响应时间
        createTimer("response.time", "Response time");
        
        // 并发请求数
        createGauge("requests.active", "Active requests");
        
        // 工具使用计数
        createCounter("tools.usage", "Tool usage count");
        createCounter("tools.errors", "Tool error count");
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample, String name) {
        Timer timer = timers.computeIfAbsent(name,
            k -> Timer.builder(k)
                .description("Operation timing")
                .register(registry));
        sample.stop(timer);
    }

    public void incrementCounter(String name) {
        counters.computeIfAbsent(name,
            k -> Counter.builder(k)
                .description("Operation counter")
                .register(registry))
            .increment();
    }

    public void setGauge(String name, int value) {
        gauges.computeIfAbsent(name,
            k -> registry.gauge(k, new AtomicInteger(0)))
            .set(value);
    }

    public void recordOperation(String operation, Duration duration, boolean success) {
        // 记录总请求数
        incrementCounter("requests.total");
        
        // 记录成功/失败
        if (success) {
            incrementCounter("requests.success");
        } else {
            incrementCounter("requests.failed");
        }
        
        // 记录响应时间
        Timer timer = timers.computeIfAbsent("response.time",
            k -> Timer.builder(k)
                .description("Response time")
                .register(registry));
        timer.record(duration);
    }

    public void recordToolUsage(String toolName, boolean success) {
        // 记录工具使用
        Counter usageCounter = Counter.builder("tools.usage")
            .tag("tool", toolName)
            .description("Tool usage count")
            .register(registry);
        usageCounter.increment();
        
        // 记录错误
        if (!success) {
            Counter errorCounter = Counter.builder("tools.errors")
                .tag("tool", toolName)
                .description("Tool error count")
                .register(registry);
            errorCounter.increment();
        }
    }

    public MeterRegistry getRegistry() {
        return registry;
    }

    private Timer createTimer(String name, String description) {
        return Timer.builder(name)
            .description(description)
            .register(registry);
    }

    private Counter createCounter(String name, String description) {
        return Counter.builder(name)
            .description(description)
            .register(registry);
    }

    private AtomicInteger createGauge(String name, String description) {
        return registry.gauge(name, new AtomicInteger(0));
    }
} 