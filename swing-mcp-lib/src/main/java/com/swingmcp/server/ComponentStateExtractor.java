package com.swingmcp.server;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.tree.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Extracts detailed state from individual Swing components.
 *
 * <p>Produces a {@code Map<String, Object>} with type-specific fields
 * (e.g., table data for JTable, tree nodes for JTree, items for JComboBox)
 * plus common fields shared by all component types.</p>
 */
public class ComponentStateExtractor {

    /**
     * Extract the full state of a component as a map.
     *
     * @param comp     the component to extract state from
     * @param rowRange optional row range for tables (e.g., "0-9"), null for default
     * @return a map of state key-value pairs
     */
    public Map<String, Object> extractState(Component comp, String rowRange) {
        Map<String, Object> state = new LinkedHashMap<>();

        // Common fields
        addCommonFields(state, comp);

        // Type-specific fields
        String simpleType = comp.getClass().getSimpleName();
        switch (simpleType) {
            case "JTable":
                addTableState(state, (JTable) comp, rowRange);
                break;
            case "JTree":
                addTreeState(state, (JTree) comp);
                break;
            case "JComboBox":
                addComboState(state, (JComboBox<?>) comp);
                break;
            case "JTextField":
            case "JTextArea":
            case "JEditorPane":
            case "JPasswordField":
                addTextFieldState(state, (JTextComponent) comp);
                break;
            case "JButton":
                addButtonState(state, (AbstractButton) comp);
                break;
            case "JCheckBox":
            case "JToggleButton":
            case "JRadioButton":
                addCheckboxState(state, (AbstractButton) comp);
                break;
            case "JLabel":
                addLabelState(state, (JLabel) comp);
                break;
            case "JMenuBar":
                addMenuBarState(state, (JMenuBar) comp);
                break;
            case "JMenu":
            case "JMenuItem":
                addMenuItemState(state, (JMenuItem) comp);
                break;
            default: {
                String text = ComponentTreeWalker.extractText(comp, simpleType);
                if (text != null) state.put("text", text);
                break;
            }
        }

        return state;
    }

    private void addCommonFields(Map<String, Object> state, Component comp) {
        if (comp instanceof JComponent) {
            JComponent jc = (JComponent) comp;
            Object id = jc.getClientProperty("swingmcp.id");
            if (id instanceof Integer) {
                state.put("id", id);
            }
        }
        state.put("type", comp.getClass().getSimpleName());
        state.put("name", comp.getName());

        Rectangle bounds = comp.getBounds();
        Map<String, Integer> boundsMap = new LinkedHashMap<>();
        boundsMap.put("x", bounds.x);
        boundsMap.put("y", bounds.y);
        boundsMap.put("width", bounds.width);
        boundsMap.put("height", bounds.height);
        state.put("bounds", boundsMap);

        try {
            Point screenLoc = comp.getLocationOnScreen();
            Map<String, Integer> screenBoundsMap = new LinkedHashMap<>();
            screenBoundsMap.put("x", screenLoc.x);
            screenBoundsMap.put("y", screenLoc.y);
            screenBoundsMap.put("width", bounds.width);
            screenBoundsMap.put("height", bounds.height);
            state.put("screenBounds", screenBoundsMap);
        } catch (Exception e) {
            state.put("screenBounds", state.get("bounds"));
        }

        state.put("visible", comp.isVisible());
        state.put("enabled", comp.isEnabled());
        state.put("focused", comp.isFocusOwner());
        state.put("foreground", colorToHex(comp.getForeground()));
        state.put("background", colorToHex(comp.getBackground()));
    }

    private void addTableState(Map<String, Object> state, JTable table, String rowRange) {
        state.put("rowCount", table.getRowCount());
        state.put("columnCount", table.getColumnCount());

        List<String> columns = new ArrayList<>();
        for (int i = 0; i < table.getColumnCount(); i++) {
            columns.add(table.getColumnName(i));
        }
        state.put("columns", columns);

        // Selected rows/columns
        List<Integer> selectedRows = new ArrayList<>();
        for (int r : table.getSelectedRows()) selectedRows.add(r);
        state.put("selectedRows", selectedRows);

        List<Integer> selectedColumns = new ArrayList<>();
        for (int c : table.getSelectedColumns()) selectedColumns.add(c);
        state.put("selectedColumns", selectedColumns);

        state.put("sortColumn", null);
        state.put("sortOrder", null);

        // Data with pagination
        int startRow = 0;
        int endRow = Math.min(table.getRowCount() - 1, 49); // Default: first 50 rows

        if (rowRange != null && rowRange.contains("-")) {
            String[] parts = rowRange.split("-");
            startRow = Integer.parseInt(parts[0]);
            endRow = Math.min(Integer.parseInt(parts[1]), table.getRowCount() - 1);
        }

        List<List<Object>> data = new ArrayList<>();
        for (int r = startRow; r <= endRow && r < table.getRowCount(); r++) {
            List<Object> row = new ArrayList<>();
            for (int c = 0; c < table.getColumnCount(); c++) {
                row.add(table.getValueAt(r, c));
            }
            data.add(row);
        }
        state.put("data", data);
        state.put("dataRange", new int[]{startRow, endRow});
        state.put("totalRows", table.getRowCount());
        state.put("hasMore", endRow < table.getRowCount() - 1);
    }

    private void addTreeState(Map<String, Object> state, JTree tree) {
        // Selected path
        TreePath selPath = tree.getSelectionPath();
        state.put("selectedPath", selPath != null ? treePathToString(selPath) : null);

        // Expanded paths
        List<String> expandedPaths = new ArrayList<>();
        for (int i = 0; i < tree.getRowCount(); i++) {
            TreePath path = tree.getPathForRow(i);
            if (tree.isExpanded(path)) {
                expandedPaths.add(treePathToString(path));
            }
        }
        state.put("expandedPaths", expandedPaths);

        // All nodes
        List<Map<String, Object>> nodes = new ArrayList<>();
        TreeModel model = tree.getModel();
        if (model.getRoot() != null) {
            collectTreeNodes(model, model.getRoot(), new TreePath(model.getRoot()), nodes);
        }
        state.put("nodes", nodes);
    }

    private void collectTreeNodes(TreeModel model, Object node, TreePath path,
                                   List<Map<String, Object>> nodes) {
        int childCount = model.getChildCount(node);
        boolean isLeaf = model.isLeaf(node);

        Map<String, Object> nodeMap = new LinkedHashMap<>();
        nodeMap.put("path", treePathToString(path));
        nodeMap.put("leaf", isLeaf);
        nodeMap.put("children", childCount);
        nodes.add(nodeMap);

        for (int i = 0; i < childCount; i++) {
            Object child = model.getChild(node, i);
            TreePath childPath = path.pathByAddingChild(child);
            collectTreeNodes(model, child, childPath, nodes);
        }
    }

    private String treePathToString(TreePath path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.getPathCount(); i++) {
            if (i > 0) sb.append(" > ");
            sb.append(path.getPathComponent(i).toString());
        }
        return sb.toString();
    }

    private void addComboState(Map<String, Object> state, JComboBox<?> combo) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < combo.getItemCount(); i++) {
            items.add(String.valueOf(combo.getItemAt(i)));
        }
        state.put("items", items);
        state.put("selectedItem", String.valueOf(combo.getSelectedItem()));
        state.put("selectedIndex", combo.getSelectedIndex());
        state.put("editable", combo.isEditable());
        state.put("popupVisible", combo.isPopupVisible());
    }

    private void addTextFieldState(Map<String, Object> state, JTextComponent textComp) {
        state.put("text", textComp.getText());
        state.put("editable", textComp.isEditable());
        state.put("caretPosition", textComp.getCaretPosition());
        state.put("selectionStart", textComp.getSelectionStart());
        state.put("selectionEnd", textComp.getSelectionEnd());
        if (textComp instanceof JTextField) {
            state.put("columns", ((JTextField) textComp).getColumns());
        }
    }

    private void addButtonState(Map<String, Object> state, AbstractButton button) {
        state.put("text", button.getText());
    }

    private void addCheckboxState(Map<String, Object> state, AbstractButton button) {
        state.put("text", button.getText());
        state.put("selected", button.isSelected());
    }

    private void addLabelState(Map<String, Object> state, JLabel label) {
        state.put("text", label.getText());
    }

    private void addMenuBarState(Map<String, Object> state, JMenuBar menuBar) {
        List<String> menus = new ArrayList<>();
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null) menus.add(menu.getText());
        }
        state.put("menus", menus);
    }

    private void addMenuItemState(Map<String, Object> state, JMenuItem item) {
        state.put("text", item.getText());
        if (item instanceof JMenu) {
            JMenu menu = (JMenu) item;
            List<String> items = new ArrayList<>();
            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem mi = menu.getItem(i);
                if (mi != null) items.add(mi.getText());
            }
            state.put("items", items);
        }
    }

    /**
     * Convert a Color to a hex string like "#RRGGBB".
     *
     * @param color the color
     * @return hex string representation
     */
    static String colorToHex(Color color) {
        if (color == null) return null;
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
