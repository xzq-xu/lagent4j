package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import com.example.lagent4j.constants.ToolConstants;
import com.example.lagent4j.exceptions.ToolException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FileIOAction implements Action {
    private final String name;
    private final Path rootPath;

    public FileIOAction(String name, String rootPath) {
        this.name = name;
        this.rootPath = Paths.get(rootPath);
        
        try {
            Files.createDirectories(this.rootPath);
        } catch (Exception e) {
            throw new ToolException("Failed to create root directory: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String operation = (String) parameters.get(ToolConstants.OPERATION_KEY);
                String relativePath = (String) parameters.get(ToolConstants.PATH_KEY);
                Path fullPath = rootPath.resolve(relativePath).normalize();
                
                // 安全检查：确保路径在根目录下
                if (!fullPath.startsWith(rootPath)) {
                    throw new ToolException("Access denied: Path outside root directory");
                }
                
                switch (operation.toLowerCase()) {
                    case "read":
                        return read(fullPath);
                    case "write":
                        String content = (String) parameters.get(ToolConstants.CONTENT_KEY);
                        return write(fullPath, content);
                    case "delete":
                        return delete(fullPath);
                    default:
                        throw new ToolException("Unsupported operation: " + operation);
                }
            } catch (Exception e) {
                return ActionResult.builder()
                    .status(ActionResult.Status.FAILURE)
                    .error(e.getMessage())
                    .build();
            }
        });
    }

    private ActionResult read(Path path) throws Exception {
        String content = Files.readString(path);
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output(content)
            .build();
    }

    private ActionResult write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output("File written successfully")
            .build();
    }

    private ActionResult delete(Path path) throws Exception {
        Files.deleteIfExists(path);
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output("File deleted successfully")
            .build();
    }
}
