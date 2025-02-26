package io.github.internlm.lagent4j.utils;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * 对象创建工具类
 * <p>
 * 用于根据配置创建对象实例
 */
public class ObjectCreator {
    /**
     * 创建对象
     *
     * @param config 对象配置，可以是对象实例或包含类型信息的Map
     * @param <T>    对象类型
     * @return 创建的对象实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T createObject(Object config) {
        if (config == null) {
            return null;
        }
        
        // 如果已经是目标类型的实例，直接返回
        if (!(config instanceof Map)) {
            return (T) config;
        }
        
        // 处理配置Map
        Map<String, Object> configMap = (Map<String, Object>) config;
        if (!configMap.containsKey("type")) {
            return null;
        }
        
        String className = configMap.get("type").toString();
        try {
            // 加载类
            Class<?> clazz = Class.forName(className);
            
            // 查找合适的构造函数
            Constructor<?>[] constructors = clazz.getConstructors();
            for (Constructor<?> constructor : constructors) {
                try {
                    // 尝试使用无参构造函数
                    if (constructor.getParameterCount() == 0) {
                        return (T) constructor.newInstance();
                    }
                    
                    // 尝试使用带Map参数的构造函数
                    if (constructor.getParameterCount() == 1 && 
                        Map.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
                        return (T) constructor.newInstance(configMap);
                    }
                } catch (Exception e) {
                    // 忽略构造函数调用异常，尝试下一个构造函数
                }
            }
            
            // 如果没有找到合适的构造函数，尝试使用默认构造函数
            return (T) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create object of type " + className, e);
        }
    }
} 