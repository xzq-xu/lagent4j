package io.github.internlm.lagent4j.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动作返回结果类
 * <p>
 * 用于表示一个动作执行的返回结果，包含参数、URL、类型、结果、错误信息、状态、思考过程和有效性等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionReturn {
    /**
     * 动作参数
     */
    private Map<String, Object> args;
    
    /**
     * 相关URL
     */
    private String url;
    
    /**
     * 动作类型
     */
    private String type;
    
    /**
     * 动作执行结果
     */
    private List<Map<String, String>> result;
    
    /**
     * 错误信息
     */
    private String errmsg;
    
    /**
     * 动作状态
     */
    @Builder.Default
    private ActionStatusCode state = ActionStatusCode.SUCCESS;
    
    /**
     * 思考过程
     */
    private String thought;
    
    /**
     * 动作有效性
     */
    @Builder.Default
    private ActionValidCode valid = ActionValidCode.OPEN;
    
    /**
     * 格式化结果为字符串
     * <p>
     * 将结果列表中的项目连接成一个字符串，文本类型直接使用内容，其他类型使用Markdown链接格式
     *
     * @return 格式化后的结果字符串
     */
    public String formatResult() {
        if (result == null || result.isEmpty()) {
            return "";
        }
        
        return result.stream()
                .map(item -> {
                    String type = item.get("type");
                    String content = item.get("content");
                    if ("text".equals(type)) {
                        return content;
                    } else {
                        return String.format("[%s](%s)", type, content);
                    }
                })
                .collect(Collectors.joining("\n"));
    }
} 