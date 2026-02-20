package com.swingmcp.demo;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class LayoutManager {

    static AppLogger log;
    private static ResourceManager resourceManager = new ResourceManager();

    private static final String LAYOUT_DIR = System.getProperty("user.home") + File.separator + ".swingmcp";
    private static final String LAYOUT_FILE = LAYOUT_DIR + File.separator + "layout.properties";

    private LayoutManager() {}

    public static void saveLayout() {
        Properties props = new Properties();

        // Save main frame
        JFrame mainFrame = resourceManager.getMainFrame();
        if (mainFrame != null) {
            boolean maximized = (mainFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
            props.setProperty("main.maximized", String.valueOf(maximized));
            // Save the non-maximized bounds
            if (!maximized) {
                props.setProperty("main.x", String.valueOf(mainFrame.getX()));
                props.setProperty("main.y", String.valueOf(mainFrame.getY()));
                props.setProperty("main.width", String.valueOf(mainFrame.getWidth()));
                props.setProperty("main.height", String.valueOf(mainFrame.getHeight()));
            }
        }

        // Save internal frames
        JDesktopPane desktop = resourceManager.getDesktopPane();
        if (desktop != null) {
            JInternalFrame[] frames = desktop.getAllFrames();
            for (int i = 0; i < frames.length; i++) {
                JInternalFrame f = frames[i];
                String name = f.getName();
                if (name == null || name.isEmpty()) {
                    continue;
                }
                String prefix = "frame." + name + ".";
                props.setProperty(prefix + "x", String.valueOf(f.getX()));
                props.setProperty(prefix + "y", String.valueOf(f.getY()));
                props.setProperty(prefix + "width", String.valueOf(f.getWidth()));
                props.setProperty(prefix + "height", String.valueOf(f.getHeight()));
                props.setProperty(prefix + "visible", String.valueOf(f.isVisible()));
                props.setProperty(prefix + "icon", String.valueOf(f.isIcon()));
                props.setProperty(prefix + "maximum", String.valueOf(f.isMaximum()));
            }
        }

        // Save tree state
        if (desktop != null) {
            JTree tree = findTreeByName(desktop, "instrumentTree");
            if (tree != null) {
                StringBuilder expanded = new StringBuilder();
                int rowCount = tree.getRowCount();
                for (int row = 0; row < rowCount; row++) {
                    TreePath path = tree.getPathForRow(row);
                    if (tree.isExpanded(path)) {
                        if (expanded.length() > 0) {
                            expanded.append("|");
                        }
                        expanded.append(treePathToString(path));
                    }
                }
                props.setProperty("tree.instrumentTree.expanded", expanded.toString());

                TreePath selPath = tree.getSelectionPath();
                if (selPath != null) {
                    props.setProperty("tree.instrumentTree.selected", treePathToString(selPath));
                }
            }
        }

        // Write to file
        File dir = new File(LAYOUT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(LAYOUT_FILE);
            props.store(out, "Swing MCP Layout");
            log.info("Layout saved to " + LAYOUT_FILE);
            resourceManager.getStatusBar().setStatusText("Layout saved.");
        } catch (IOException e) {
            log.error("Failed to save layout", e);
        } finally {
            if (out != null) {
                try { out.close(); } catch (IOException ignored) {}
            }
        }
    }

    public static void restoreMainFrame() {
        Properties props = loadProperties();
        if (props == null) {
            return;
        }

        JFrame mainFrame = resourceManager.getMainFrame();
        if (mainFrame == null) {
            return;
        }

        String xStr = props.getProperty("main.x");
        if (xStr == null) {
            return; // No saved main frame data
        }

        try {
            int x = Integer.parseInt(props.getProperty("main.x"));
            int y = Integer.parseInt(props.getProperty("main.y"));
            int w = Integer.parseInt(props.getProperty("main.width"));
            int h = Integer.parseInt(props.getProperty("main.height"));
            boolean maximized = Boolean.parseBoolean(props.getProperty("main.maximized", "false"));

            Rectangle bounds = clampToScreen(new Rectangle(x, y, w, h));
            mainFrame.setBounds(bounds);

            if (maximized) {
                mainFrame.setExtendedState(mainFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            }

            log.info("Main frame layout restored.");
        } catch (NumberFormatException e) {
            log.error("Invalid layout properties for main frame", e);
        }
    }

    public static boolean restoreInternalFrame(JInternalFrame frame) {
        Properties props = loadProperties();
        if (props == null || frame == null) {
            return false;
        }

        String name = frame.getName();
        if (name == null || name.isEmpty()) {
            return false;
        }

        String prefix = "frame." + name + ".";
        String xStr = props.getProperty(prefix + "x");
        if (xStr == null) {
            return false; // No saved data for this frame
        }

        try {
            int x = Integer.parseInt(props.getProperty(prefix + "x"));
            int y = Integer.parseInt(props.getProperty(prefix + "y"));
            int w = Integer.parseInt(props.getProperty(prefix + "width"));
            int h = Integer.parseInt(props.getProperty(prefix + "height"));
            boolean visible = Boolean.parseBoolean(props.getProperty(prefix + "visible", "true"));
            boolean icon = Boolean.parseBoolean(props.getProperty(prefix + "icon", "false"));
            boolean maximum = Boolean.parseBoolean(props.getProperty(prefix + "maximum", "false"));

            Rectangle bounds = clampToDesktop(new Rectangle(x, y, w, h));
            frame.setBounds(bounds);
            frame.setVisible(visible);
            frame.setMaximum(maximum);
            frame.setIcon(icon);

            log.info("Restored layout for internal frame: " + name);
            return true;
        } catch (Exception e) {
            log.error("Failed to restore layout for frame: " + name, e);
            return false;
        }
    }

    private static Rectangle clampToScreen(Rectangle rect) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();

        // Check if at least 50x50 overlaps any screen
        for (int i = 0; i < screens.length; i++) {
            Rectangle screenBounds = screens[i].getDefaultConfiguration().getBounds();
            Rectangle overlap = screenBounds.intersection(rect);
            if (overlap.width >= 50 && overlap.height >= 50) {
                return rect; // Sufficiently visible
            }
        }

        // Not visible enough — clamp to primary screen
        Rectangle primary = ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        int x = Math.max(primary.x, Math.min(rect.x, primary.x + primary.width - rect.width));
        int y = Math.max(primary.y, Math.min(rect.y, primary.y + primary.height - rect.height));
        int w = Math.min(rect.width, primary.width);
        int h = Math.min(rect.height, primary.height);
        return new Rectangle(x, y, w, h);
    }

    private static Rectangle clampToDesktop(Rectangle rect) {
        JDesktopPane desktop = resourceManager.getDesktopPane();
        if (desktop == null) {
            return rect;
        }

        int dw = desktop.getWidth();
        int dh = desktop.getHeight();

        // Ensure at least 50px visible within desktop
        int x = rect.x;
        int y = rect.y;
        int w = rect.width;
        int h = rect.height;

        if (x + w < 50) {
            x = 0;
        }
        if (y + h < 50) {
            y = 0;
        }
        if (x > dw - 50) {
            x = dw - w;
        }
        if (y > dh - 50) {
            y = dh - h;
        }

        return new Rectangle(x, y, w, h);
    }

    public static List getSavedQuoteSectors() {
        List sectors = new ArrayList();
        Properties props = loadProperties();
        if (props == null) {
            return sectors;
        }
        Enumeration names = props.propertyNames();
        while (names.hasMoreElements()) {
            String key = (String) names.nextElement();
            if (key.startsWith("frame.quoteFrame_") && key.endsWith(".x")) {
                String sector = key.substring("frame.quoteFrame_".length(), key.length() - 2);
                sectors.add(sector);
            }
        }
        return sectors;
    }

    private static final String[] KNOWN_SECTORS = {"Technology", "Financials", "Healthcare", "Energy", "ETFs"};

    public static List getSavedOrderSymbols() {
        List symbols = new ArrayList();
        Properties props = loadProperties();
        if (props == null) {
            return symbols;
        }
        Enumeration names = props.propertyNames();
        while (names.hasMoreElements()) {
            String key = (String) names.nextElement();
            if (key.startsWith("frame.orderFrame_") && key.endsWith(".x")) {
                String symbol = key.substring("frame.orderFrame_".length(), key.length() - 2);
                // Only include if we can find quote data for it (i.e., it's a real symbol, not an orderId)
                boolean found = false;
                for (int i = 0; i < KNOWN_SECTORS.length; i++) {
                    if (QuotePanel.findQuoteData(KNOWN_SECTORS[i], symbol) != null) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    symbols.add(symbol);
                }
            }
        }
        return symbols;
    }

    public static String findSectorForSymbol(String symbol) {
        for (int i = 0; i < KNOWN_SECTORS.length; i++) {
            if (QuotePanel.findQuoteData(KNOWN_SECTORS[i], symbol) != null) {
                return KNOWN_SECTORS[i];
            }
        }
        return null;
    }

    public static void restoreTreeState() {
        Properties props = loadProperties();
        if (props == null) {
            return;
        }

        JDesktopPane desktop = resourceManager.getDesktopPane();
        if (desktop == null) {
            return;
        }

        JTree tree = findTreeByName(desktop, "instrumentTree");
        if (tree == null) {
            return;
        }

        String expandedStr = props.getProperty("tree.instrumentTree.expanded");
        if (expandedStr != null && !expandedStr.isEmpty()) {
            // First collapse all
            int rowCount = tree.getRowCount();
            for (int row = rowCount - 1; row >= 0; row--) {
                tree.collapseRow(row);
            }

            // Expand saved paths
            String[] paths = expandedStr.split("\\|");
            for (int i = 0; i < paths.length; i++) {
                TreePath treePath = stringToTreePath(tree, paths[i]);
                if (treePath != null) {
                    tree.expandPath(treePath);
                }
            }
        }

        String selectedStr = props.getProperty("tree.instrumentTree.selected");
        if (selectedStr != null && !selectedStr.isEmpty()) {
            TreePath selPath = stringToTreePath(tree, selectedStr);
            if (selPath != null) {
                tree.setSelectionPath(selPath);
                tree.scrollPathToVisible(selPath);
            }
        }

        log.info("Restored tree state for instrumentTree.");
    }

    private static JTree findTreeByName(Container container, String name) {
        Component[] children = container.getComponents();
        for (int i = 0; i < children.length; i++) {
            Component child = children[i];
            if (child instanceof JTree && name.equals(child.getName())) {
                return (JTree) child;
            }
            if (child instanceof Container) {
                JTree found = findTreeByName((Container) child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String treePathToString(TreePath path) {
        StringBuilder sb = new StringBuilder();
        Object[] nodes = path.getPath();
        for (int i = 0; i < nodes.length; i++) {
            if (i > 0) {
                sb.append(" > ");
            }
            sb.append(nodes[i].toString());
        }
        return sb.toString();
    }

    private static TreePath stringToTreePath(JTree tree, String pathStr) {
        String[] parts = pathStr.split(" > ");
        TreeModel model = tree.getModel();
        Object current = model.getRoot();

        if (!current.toString().equals(parts[0])) {
            return null;
        }

        List<Object> pathNodes = new ArrayList<Object>();
        pathNodes.add(current);

        for (int i = 1; i < parts.length; i++) {
            boolean found = false;
            int childCount = model.getChildCount(current);
            for (int c = 0; c < childCount; c++) {
                Object child = model.getChild(current, c);
                if (child.toString().equals(parts[i])) {
                    pathNodes.add(child);
                    current = child;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }

        return new TreePath(pathNodes.toArray());
    }

    private static Properties loadProperties() {
        File file = new File(LAYOUT_FILE);
        if (!file.exists()) {
            return null;
        }

        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
            return props;
        } catch (IOException e) {
            log.error("Failed to load layout properties", e);
            return null;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
        }
    }
}
