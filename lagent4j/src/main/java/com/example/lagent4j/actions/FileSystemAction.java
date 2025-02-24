package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 文件系统操作工具
 * 支持文件读写、目录操作等
 */
public class FileSystemAction implements Action {
    private final String name;
    private final Path rootPath;

    public FileSystemAction(String name, String rootPath) {
        this.name = name;
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String operation = (String) parameters.get("operation");
                String path = (String) parameters.get("path");
                Path targetPath = validatePath(path);

                return switch (operation.toLowerCase()) {
                    case "read" -> readFile(targetPath);
                    case "write" -> writeFile(targetPath, (String) parameters.get("content"));
                    case "delete" -> deleteFile(targetPath);
                    case "list" -> listDirectory(targetPath);
                    case "mkdir" -> createDirectory(targetPath);
                    case "exists" -> checkExists(targetPath);
                    default -> ActionResult.builder()
                        .status(ActionResult.Status.FAILURE)
                        .error("Unsupported operation: " + operation)
                        .build();
                };
            } catch (Exception e) {
                return ActionResult.builder()
                    .status(ActionResult.Status.FAILURE)
                    .error(e.getMessage())
                    .build();
            }
        });
    }

    private Path validatePath(String path) {
        Path normalizedPath = rootPath.resolve(path).normalize();
        if (!normalizedPath.startsWith(rootPath)) {
            throw new SecurityException("Access denied: " + path);
        }
        return normalizedPath;
    }

    private ActionResult readFile(Path path) throws Exception {
        String content = Files.readString(path);
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output(content)
            .build();
    }

    private ActionResult writeFile(Path path, String content) throws Exception {
        Files.writeString(path, content, StandardOpenOption.CREATE, 
            StandardOpenOption.TRUNCATE_EXISTING);
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output("File written successfully")
            .build();
    }

    private ActionResult deleteFile(Path path) throws Exception {
        Files.delete(path);
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output("File deleted successfully")
            .build();
    }

    private ActionResult listDirectory(Path path) throws Exception {
        String files = Files.list(path)
            .map(p -> rootPath.relativize(p).toString())
            .collect(Collectors.joining("\n"));
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output(files)
            .build();
    }

    private ActionResult createDirectory(Path path) throws Exception {
        Files.createDirectories(path);
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output("Directory created successfully")
            .build();
    }

    private ActionResult checkExists(Path path) {
        boolean exists = Files.exists(path);
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output(Boolean.toString(exists))
            .build();
    }
} 