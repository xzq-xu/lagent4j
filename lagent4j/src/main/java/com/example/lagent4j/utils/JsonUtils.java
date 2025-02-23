package com.example.lagent4j.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

public class JsonUtils {
    public static JSONObject parseObject(String json) {
        return JSON.parseObject(json);
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    public static String toJsonString(Object object) {
        return JSON.toJSONString(object, JSONWriter.Feature.PrettyFormat);
    }
}
