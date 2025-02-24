package com.example.lagent4j.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent注册表
 * 用于管理多个Agent实例
 */
public class AgentRegistry {
    private static final AgentRegistry INSTANCE = new AgentRegistry();
    private final Map<String, AsyncAgent> agents = new ConcurrentHashMap<>();

    private AgentRegistry() {}

    public static AgentRegistry getInstance() {
        return INSTANCE;
    }

    public void register(String name, AsyncAgent agent) {
        agents.put(name, agent);
    }

    public AsyncAgent getAgent(String name) {
        return agents.get(name);
    }

    public void unregister(String name) {
        agents.remove(name);
    }

    public void clear() {
        agents.clear();
    }
} 