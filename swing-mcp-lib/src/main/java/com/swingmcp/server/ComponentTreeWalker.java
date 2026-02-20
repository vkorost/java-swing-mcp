package com.swingmcp.server;

import com.swingmcp.server.model.ComponentNode;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Recursively walks the Swing component tree and produces a flat list of
 * {@link ComponentNode} DTOs suitable for JSON serialization.
 *
 * <p>Each component is assigned a stable integer ID stored via
 * {@code putClientProperty("swingmcp.id", id)}. IDs persist across calls
 * so that external clients can reference components reliably.</p>
 */
public class ComponentTreeWalker {

    private static final String ID_PROPERTY = "swingmcp.id";
    private static final AtomicInteger nextId = new AtomicInteger(1);

    /** Component types to skip unless they have an explicit name set. */
    private static final Set<String> SKIP_TYPES = new HashSet<>(Arrays.asList(
            "JPanel", "JLayeredPane", "JRootPane", "CellRendererPane", "JViewport"
    ));

    /** Component types considered interactive. */
    private static final Set<String> INTERACTABLE_TYPES = new HashSet<>(Arrays.asList(
            "JButton", "JToggleButton", "JCheckBox", "JRadioButton",
            "JTextField", "JTextArea", "JEditorPane", "JPasswordField",
            "JComboBox", "JTable", "JTree", "JList",
            "JMenu", "JMenuItem", "JCheckBoxMenuItem", "JRadioButtonMenuItem",
            "JMenuBar", "JSpinner", "JSlider", "JTabbedPane"
    ));

    /**
     * Walk all visible Swing windows and return a flat component list.
     *
     * @param maxDepth  maximum traversal depth (null for unlimited)
     * @param types     filter to these component types (null for all)
     * @param interactableOnly if true, return only interactive components
     * @return flat list of component nodes
     */
    public List<ComponentNode> walk(Integer maxDepth, Set<String> types, boolean interactableOnly) {
        List<ComponentNode> nodes = new ArrayList<>();
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window instanceof JFrame) {
                walkComponent(window, null, 0, maxDepth, types, interactableOnly, nodes);
            }
        }
        // Truncate to 200 components max
        if (nodes.size() > 200) {
            return new ArrayList<>(nodes.subList(0, 200));
        }
        return nodes;
    }

    /**
     * Get the total count of components across all visible windows.
     *
     * @return total component count
     */
    public int getTotalComponentCount() {
        int count = 0;
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window instanceof JFrame) {
                count += countComponents(window);
            }
        }
        return count;
    }

    private int countComponents(Component comp) {
        int count = 1;
        if (comp instanceof Container) {
            Container container = (Container) comp;
            for (int i = 0; i < container.getComponentCount(); i++) {
                count += countComponents(container.getComponent(i));
            }
        }
        return count;
    }

    private void walkComponent(Component comp, Integer parentId, int depth,
                                Integer maxDepth, Set<String> types,
                                boolean interactableOnly, List<ComponentNode> nodes) {
        if (maxDepth != null && depth > maxDepth) return;
        if (!comp.isVisible()) return;

        String simpleType = comp.getClass().getSimpleName();
        int id = assignId(comp);

        boolean shouldInclude = shouldInclude(comp, simpleType, types, interactableOnly);

        if (shouldInclude) {
            ComponentNode node = buildNode(comp, id, parentId, simpleType);
            nodes.add(node);
        }

        // Always traverse children even if this node is skipped
        if (comp instanceof Container) {
            Container container = (Container) comp;
            Integer effectiveParentId = shouldInclude ? Integer.valueOf(id) : parentId;
            for (int i = 0; i < container.getComponentCount(); i++) {
                Component child = container.getComponent(i);
                walkComponent(child, effectiveParentId, depth + 1, maxDepth, types, interactableOnly, nodes);
            }
        }

        // Also traverse JMenuBar for JFrame
        if (comp instanceof JFrame) {
            JFrame frame = (JFrame) comp;
            if (frame.getJMenuBar() != null) {
                Integer effectiveParentId = shouldInclude ? Integer.valueOf(id) : parentId;
                walkComponent(frame.getJMenuBar(), effectiveParentId, depth + 1, maxDepth, types, interactableOnly, nodes);
            }
        }
    }

    private boolean shouldInclude(Component comp, String simpleType, Set<String> types, boolean interactableOnly) {
        // Type filter
        if (types != null && !types.contains(simpleType)) {
            return false;
        }

        // Interactable filter
        if (interactableOnly && !INTERACTABLE_TYPES.contains(simpleType)) {
            return false;
        }

        // Skip unnamed layout containers
        if (SKIP_TYPES.contains(simpleType) && (comp.getName() == null || comp.getName().isEmpty())) {
            return false;
        }

        // Skip JScrollPane internal components without names
        if (comp instanceof JScrollPane && (comp.getName() == null || comp.getName().isEmpty())) {
            return false;
        }

        return true;
    }

    private ComponentNode buildNode(Component comp, int id, Integer parentId, String simpleType) {
        ComponentNode node = new ComponentNode();
        node.setId(id);
        node.setType(simpleType);
        node.setName(comp.getName());
        node.setParent(parentId);
        node.setVisible(comp.isVisible());
        node.setEnabled(comp.isEnabled());
        node.setFocused(comp.isFocusOwner());

        // Bounds
        Rectangle bounds = comp.getBounds();
        Map<String, Integer> boundsMap = new LinkedHashMap<>();
        boundsMap.put("x", bounds.x);
        boundsMap.put("y", bounds.y);
        boundsMap.put("width", bounds.width);
        boundsMap.put("height", bounds.height);
        node.setBounds(boundsMap);

        // Screen bounds
        try {
            Point screenLoc = comp.getLocationOnScreen();
            Map<String, Integer> screenBoundsMap = new LinkedHashMap<>();
            screenBoundsMap.put("x", screenLoc.x);
            screenBoundsMap.put("y", screenLoc.y);
            screenBoundsMap.put("width", bounds.width);
            screenBoundsMap.put("height", bounds.height);
            node.setScreenBounds(screenBoundsMap);
        } catch (Exception e) {
            node.setScreenBounds(node.getBounds());
        }

        // Text extraction
        String text = extractText(comp, simpleType);
        if (text != null && text.length() > 100) {
            text = text.substring(0, 100);
        }
        node.setText(text);

        // Accessible role
        try {
            AccessibleContext ac = comp.getAccessibleContext();
            if (ac != null && ac.getAccessibleRole() != null) {
                node.setAccessibleRole(ac.getAccessibleRole().toString());
            }
        } catch (Exception ignored) {}

        // Child count
        if (comp instanceof Container) {
            node.setChildCount(((Container) comp).getComponentCount());
        }

        return node;
    }

    /**
     * Extract display text from a component based on its type.
     *
     * @param comp       the component
     * @param simpleType the simple class name
     * @return the extracted text, or null
     */
    static String extractText(Component comp, String simpleType) {
        switch (simpleType) {
            case "JFrame":
                return ((JFrame) comp).getTitle();
            case "JDialog":
                return ((JDialog) comp).getTitle();
            case "JInternalFrame":
                return ((JInternalFrame) comp).getTitle();
            case "JButton":
            case "JToggleButton":
            case "JCheckBox":
            case "JRadioButton":
                return ((AbstractButton) comp).getText();
            case "JLabel":
                return ((JLabel) comp).getText();
            case "JTextField":
            case "JTextArea":
            case "JEditorPane":
            case "JPasswordField": {
                String t = ((javax.swing.text.JTextComponent) comp).getText();
                return (t != null && t.length() > 500) ? t.substring(0, 500) : t;
            }
            case "JComboBox":
                return String.valueOf(((JComboBox<?>) comp).getSelectedItem());
            case "JTable": {
                JTable table = (JTable) comp;
                return "[rows=" + table.getRowCount() + ", cols=" + table.getColumnCount() + "]";
            }
            case "JTree": {
                JTree tree = (JTree) comp;
                return "[rows=" + tree.getRowCount() + "]";
            }
            case "JMenuBar":
                return "[menus=" + ((JMenuBar) comp).getMenuCount() + "]";
            case "JMenu":
            case "JMenuItem":
            case "JCheckBoxMenuItem":
            case "JRadioButtonMenuItem":
                return ((JMenuItem) comp).getText();
            default:
                try {
                    AccessibleContext ac = comp.getAccessibleContext();
                    return (ac != null) ? ac.getAccessibleName() : null;
                } catch (Exception e) {
                    return null;
                }
        }
    }

    /**
     * Assign a stable ID to a component, reusing any existing ID.
     *
     * @param comp the component
     * @return the assigned or existing ID
     */
    static int assignId(Component comp) {
        if (comp instanceof JComponent) {
            JComponent jc = (JComponent) comp;
            Object existing = jc.getClientProperty(ID_PROPERTY);
            if (existing instanceof Integer) {
                return (Integer) existing;
            }
            int id = nextId.getAndIncrement();
            jc.putClientProperty(ID_PROPERTY, id);
            return id;
        }
        // For non-JComponent (Window, etc.), use identity hash as stable ID
        return System.identityHashCode(comp);
    }

    /**
     * Find a component by name or numeric ID across all windows.
     *
     * @param nameOrId component name (string) or ID (numeric string)
     * @return the found component, or null
     */
    public static Component findComponent(String nameOrId) {
        boolean isNumeric = nameOrId.matches("\\d+");

        for (Window window : Window.getWindows()) {
            if (!window.isShowing()) continue;
            Component found = searchComponent(window, nameOrId, isNumeric);
            if (found != null) return found;

            // Also search JMenuBar
            if (window instanceof JFrame) {
                JFrame frame = (JFrame) window;
                if (frame.getJMenuBar() != null) {
                    found = searchComponent(frame.getJMenuBar(), nameOrId, isNumeric);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private static Component searchComponent(Component comp, String nameOrId, boolean isNumeric) {
        if (isNumeric) {
            int targetId = Integer.parseInt(nameOrId);
            if (comp instanceof JComponent) {
                JComponent jc = (JComponent) comp;
                Object id = jc.getClientProperty(ID_PROPERTY);
                if (id instanceof Integer && (Integer) id == targetId) {
                    return comp;
                }
            }
        } else {
            if (nameOrId.equals(comp.getName())) {
                return comp;
            }
        }

        if (comp instanceof Container) {
            Container container = (Container) comp;
            for (int i = 0; i < container.getComponentCount(); i++) {
                Component found = searchComponent(container.getComponent(i), nameOrId, isNumeric);
                if (found != null) return found;
            }
        }
        return null;
    }
}
