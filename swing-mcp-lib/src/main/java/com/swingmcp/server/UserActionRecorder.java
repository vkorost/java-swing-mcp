package com.swingmcp.server;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Objects;

/**
 * Records user interactions with the Swing UI for later replay.
 *
 * <p>Uses {@link AWTEventListener} to capture mouse clicks and focus changes,
 * plus {@link ItemListener} on combo boxes. Produces a timestamped Markdown
 * file with an action table and replay curl commands.</p>
 */
public class UserActionRecorder {

    /** A single recorded user action. */
    public static class RecordedAction {
        public final long timestamp;
        public final String action;
        public final String targetName;
        public final String targetType;
        public final Map<String, String> params;

        public RecordedAction(long timestamp, String action, String targetName,
                              String targetType, Map<String, String> params) {
            this.timestamp = timestamp;
            this.action = action;
            this.targetName = targetName;
            this.targetType = targetType;
            this.params = params != null ? params : new LinkedHashMap<String, String>();
        }
    }

    private final List<RecordedAction> actions = new ArrayList<RecordedAction>();
    private volatile boolean recording = false;
    private long startTime;
    private long stopTime;

    private final Map<Component, String> lastTextValues =
            new IdentityHashMap<Component, String>();
    private final Set<JComboBox> trackedCombos =
            Collections.newSetFromMap(new IdentityHashMap<JComboBox, Boolean>());

    private AWTEventListener mouseAndFocusListener;
    private AWTEventListener windowListener;

    public synchronized boolean isRecording() {
        return recording;
    }

    public synchronized int getActionCount() {
        return actions.size();
    }

    public synchronized List<RecordedAction> getActions() {
        return new ArrayList<RecordedAction>(actions);
    }

    /**
     * Start recording user actions. Registers global AWT event listeners
     * and snapshots current text field / combo box state.
     */
    public synchronized void startRecording() {
        if (recording) return;

        actions.clear();
        lastTextValues.clear();
        trackedCombos.clear();
        startTime = System.currentTimeMillis();
        recording = true;

        // Must initialise listeners on EDT to access Swing components
        if (SwingUtilities.isEventDispatchThread()) {
            initListeners();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        initListeners();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("[Recorder] Recording started");
    }

    /**
     * Stop recording. Flushes any pending text-field changes, writes the
     * Markdown file and removes all listeners.
     *
     * @return the filename of the Markdown file, or null on error
     */
    public synchronized String stopRecording() {
        if (!recording) return null;
        recording = false;
        stopTime = System.currentTimeMillis();

        // Remove global listeners
        if (mouseAndFocusListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(mouseAndFocusListener);
            mouseAndFocusListener = null;
        }
        if (windowListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(windowListener);
            windowListener = null;
        }

        // Flush text fields that still have focus
        if (SwingUtilities.isEventDispatchThread()) {
            flushPendingTextChanges();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        flushPendingTextChanges();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Remove redundant single-clicks that precede double-clicks
        int before = actions.size();
        List<RecordedAction> deduped = deduplicateActions(actions);
        actions.clear();
        actions.addAll(deduped);
        System.out.println("[Recorder] Deduplicated: " + before + " -> " + actions.size());

        String filename = writeMarkdownFile();
        System.out.println("[Recorder] Recording stopped. " + actions.size()
                + " actions saved to " + filename);
        return filename;
    }

    // ---------------------------------------------------------------
    // Listener initialisation
    // ---------------------------------------------------------------

    private void initListeners() {
        // Snapshot existing text fields and attach combo listeners
        for (Window window : Window.getWindows()) {
            scanComponents(window);
        }

        // Global mouse + focus listener
        mouseAndFocusListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (!recording) return;
                try {
                    if (event instanceof MouseEvent) {
                        handleMouseEvent((MouseEvent) event);
                    } else if (event instanceof FocusEvent) {
                        handleFocusEvent((FocusEvent) event);
                    }
                } catch (Exception e) {
                    // Never let recorder errors break the application
                    e.printStackTrace();
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(mouseAndFocusListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK);

        // Watch for new windows AND new containers (JInternalFrames added to
        // a JDesktopPane don't fire WINDOW_OPENED, so we also listen for
        // COMPONENT_ADDED events on containers).
        windowListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (!recording) return;
                if (event instanceof WindowEvent
                        && ((WindowEvent) event).getID() == WindowEvent.WINDOW_OPENED) {
                    final Window window = ((WindowEvent) event).getWindow();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            scanComponents(window);
                        }
                    });
                } else if (event instanceof ContainerEvent
                        && ((ContainerEvent) event).getID() == ContainerEvent.COMPONENT_ADDED) {
                    final Component child = ((ContainerEvent) event).getChild();
                    if (child instanceof JInternalFrame) {
                        // Delay scan so the internal frame's children are fully added
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                scanComponents((Container) child);
                            }
                        });
                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(windowListener,
                AWTEvent.WINDOW_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);
    }

    @SuppressWarnings("unchecked")
    private void scanComponents(Container container) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component child = container.getComponent(i);

            // Snapshot text fields
            if (child instanceof JTextComponent) {
                lastTextValues.put(child, ((JTextComponent) child).getText());
            }

            // Attach ItemListener to combo boxes we haven't seen yet
            if (child instanceof JComboBox && !trackedCombos.contains(child)) {
                final JComboBox combo = (JComboBox) child;
                trackedCombos.add(combo);
                combo.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        if (!recording) return;
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            String name = resolveComponentName(combo);
                            Map<String, String> params = new LinkedHashMap<String, String>();
                            params.put("value", String.valueOf(combo.getSelectedItem()));
                            recordAction("select_combo", name, "JComboBox", params);
                        }
                    }
                });
            }

            if (child instanceof Container) {
                scanComponents((Container) child);
            }
        }

        // JMenuBar is not always reachable through Container.getComponent()
        if (container instanceof JFrame) {
            JMenuBar menuBar = ((JFrame) container).getJMenuBar();
            if (menuBar != null) {
                scanComponents(menuBar);
            }
        }
    }

    // ---------------------------------------------------------------
    // Event handlers
    // ---------------------------------------------------------------

    private void handleMouseEvent(MouseEvent e) {
        if (e.getID() != MouseEvent.MOUSE_CLICKED) return;
        if (e.getClickCount() > 2) return;

        Component source = e.getComponent();
        if (source == null) return;

        // Combo popup selections are handled by ItemListener
        if (isInsideComboPopup(source)) return;

        Component target = findMeaningfulComponent(source);
        if (target == null) return;

        String name = resolveComponentName(target);
        String type = target.getClass().getSimpleName();
        boolean isDouble = e.getClickCount() == 2;
        boolean isRight = SwingUtilities.isRightMouseButton(e);

        // -- JMenu (just opens popup, skip) --
        if (target instanceof JMenu) {
            return;
        }

        // -- JMenuItem (including JCheckBoxMenuItem, JRadioButtonMenuItem) --
        if (target instanceof JMenuItem) {
            String menuPath = getMenuPath((JMenuItem) target);
            Map<String, String> params = new LinkedHashMap<String, String>();
            params.put("path", menuPath);
            recordAction("menu", null, "JMenuItem", params);
            return;
        }

        // -- Buttons, checkboxes, radio buttons --
        if (target instanceof AbstractButton) {
            Map<String, String> params = new LinkedHashMap<String, String>();
            String text = ((AbstractButton) target).getText();
            if (text != null && !text.isEmpty()) {
                params.put("text", text);
            }
            if (target instanceof JCheckBox) {
                params.put("selected", String.valueOf(((JCheckBox) target).isSelected()));
            }
            recordAction("click", name, type, params);
            return;
        }

        // -- JTable --
        if (target instanceof JTable) {
            JTable table = (JTable) target;
            Point p = SwingUtilities.convertPoint(source, e.getPoint(), table);
            int row = table.rowAtPoint(p);
            int col = table.columnAtPoint(p);
            String actionName = isDouble ? "double_click"
                    : (isRight ? "right_click" : "click");
            Map<String, String> params = new LinkedHashMap<String, String>();
            if (row >= 0) params.put("row", String.valueOf(row));
            if (col >= 0) params.put("column", String.valueOf(col));
            recordAction(actionName, name, type, params);
            return;
        }

        // -- JTree --
        if (target instanceof JTree) {
            JTree tree = (JTree) target;
            Point p = SwingUtilities.convertPoint(source, e.getPoint(), tree);
            int treeRow = tree.getRowForLocation(p.x, p.y);
            TreePath path = treeRow >= 0 ? tree.getPathForRow(treeRow) : null;
            String actionName = isDouble ? "double_click" : "click";
            Map<String, String> params = new LinkedHashMap<String, String>();
            if (path != null) {
                params.put("path", treePathToString(path));
            }
            recordAction(actionName, name, type, params);
            return;
        }

        // -- JComboBox body click (opens popup, handled by ItemListener) --
        if (target instanceof JComboBox) {
            return;
        }

        // -- JTabbedPane --
        if (target instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) target;
            Point p = SwingUtilities.convertPoint(source, e.getPoint(), tabs);
            int tabIndex = tabs.indexAtLocation(p.x, p.y);
            Map<String, String> params = new LinkedHashMap<String, String>();
            if (tabIndex >= 0) {
                params.put("tab", tabs.getTitleAt(tabIndex));
                params.put("index", String.valueOf(tabIndex));
            }
            recordAction("click", name, type, params);
            return;
        }

        // -- Generic component --
        String actionName = isDouble ? "double_click"
                : (isRight ? "right_click" : "click");
        recordAction(actionName, name, type, new LinkedHashMap<String, String>());
    }

    private void handleFocusEvent(FocusEvent e) {
        Component source = e.getComponent();
        if (!(source instanceof JTextComponent)) return;

        if (e.getID() == FocusEvent.FOCUS_GAINED) {
            // Snapshot current text so we can detect changes on focus-lost
            lastTextValues.put(source, ((JTextComponent) source).getText());
        } else if (e.getID() == FocusEvent.FOCUS_LOST) {
            checkTextChange(source);
        }
    }

    private void checkTextChange(Component source) {
        if (!(source instanceof JTextComponent)) return;

        JTextComponent tc = (JTextComponent) source;
        String oldText = lastTextValues.get(source);
        String newText = tc.getText();

        if (oldText != null && oldText.equals(newText)) return;

        String name = resolveComponentName(source);
        String type = source.getClass().getSimpleName();
        Map<String, String> params = new LinkedHashMap<String, String>();

        if (newText == null || newText.isEmpty()) {
            recordAction("clear", name, type, params);
        } else {
            params.put("text", newText);
            recordAction("type", name, type, params);
        }

        lastTextValues.put(source, newText);
    }

    private void flushPendingTextChanges() {
        Component focused = KeyboardFocusManager
                .getCurrentKeyboardFocusManager().getFocusOwner();
        if (focused instanceof JTextComponent) {
            checkTextChange(focused);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private boolean isInsideComboPopup(Component c) {
        Component current = c;
        while (current != null) {
            if (current instanceof JPopupMenu) {
                Component invoker = ((JPopupMenu) current).getInvoker();
                if (invoker instanceof JComboBox) return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Walk up from the event source to find the nearest interactive component.
     */
    private Component findMeaningfulComponent(Component c) {
        Component current = c;
        while (current != null) {
            if (current instanceof JMenuItem
                    || current instanceof AbstractButton
                    || current instanceof JTextComponent
                    || current instanceof JTable
                    || current instanceof JTree
                    || current instanceof JComboBox
                    || current instanceof JList
                    || current instanceof JTabbedPane
                    || current instanceof JSlider
                    || current instanceof JSpinner) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private String resolveComponentName(Component c) {
        if (c == null) return null;
        String name = c.getName();
        if (name != null && !name.isEmpty()) return name;
        if (c instanceof JComponent) {
            Object id = ((JComponent) c).getClientProperty("swingmcp.id");
            if (id != null) return String.valueOf(id);
        }
        return null;
    }

    private String treePathToString(TreePath path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.getPathCount(); i++) {
            if (i > 0) sb.append(" > ");
            sb.append(path.getPathComponent(i).toString());
        }
        return sb.toString();
    }

    private String getMenuPath(JMenuItem item) {
        List<String> parts = new ArrayList<String>();
        parts.add(item.getText());
        Component parent = item.getParent();
        while (parent != null) {
            if (parent instanceof JPopupMenu) {
                Component invoker = ((JPopupMenu) parent).getInvoker();
                if (invoker instanceof JMenu) {
                    parts.add(((JMenu) invoker).getText());
                    parent = invoker.getParent();
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        Collections.reverse(parts);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(" > ");
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Deduplication
    // ---------------------------------------------------------------

    /**
     * Remove single/right-click events that are immediately followed by a
     * double-click on the same target within 500ms.  These phantom clicks
     * are an artefact of AWT dispatching {@code clickCount=1} before
     * {@code clickCount=2} for every double-click gesture.
     */
    private List<RecordedAction> deduplicateActions(List<RecordedAction> raw) {
        List<RecordedAction> result = new ArrayList<RecordedAction>();
        for (int i = 0; i < raw.size(); i++) {
            RecordedAction current = raw.get(i);

            // Only consider deduplicating click / right_click
            if ("click".equals(current.action) || "right_click".equals(current.action)) {
                // Look ahead for a double_click on the same target within 500ms
                if (i + 1 < raw.size()) {
                    RecordedAction next = raw.get(i + 1);
                    if ("double_click".equals(next.action)
                            && sameTargetAndParams(current, next)
                            && (next.timestamp - current.timestamp) < 500) {
                        continue; // skip this phantom click
                    }
                }
            }
            result.add(current);
        }
        return result;
    }

    private boolean sameTargetAndParams(RecordedAction a, RecordedAction b) {
        if (!Objects.equals(a.targetName, b.targetName)) return false;
        if (!Objects.equals(a.params.get("row"), b.params.get("row"))) return false;
        if (!Objects.equals(a.params.get("path"), b.params.get("path"))) return false;
        return true;
    }

    // ---------------------------------------------------------------
    // Recording
    // ---------------------------------------------------------------

    private synchronized void recordAction(String action, String targetName,
                                           String targetType,
                                           Map<String, String> params) {
        RecordedAction ra = new RecordedAction(
                System.currentTimeMillis(), action, targetName, targetType, params);
        actions.add(ra);

        StringBuilder sb = new StringBuilder("[Recorder] ");
        sb.append(action);
        if (targetName != null) sb.append(" -> ").append(targetName);
        sb.append(" (").append(targetType).append(")");
        if (params != null && !params.isEmpty()) sb.append(" ").append(params);
        System.out.println(sb.toString());
    }

    // ---------------------------------------------------------------
    // Markdown output
    // ---------------------------------------------------------------

    private String writeMarkdownFile() {
        SimpleDateFormat fileFmt = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        SimpleDateFormat readableFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss.SSS");

        String filename = "swing-mcp-server-user-actions-"
                + fileFmt.format(new Date(startTime)) + ".md";

        try {
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
            try {
                pw.println("# User Action Recording");
                pw.println();
                pw.println("**Started**: " + readableFmt.format(new Date(startTime)));
                pw.println("**Stopped**: " + readableFmt.format(new Date(stopTime)));
                pw.println("**Actions recorded**: " + actions.size());
                pw.println();

                // --- Actions table ---
                pw.println("## Actions");
                pw.println();
                pw.println("| # | Time | Action | Target | Type | Details |");
                pw.println("|---|------|--------|--------|------|---------|");

                for (int i = 0; i < actions.size(); i++) {
                    RecordedAction a = actions.get(i);
                    String time = timeFmt.format(new Date(a.timestamp));
                    String target = a.targetName != null ? escapeMd(a.targetName) : "-";
                    String details = escapeMd(formatParams(a.params));
                    pw.println("| " + (i + 1) + " | " + time + " | " + a.action
                            + " | " + target + " | " + a.targetType
                            + " | " + details + " |");
                }

                // --- Replay section ---
                pw.println();
                pw.println("## Replay Commands");
                pw.println();
                pw.println("```bash");
                for (RecordedAction a : actions) {
                    String json = buildReplayJson(a);
                    if (json != null) {
                        pw.println("curl -X POST http://localhost:9222/action "
                                + "-H \"Content-Type: application/json\" "
                                + "-d '" + json + "'");
                    }
                }
                pw.println("```");
            } finally {
                pw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return filename;
    }

    private String formatParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private String buildReplayJson(RecordedAction a) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"action\":\"").append(escapeJson(a.action)).append("\"");

        if (a.targetName != null) {
            json.append(",\"target\":\"").append(escapeJson(a.targetName)).append("\"");
        }

        for (Map.Entry<String, String> entry : a.params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // Skip display-only params that are not part of the API
            if ("selected".equals(key) || "column".equals(key)
                    || "text".equals(key) && "click".equals(a.action)
                    || "tab".equals(key)) {
                continue;
            }
            json.append(",\"").append(escapeJson(key)).append("\":");
            if ("row".equals(key) || "index".equals(key)) {
                json.append(value);
            } else {
                json.append("\"").append(escapeJson(value)).append("\"");
            }
        }

        json.append("}");
        return json.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String escapeMd(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|");
    }
}
