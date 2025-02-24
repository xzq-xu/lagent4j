package com.example.lagent4j.actions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class ToolRegistry {
    private final Map<String, Action> tools = new ConcurrentHashMap<>();
    
    public void register(Action tool) {
        tools.put(tool.getName(), tool);
    }
    
    public void unregister(String name) {
        tools.remove(name);
    }
    
    public Action getTool(String name) {
        return tools.get(name);
    }
    
    public List<Action> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    public void clear() {
        tools.clear();
    }
} 