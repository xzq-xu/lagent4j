package com.example.lagent4j.actions;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.example.lagent4j.agent.ActionResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FileIOAction implements Action {

    @Override
    public String getName() {
        return "fileAction";
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            JSONObject params = JSONObject.from(parameters);
            String operation = params.getString("operation");
            String path = params.getString("path");
            String content = params.getString("content");

            try {
                switch (operation) {
                    case "read":
                        return ActionResult.builder().status(ActionResult.Status.SUCCESS)
                                .output(FileUtil.readUtf8String(path)).build();
                    case "write":
                        FileUtil.writeUtf8String(content, path);
                        return ActionResult.builder().status(ActionResult.Status.SUCCESS)
                                .output( "{\"message\":\"Write successful\"}").build();
                    case "mkdir":
                        FileUtil.mkdir(path);
                        return ActionResult.builder().status(ActionResult.Status.SUCCESS)
                                .output( "{\"message\":\"Directory created\"}").build() ;
                    default:
                        return ActionResult.builder().status(ActionResult.Status.FAILURE)
                                .error("{\"error\":\"Unsupported operation\"}").build();
                }
            } catch (Exception e) {
                return ActionResult.builder().status(ActionResult.Status.FAILURE)
                        .error("{\"error\":\"" + e.getMessage() + "\"}").build();
            }
        });
    }
}
