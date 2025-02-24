package com.example.lagent4j.agent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LRUMemory implements Memory {
    private final int maxSize;
    private final Map<Long, LinkedList<AgentMessage>> sessions;

    public LRUMemory(int maxSize) {
        this.maxSize = maxSize;
        this.sessions = new ConcurrentHashMap<>();
    }

    @Override
    public List<AgentMessage> get(long sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new LinkedList<>());
    }

    @Override
    public void add(long sessionId, AgentMessage message) {
        LinkedList<AgentMessage> history = sessions.computeIfAbsent(sessionId, k -> new LinkedList<>());
        history.add(message);
        while (history.size() > maxSize) {
            history.removeFirst();
        }
    }

    @Override
    public void clear(long sessionId) {
        sessions.remove(sessionId);
    }
}
