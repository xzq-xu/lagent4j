package com.example.lagent4j.agents.aggregator;

import java.util.List;
import java.util.Map;

public interface MessageAggregator {
    List<Map<String, String>> aggregateMessages(long sessionId);
} 