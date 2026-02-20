package com.swingmcp.server;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Checks text-bearing Swing components for WCAG 2.1 contrast ratio compliance.
 *
 * <p>Uses the relative luminance formula from WCAG 2.1 to calculate contrast
 * ratios between foreground (text) and background colors. Reports components
 * that fail AA and AAA thresholds.</p>
 */
public class ContrastChecker {

    /** WCAG AA minimum contrast ratio for normal text. */
    private static final double WCAG_AA_NORMAL = 4.5;
    /** WCAG AA minimum contrast ratio for large text. */
    private static final double WCAG_AA_LARGE = 3.0;
    /** WCAG AAA minimum contrast ratio for normal text. */
    private static final double WCAG_AAA_NORMAL = 7.0;
    /** WCAG AAA minimum contrast ratio for large text. */
    private static final double WCAG_AAA_LARGE = 4.5;

    /**
     * Check all text-bearing components for contrast issues.
     *
     * @return a map containing "issues", "totalChecked", and "totalIssues"
     */
    public Map<String, Object> checkAll() {
        List<Map<String, Object>> issues = new ArrayList<>();
        int totalChecked = 0;

        for (Window window : Window.getWindows()) {
            if (window.isShowing()) {
                totalChecked += checkComponent(window, issues);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("issues", issues);
        result.put("totalChecked", totalChecked);
        result.put("totalIssues", issues.size());
        return result;
    }

    private int checkComponent(Component comp, List<Map<String, Object>> issues) {
        int checked = 0;
        String text = getTextIfApplicable(comp);

        if (text != null && !text.isEmpty()) {
            checked = 1;
            Color fg = comp.getForeground();
            Color bg = getEffectiveBackground(comp);

            if (fg != null && bg != null) {
                double ratio = contrastRatio(fg, bg);
                boolean passAA = ratio >= WCAG_AA_NORMAL;
                boolean passAAA = ratio >= WCAG_AAA_NORMAL;

                if (!passAA) {
                    Map<String, Object> issue = new LinkedHashMap<>();
                    if (comp instanceof JComponent) {
                        JComponent jc = (JComponent) comp;
                        Object id = jc.getClientProperty("swingmcp.id");
                        if (id instanceof Integer) issue.put("id", id);
                    }
                    issue.put("type", comp.getClass().getSimpleName());
                    issue.put("name", comp.getName());
                    issue.put("text", text.length() > 50 ? text.substring(0, 50) + "..." : text);
                    issue.put("foreground", ComponentStateExtractor.colorToHex(fg));
                    issue.put("background", ComponentStateExtractor.colorToHex(bg));
                    issue.put("contrastRatio", Math.round(ratio * 100.0) / 100.0);
                    issue.put("wcagAA", passAA);
                    issue.put("wcagAAA", passAAA);
                    issue.put("minimumRequired", WCAG_AA_NORMAL);
                    issues.add(issue);
                }
            }
        }

        if (comp instanceof Container) {
            Container container = (Container) comp;
            for (int i = 0; i < container.getComponentCount(); i++) {
                checked += checkComponent(container.getComponent(i), issues);
            }
        }

        // Also check JMenuBar items
        if (comp instanceof JFrame) {
            JFrame frame = (JFrame) comp;
            if (frame.getJMenuBar() != null) {
                checked += checkComponent(frame.getJMenuBar(), issues);
            }
        }

        return checked;
    }

    private String getTextIfApplicable(Component comp) {
        if (comp instanceof JLabel) return ((JLabel) comp).getText();
        if (comp instanceof AbstractButton) return ((AbstractButton) comp).getText();
        if (comp instanceof javax.swing.text.JTextComponent) return ((javax.swing.text.JTextComponent) comp).getText();
        if (comp instanceof JMenuItem) return ((JMenuItem) comp).getText();
        return null;
    }

    /**
     * Walk up the parent chain to find the first opaque background color.
     *
     * @param comp the component
     * @return the effective background color
     */
    static Color getEffectiveBackground(Component comp) {
        Component current = comp;
        while (current != null) {
            if (current instanceof JComponent) {
                JComponent jc = (JComponent) current;
                if (jc.isOpaque() && jc.getBackground() != null) {
                    return jc.getBackground();
                }
            } else if (current.getBackground() != null) {
                return current.getBackground();
            }
            current = current.getParent();
        }
        return comp.getBackground();
    }

    /**
     * Calculate the WCAG 2.1 contrast ratio between two colors.
     *
     * @param c1 first color
     * @param c2 second color
     * @return the contrast ratio (1.0 to 21.0)
     */
    static double contrastRatio(Color c1, Color c2) {
        double l1 = relativeLuminance(c1);
        double l2 = relativeLuminance(c2);

        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);

        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * Calculate the relative luminance of a color per WCAG 2.1.
     *
     * @param color the color
     * @return relative luminance (0.0 to 1.0)
     */
    static double relativeLuminance(Color color) {
        double r = linearize(color.getRed() / 255.0);
        double g = linearize(color.getGreen() / 255.0);
        double b = linearize(color.getBlue() / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linearize(double value) {
        return (value <= 0.04045)
                ? value / 12.92
                : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
