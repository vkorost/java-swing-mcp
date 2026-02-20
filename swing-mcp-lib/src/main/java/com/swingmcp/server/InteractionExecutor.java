package com.swingmcp.server;

import com.swingmcp.server.model.ActionRequest;
import com.swingmcp.server.model.ActionResult;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;

/**
 * Executes user interaction actions on Swing components.
 *
 * <p>Supports clicking, typing, selecting items in combo boxes, tables,
 * and trees, expanding/collapsing tree nodes, and navigating menus.
 * All actions are expected to be called from the EDT.</p>
 */
public class InteractionExecutor {

    private final ComponentStateExtractor stateExtractor = new ComponentStateExtractor();

    /**
     * Execute an action and return the result with updated component state.
     *
     * @param request the action request
     * @return the action result
     */
    public ActionResult execute(ActionRequest request) {
        String target = request.getTarget();
        String action = request.getAction();

        // For menu actions, target is the path
        if ("menu".equals(action)) {
            return executeMenuAction(request);
        }

        Component comp = ComponentTreeWalker.findComponent(target);
        if (comp == null) {
            return ActionResult.failure(action, target, "Component not found: " + target);
        }

        try {
            switch (action) {
                case "click":
                    doClick(comp, request.getRow(), request.getPath());
                    break;
                case "double_click":
                    doDoubleClick(comp, request.getRow(), request.getPath());
                    break;
                case "right_click":
                    doRightClick(comp);
                    break;
                case "type":
                    doType(comp, request.getText());
                    break;
                case "clear":
                    doClear(comp);
                    break;
                case "select_combo":
                    doSelectCombo(comp, request.getValue(), request.getIndex());
                    break;
                case "select_row":
                    doSelectRow(comp, request.getRow());
                    break;
                case "select_tree":
                    doSelectTree(comp, request.getPath());
                    break;
                case "expand_tree":
                    doExpandTree(comp, request.getPath());
                    break;
                case "collapse_tree":
                    doCollapseTree(comp, request.getPath());
                    break;
                case "check":
                    doCheck(comp);
                    break;
                case "uncheck":
                    doUncheck(comp);
                    break;
                default:
                    return ActionResult.failure(action, target, "Unknown action: " + action);
            }

            // Wait for UI to settle
            Thread.sleep(100);

            Map<String, Object> state = stateExtractor.extractState(comp, null);
            return ActionResult.success(action, target, state);
        } catch (Exception e) {
            return ActionResult.failure(action, target, e.getMessage());
        }
    }

    private void doClick(Component comp, Integer row, String path) {
        if (comp instanceof AbstractButton) {
            ((AbstractButton) comp).doClick();
        } else {
            robotClick(comp, false, 1, row, path);
        }
    }

    private void doDoubleClick(Component comp, Integer row, String path) {
        robotClick(comp, false, 2, row, path);
    }

    private void doRightClick(Component comp) {
        robotClick(comp, true, 1, null, null);
    }

    private void robotClick(Component comp, boolean rightButton, int clickCount, Integer row, String path) {
        try {
            Point loc = comp.getLocationOnScreen();
            int clickX = loc.x + comp.getWidth() / 2;
            int clickY = loc.y + comp.getHeight() / 2;

            // For JTable with a row parameter, click at the center of that row
            if (comp instanceof JTable && row != null && row >= 0) {
                JTable table = (JTable) comp;
                if (row < table.getRowCount()) {
                    Rectangle cellRect = table.getCellRect(row, 0, true);
                    clickX = loc.x + cellRect.x + cellRect.width / 2;
                    clickY = loc.y + cellRect.y + cellRect.height / 2;
                }
            }

            // For JTree with a path parameter, click at the center of that tree row
            if (comp instanceof JTree && path != null && !path.isEmpty()) {
                JTree tree = (JTree) comp;
                TreePath treePath = findTreePath(tree, path);
                if (treePath != null) {
                    tree.scrollPathToVisible(treePath);
                    int treeRow = tree.getRowForPath(treePath);
                    if (treeRow >= 0) {
                        Rectangle rowBounds = tree.getRowBounds(treeRow);
                        if (rowBounds != null) {
                            clickX = loc.x + rowBounds.x + rowBounds.width / 2;
                            clickY = loc.y + rowBounds.y + rowBounds.height / 2;
                        }
                    }
                }
            }

            Robot robot = new Robot();
            robot.mouseMove(clickX, clickY);
            int button = rightButton ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK;
            for (int i = 0; i < clickCount; i++) {
                robot.mousePress(button);
                robot.mouseRelease(button);
                robot.delay(50);
            }
        } catch (AWTException e) {
            throw new RuntimeException("Robot click failed: " + e.getMessage(), e);
        }
    }

    private void doType(Component comp, String text) {
        if (comp instanceof javax.swing.text.JTextComponent) {
            javax.swing.text.JTextComponent textComp = (javax.swing.text.JTextComponent) comp;
            textComp.requestFocusInWindow();
            textComp.setText(text);
        } else {
            comp.requestFocusInWindow();
            try {
                Robot robot = new Robot();
                for (char c : text.toCharArray()) {
                    int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
                    if (keyCode != KeyEvent.VK_UNDEFINED) {
                        robot.keyPress(keyCode);
                        robot.keyRelease(keyCode);
                        robot.delay(20);
                    }
                }
            } catch (AWTException e) {
                throw new RuntimeException("Robot type failed: " + e.getMessage(), e);
            }
        }
    }

    private void doClear(Component comp) {
        if (comp instanceof javax.swing.text.JTextComponent) {
            ((javax.swing.text.JTextComponent) comp).setText("");
        }
    }

    @SuppressWarnings("unchecked")
    private void doSelectCombo(Component comp, String value, Integer index) {
        if (!(comp instanceof JComboBox)) {
            throw new IllegalArgumentException("Component is not a JComboBox: " + comp.getClass().getSimpleName());
        }
        JComboBox<?> combo = (JComboBox<?>) comp;
        if (value != null) {
            ((JComboBox<Object>) combo).setSelectedItem(value);
        } else if (index != null) {
            combo.setSelectedIndex(index);
        }
    }

    private void doSelectRow(Component comp, Integer row) {
        if (!(comp instanceof JTable)) {
            throw new IllegalArgumentException("Component is not a JTable: " + comp.getClass().getSimpleName());
        }
        JTable table = (JTable) comp;
        if (row != null && row >= 0 && row < table.getRowCount()) {
            table.setRowSelectionInterval(row, row);
        }
    }

    private void doSelectTree(Component comp, String path) {
        if (!(comp instanceof JTree)) {
            throw new IllegalArgumentException("Component is not a JTree: " + comp.getClass().getSimpleName());
        }
        JTree tree = (JTree) comp;
        TreePath treePath = findTreePath(tree, path);
        if (treePath != null) {
            tree.setSelectionPath(treePath);
            tree.scrollPathToVisible(treePath);
        } else {
            throw new IllegalArgumentException("Tree path not found: " + path);
        }
    }

    private void doExpandTree(Component comp, String path) {
        if (!(comp instanceof JTree)) {
            throw new IllegalArgumentException("Component is not a JTree");
        }
        JTree tree = (JTree) comp;
        TreePath treePath = findTreePath(tree, path);
        if (treePath != null) {
            tree.expandPath(treePath);
        }
    }

    private void doCollapseTree(Component comp, String path) {
        if (!(comp instanceof JTree)) {
            throw new IllegalArgumentException("Component is not a JTree");
        }
        JTree tree = (JTree) comp;
        TreePath treePath = findTreePath(tree, path);
        if (treePath != null) {
            tree.collapsePath(treePath);
        }
    }

    private void doCheck(Component comp) {
        if (comp instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) comp;
            if (!button.isSelected()) {
                button.doClick();
            }
        }
    }

    private void doUncheck(Component comp) {
        if (comp instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) comp;
            if (button.isSelected()) {
                button.doClick();
            }
        }
    }

    private ActionResult executeMenuAction(ActionRequest request) {
        String path = request.getPath();
        if (path == null || path.isEmpty()) {
            return ActionResult.failure("menu", null, "Menu path is required");
        }

        String[] parts = path.split("\\s*>\\s*");
        if (parts.length < 2) {
            return ActionResult.failure("menu", path, "Menu path must have at least two parts (e.g., 'File > Exit')");
        }

        // Find the JMenuBar
        JMenuBar menuBar = null;
        for (Window window : Window.getWindows()) {
            if (window instanceof JFrame) {
                JFrame frame = (JFrame) window;
                if (frame.getJMenuBar() != null) {
                    menuBar = frame.getJMenuBar();
                    break;
                }
            }
        }
        if (menuBar == null) {
            return ActionResult.failure("menu", path, "No menu bar found");
        }

        // Find the top-level menu
        JMenu topMenu = null;
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null && parts[0].trim().equals(menu.getText())) {
                topMenu = menu;
                break;
            }
        }
        if (topMenu == null) {
            return ActionResult.failure("menu", path, "Menu not found: " + parts[0]);
        }

        // Navigate submenu path
        JMenuItem target = navigateMenu(topMenu, parts, 1);
        if (target == null) {
            return ActionResult.failure("menu", path, "Menu item not found: " + path);
        }

        target.doClick();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}

        Map<String, Object> state = stateExtractor.extractState(target, null);
        return ActionResult.success("menu", path, state);
    }

    private JMenuItem navigateMenu(JMenu menu, String[] parts, int index) {
        if (index >= parts.length) return menu;

        String targetText = parts[index].trim();
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null && targetText.equals(item.getText())) {
                if (index == parts.length - 1) {
                    return item;
                } else if (item instanceof JMenu) {
                    return navigateMenu((JMenu) item, parts, index + 1);
                }
            }
        }
        return null;
    }

    /**
     * Find a TreePath matching a string path like "Portfolio > Equities > AAPL".
     *
     * @param tree the JTree
     * @param path the path string with " > " separators
     * @return the TreePath, or null if not found
     */
    static TreePath findTreePath(JTree tree, String path) {
        String[] parts = path.split("\\s*>\\s*");
        TreeModel model = tree.getModel();
        Object root = model.getRoot();

        if (root == null || !root.toString().equals(parts[0].trim())) {
            return null;
        }

        TreePath treePath = new TreePath(root);
        Object current = root;

        for (int i = 1; i < parts.length; i++) {
            String target = parts[i].trim();
            boolean found = false;
            for (int j = 0; j < model.getChildCount(current); j++) {
                Object child = model.getChild(current, j);
                if (child.toString().equals(target)) {
                    treePath = treePath.pathByAddingChild(child);
                    current = child;
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }

        return treePath;
    }
}
