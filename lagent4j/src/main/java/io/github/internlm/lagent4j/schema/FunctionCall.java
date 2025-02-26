package io.github.internlm.lagent4j.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 函数调用类
 * <p>
 * 用于表示一个函数调用，包含函数名和参数
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
     * 函数参数，可以是Map或字符串形式
     */
    private Object parameters;
    
    /**
     * 检查参数是否为Map类型
     *
     * @return 如果参数是Map类型则返回true，否则返回false
     */
    public boolean isParametersMap() {
        return parameters instanceof Map;
    }
    
    /**
     * 获取参数的Map形式
     *
     * @return 参数的Map形式，如果参数不是Map类型则返回null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getParametersAsMap() {
        if (isParametersMap()) {
            return (Map<String, Object>) parameters;
        }
        return null;
    }
    
    /**
     * 获取参数的字符串形式
     *
     * @return 参数的字符串形式，如果参数不是字符串类型则返回toString()的结果
     */
    public String getParametersAsString() {
        if (parameters instanceof String) {
            return (String) parameters;
        }
        return parameters != null ? parameters.toString() : null;
    }
} 