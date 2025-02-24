package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.Base64;

/**
 * 图像处理工具
 * 支持图像格式转换、调整大小等操作
 */
public class ImageProcessingAction implements Action {
    private final String name;

    public ImageProcessingAction(String name) {
        this.name = name;
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
                String imageData = (String) parameters.get("image");
                BufferedImage image = decodeImage(imageData);

                return switch (operation.toLowerCase()) {
                    case "resize" -> resizeImage(image, parameters);
                    case "rotate" -> rotateImage(image, parameters);
                    case "convert" -> convertFormat(image, parameters);
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

    private BufferedImage decodeImage(String imageData) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(imageData);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private String encodeImage(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private ActionResult resizeImage(BufferedImage image, Map<String, Object> parameters) throws IOException {
        int width = ((Number) parameters.get("width")).intValue();
        int height = ((Number) parameters.get("height")).intValue();
        
        BufferedImage resized = new BufferedImage(width, height, image.getType());
        resized.createGraphics()
            .drawImage(image, 0, 0, width, height, null);
        
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output(encodeImage(resized, "png"))
            .build();
    }

    private ActionResult rotateImage(BufferedImage image, Map<String, Object> parameters) throws IOException {
        double degrees = ((Number) parameters.get("degrees")).doubleValue();
        
        // 实现旋转逻辑
        BufferedImage rotated = image; // 简化示例
        
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output(encodeImage(rotated, "png"))
            .build();
    }

    private ActionResult convertFormat(BufferedImage image, Map<String, Object> parameters) throws IOException {
        String format = (String) parameters.get("format");
        
        return ActionResult.builder()
            .status(ActionResult.Status.SUCCESS)
            .output(encodeImage(image, format))
            .build();
    }
} 