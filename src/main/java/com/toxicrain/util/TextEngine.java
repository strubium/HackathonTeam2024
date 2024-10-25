package com.toxicrain.util;

import com.toxicrain.core.Color;
import com.toxicrain.texture.TextureInfo;
import com.toxicrain.core.render.BatchRenderer;
import com.toxicrain.texture.TextureSystem;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * The TextEngine class provides a way to render "text" on the screen
 */
public class TextEngine {
    private static final float TEXT_SCALE = 1.2f; // Scale factor for text rendering
    private final Font font;
    private final float transparency;

    // Cache for text and textures
    private Map<String, TextureInfo> textureCache;
    private Map<String, BufferedImage> imageCache;  // Cache for BufferedImages

    public TextEngine(Font font, float transparency) {
        this.font = font;
        this.transparency = transparency;
        this.textureCache = new HashMap<>();  // Initialize the texture cache
        this.imageCache = new HashMap<>();     // Initialize the image cache
    }

    public void render(BatchRenderer batchRenderer, String toWrite, int xOffset, int yOffset) {
        // Trim the text to avoid caching issues with leading/trailing spaces
        String trimmedText = toWrite.trim();

        // Check if the text is in the image cache
        BufferedImage textImage = imageCache.get(trimmedText);
        TextureInfo textureInfo;

        if (textImage == null) {
            // Create a new BufferedImage if not cached
            textImage = createTextImage(trimmedText);
            // Cache the created BufferedImage
            imageCache.put(trimmedText, textImage);
        }

        // Check if the texture info is in the cache
        textureInfo = textureCache.get(trimmedText);
        if (textureInfo == null) {
            // Convert the BufferedImage to a TextureInfo
            textureInfo = convertToTextureInfo(textImage);
            // Cache the texture for this text
            textureCache.put(trimmedText, textureInfo);
        }

        // Render the texture using BatchRenderer
        batchRenderer.addTexture(
                textureInfo,
                xOffset,  // X coordinate based on adjusted origin
                yOffset,   // Y coordinate based on adjusted origin
                TEXT_SCALE,
                0,  // Rotation (assuming 0 for no rotation)
                1.0f, // Scale for X
                1.0f, // Scale for Y
                com.toxicrain.core.Color.toFloatArray(transparency, Color.WHITE)  // Applying color/transparency
        );
    }

    private BufferedImage createTextImage(String text) {
        // Create a temporary image to measure text size
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImage.createGraphics();
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics();

        // Calculate width and height of the text
        int width = metrics.stringWidth(text);
        int height = metrics.getHeight();

        // Create the final image with the calculated width and height
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();
        g2d.setFont(font);
        g2d.drawString(text, 0, metrics.getAscent()); // Draw the text at the top left of the image
        g2d.dispose();

        return flipImageVertically(image);
    }

    private TextureInfo convertToTextureInfo(BufferedImage image) {
        // Save BufferedImage to a temporary file
        try {
            File tempFile = File.createTempFile("textTexture", ".png");
            ImageIO.write(image, "png", tempFile);

            // Load the texture using your existing loadTexture method
            TextureInfo textureInfo = TextureSystem.loadTexture(tempFile.getAbsolutePath());

            // Optionally delete the temporary file
            tempFile.deleteOnExit(); // Mark for deletion when the JVM exits

            return textureInfo;
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert BufferedImage to TextureInfo", e);
        }
    }

    private BufferedImage flipImageVertically(BufferedImage image) {
        BufferedImage flipped = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = flipped.createGraphics();
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), 0, image.getHeight(), image.getWidth(), 0, null);
        g.dispose();
        return flipped;
    }
}
