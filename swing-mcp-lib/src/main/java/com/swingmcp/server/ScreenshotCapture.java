package com.swingmcp.server;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Captures screenshots of Swing components using {@link Robot}.
 *
 * <p>Supports capturing the entire main frame or a specific component's
 * screen bounds. Returns images as PNG byte arrays or Base64-encoded strings.</p>
 */
public class ScreenshotCapture {

    /**
     * Capture a screenshot of a component or the main frame.
     *
     * @param componentNameOrId optional component to capture; null for main frame
     * @return a result containing the image data, width, and height
     * @throws Exception if capture fails
     */
    public CaptureResult capture(String componentNameOrId) throws Exception {
        Component target;

        if (componentNameOrId != null && !componentNameOrId.isEmpty()) {
            target = ComponentTreeWalker.findComponent(componentNameOrId);
            if (target == null) {
                throw new IllegalArgumentException("Component not found: " + componentNameOrId);
            }
        } else {
            // Find the main JFrame
            target = findMainFrame();
            if (target == null) {
                throw new IllegalStateException("No visible JFrame found");
            }
        }

        Point loc = target.getLocationOnScreen();
        Dimension size = target.getSize();
        Rectangle rect = new Rectangle(loc.x, loc.y, size.width, size.height);

        Robot robot = new Robot();
        BufferedImage image = robot.createScreenCapture(rect);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        return new CaptureResult(pngBytes, image.getWidth(), image.getHeight());
    }

    private Component findMainFrame() {
        for (Window window : Window.getWindows()) {
            if (window instanceof JFrame && window.isShowing()) {
                return window;
            }
        }
        return null;
    }

    /**
     * Result of a screenshot capture containing raw PNG bytes and dimensions.
     */
    public static class CaptureResult {
        private final byte[] pngBytes;
        private final int width;
        private final int height;

        /**
         * Create a capture result.
         *
         * @param pngBytes the raw PNG image bytes
         * @param width    the image width in pixels
         * @param height   the image height in pixels
         */
        public CaptureResult(byte[] pngBytes, int width, int height) {
            this.pngBytes = pngBytes;
            this.width = width;
            this.height = height;
        }

        /**
         * Get the raw PNG bytes.
         *
         * @return PNG byte array
         */
        public byte[] getPngBytes() {
            return pngBytes;
        }

        /**
         * Get the image width.
         *
         * @return width in pixels
         */
        public int getWidth() {
            return width;
        }

        /**
         * Get the image height.
         *
         * @return height in pixels
         */
        public int getHeight() {
            return height;
        }

        /**
         * Get the image as a Base64-encoded string.
         *
         * @return Base64 string
         */
        public String toBase64() {
            return Base64.getEncoder().encodeToString(pngBytes);
        }
    }
}
