package io.github.internlm.lagent4j.prompts.parsers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.internlm.lagent4j.prompts.Parser;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON解析器
 * <p>
 * 用于解析模型输出中的JSON格式数据，支持以下功能：
 * 1. 支持多种JSON标记格式（json、JSON、object等）
 * 2. 支持多个JSON块的提取和解析
 * 3. 支持嵌套的JSON结构
 * 4. 支持思考过程和JSON数据的分离
 * 5. 提供详细的错误信息和解析状态
 */
@Slf4j
public class JsonParser implements Parser {
    /**
     * JSON开始标记列表
     */
    private static final List<String> BEGIN_MARKERS = List.of(
            "```json",
            "```JSON",
            "```object",
            "{",
            "[\n{"
    );

    /**
     * JSON结束标记列表
     */
    private static final List<String> END_MARKERS = List.of(
            "```",
            "}",
            "}\n]"
    );

    /**
     * 默认JSON开始标记
     */
    private final String defaultBegin;

    /**
     * 默认JSON结束标记
     */
    private final String defaultEnd;

    /**
     * 创建一个JSON解析器
     *
     * @param begin 默认开始标记
     * @param end   默认结束标记
     */
    public JsonParser(String begin, String end) {
        this.defaultBegin = begin;
        this.defaultEnd = end;
    }

    /**
     * 创建一个JSON解析器，使用默认的开始和结束标记
     */
    public JsonParser() {
        this("```json", "```");
    }

    @Override
    public Object parseResponse(String response) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> jsonBlocks = new ArrayList<>();

        // 提取思考过程
        StringBuilder thought = new StringBuilder();
        int lastEnd = 0;

        // 查找所有JSON块
        for (String begin : BEGIN_MARKERS) {
            for (String end : END_MARKERS) {
                Pattern pattern = Pattern.compile(
                        Pattern.quote(begin) + "(.*?)" + Pattern.quote(end),
                        Pattern.DOTALL
                );
                Matcher matcher = pattern.matcher(response);

                while (matcher.find()) {
                    // 提取JSON块前的内容作为思考过程
                    int beginIndex = matcher.start();
                    if (beginIndex > lastEnd) {
                        thought.append(response.substring(lastEnd, beginIndex).trim())
                                .append("\n");
                    }
                    lastEnd = matcher.end();

                    // 提取JSON块内容
                    String jsonStr = matcher.group(1).trim();
                    if (jsonStr.startsWith("{") || jsonStr.startsWith("[")) {
                        try {
                            // 解析JSON
                            Object jsonObj = jsonStr.startsWith("[") ?
                                    JSON.parseArray(jsonStr) :
                                    JSON.parseObject(jsonStr);

                            // 将解析结果添加到JSON块列表
                            Map<String, Object> block = new HashMap<>();
                            block.put("content", jsonObj);
                            block.put("start", matcher.start());
                            block.put("end", matcher.end());
                            block.put("format", begin);
                            jsonBlocks.add(block);
                        } catch (Exception e) {
                            log.debug("Failed to parse JSON block: {}", jsonStr, e);
                        }
                    }
                }
            }
        }

        // 如果没有找到任何JSON块，尝试直接解析整个响应
        if (jsonBlocks.isEmpty()) {
            try {
                Object jsonObj = response.trim().startsWith("[") ?
                        JSON.parseArray(response) :
                        JSON.parseObject(response);

                Map<String, Object> block = new HashMap<>();
                block.put("content", jsonObj);
                block.put("start", 0);
                block.put("end", response.length());
                block.put("format", "raw");
                jsonBlocks.add(block);

                result.put("status", 1);
            } catch (Exception e) {
                // 如果直接解析也失败，则整个响应作为思考过程
                thought = new StringBuilder(response.trim());

                result.put("status", 0);
                result.put("error", "无法解析JSON：" + e.getMessage());
                log.error("Failed to parse response as JSON: {}", response, e);
            }
        } else {
            // 添加剩余的思考过程
            if (lastEnd < response.length()) {
                thought.append(response.substring(lastEnd).trim());
            }

            // 设置解析状态
            result.put("status", 1);
        }

        // 添加思考过程（如果有）
        String thoughtStr = thought.toString().trim();
        if (!thoughtStr.isEmpty()) {
            result.put("thought", thoughtStr);
        }

        // 添加JSON块（如果有）
        if (!jsonBlocks.isEmpty()) {
            if (jsonBlocks.size() == 1) {
                // 如果只有一个JSON块，直接使用其内容
                result.put("content", jsonBlocks.get(0).get("content"));
            } else {
                // 如果有多个JSON块，保留完整的块信息
                result.put("blocks", jsonBlocks);
            }
        }

        return result;
    }

    /**
     * 检查字符串是否可能是JSON
     *
     * @param str 要检查的字符串
     * @return 是否可能是JSON
     */
    private boolean isPossibleJson(String str) {
        str = str.trim();
        return (str.startsWith("{") && str.endsWith("}")) ||
                (str.startsWith("[") && str.endsWith("]"));
    }

    /**
     * 清理JSON字符串
     *
     * @param jsonStr JSON字符串
     * @return 清理后的JSON字符串
     */
    private String cleanJsonString(String jsonStr) {
        // 移除注释
        jsonStr = jsonStr.replaceAll("//.*?\\n", "\n")
                .replaceAll("/\\*.*?\\*/", "");

        // 移除多余的逗号
        jsonStr = jsonStr.replaceAll(",\\s*([}\\]])", "$1");

        // 修复常见的格式问题
        jsonStr = jsonStr.replaceAll("'", "\"")
                .replaceAll("\\\\n", "\\n")
                .replaceAll("\\\\r", "\\r")
                .replaceAll("\\\\t", "\\t");

        return jsonStr;
    }
} 