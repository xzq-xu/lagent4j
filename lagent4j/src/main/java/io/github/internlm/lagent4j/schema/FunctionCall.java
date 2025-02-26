package io.github.internlm.lagent4j.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 函数调用
 * <p>
 * 表示一个工具函数的调用信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FunctionCall {
    /**
     * 函数名称
     */
    private String name;
    
    /**
     * 函数参数
     */
    private Map<String, Object> arguments;
    
    /**
     * 检查参数是否为Map类型
     *
     * @return 如果参数是Map类型则返回true，否则返回false
     */
    public boolean isParametersMap() {
        return arguments instanceof Map;
    }
    
    /**
     * 获取参数的Map形式
     *
     * @return 参数的Map形式，如果参数不是Map类型则返回null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getParametersAsMap() {
        if (isParametersMap()) {
            return (Map<String, Object>) arguments;
        }
        return null;
    }
    
    /**
     * 获取参数的字符串形式
     *
     * @return 参数的字符串形式，如果参数不是字符串类型则返回toString()的结果
     */
    public String getParametersAsString() {
//        if (arguments instanceof String) {
//            return (String) arguments;
//        }
        return arguments != null ? arguments.toString() : null;
    }
} 