package com.example.lagent4j.prompts;

public class DefaultToolParser implements OutputFormat {
    private final String toolType;
    private final String begin;
    private final String end;

    public DefaultToolParser(String toolType, String begin, String end) {
        this.toolType = toolType;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public String getToolType() {
        return toolType;
    }

    @Override
    public String getBeginPattern() {
        return begin;
    }

    @Override
    public String getEndPattern() {
        return end;
    }

    @Override
    public Object parseResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        int start = response.indexOf(begin);
        if (start == -1) {
            return null;
        }

        int end = response.indexOf(this.end, start + begin.length());
        if (end == -1) {
            return null;
        }

        return response.substring(start + begin.length(), end).trim();
    }

    @Override
    public boolean validate(String content) {
        return content != null && !content.isEmpty();
    }
} 