# Architecture Deep Dive

This document describes the internal architecture, data flow, and design decisions of the swing-mcp-server system.

## System Overview

The system consists of three independently running components that communicate over HTTP and the Anthropic API:

```
┌─────────────────────────────────┐
│  Java Swing App Process         │
│                                 │
│  ┌───────────────────────────┐  │
│  │  Swing UI (EDT)           │  │
│  │  - Components, listeners  │  │
│  │  - Renders on screen      │  │
│  └───────────┬───────────────┘  │
│              │ invokeAndWait()  │
│  ┌───────────┴───────────────┐  │
│  │  SwingMcpServer           │  │
│  │  - HttpServer threads     │  │
│  │  - JSON serialization     │  │
│  │  - EDT bridge             │  │
│  └───────────┬───────────────┘  │
│              │ HTTP :9222       │
└──────────────┼──────────────────┘
               │
┌──────────────┼──────────────────┐
│  Python Orchestrator Process    │
│              │                  │
│  ┌───────────┴───────────────┐  │
│  │  SwingClient              │  │
│  │  - HTTP requests          │  │
│  │  - Response parsing       │  │
│  └───────────┬───────────────┘  │
│              │                  │
│  ┌───────────┴───────────────┐  │
│  │  Agent Loop               │  │
│  │  - Tool dispatch          │  │
│  │  - Context management     │  │
│  │  - Token estimation       │  │
│  └───────────┬───────────────┘  │
│              │ Anthropic API    │
└──────────────┼──────────────────┘
               │
┌──────────────┼──────────────────┐
│  Claude (Anthropic Cloud)       │
│  - Receives component data      │
│  - Reasons about UI state       │
│  - Issues tool calls            │
│  - Produces final reports       │
└─────────────────────────────────┘
```

## Java Library Architecture

### Class Responsibilities

| Class | Thread | Responsibility |
|-------|--------|---------------|
| `SwingMcpServer` | Main | Entry point. Creates and starts the JDK `HttpServer`. |
| `HttpApiHandler` | HTTP thread pool | Routes HTTP requests. Bridges to EDT via `runOnEDT()`. |
| `ComponentTreeWalker` | EDT | Recursively walks the Swing component hierarchy. Assigns stable IDs. Filters by type and interactability. |
| `ComponentStateExtractor` | EDT | Extracts per-type detailed state (table data, tree nodes, combo items, etc.) |
| `InteractionExecutor` | EDT | Executes semantic actions (click, type, select) on components. |
| `ScreenshotCapture` | EDT | Uses `java.awt.Robot` to capture component/window screenshots. |
| `ContrastChecker` | EDT | Computes WCAG 2.1 contrast ratios for all text-bearing components. |

### Threading Model

The JDK `HttpServer` dispatches each HTTP request on a thread from its internal pool. Swing components must only be accessed on the Event Dispatch Thread (EDT). The bridge between these two threading domains is the `runOnEDT()` utility in `HttpApiHandler`:

```
HTTP Thread Pool          EDT
     │                     │
     │  invokeAndWait()    │
     ├────────────────────>│
     │                     │── read component state
     │                     │── serialize to Map
     │<────────────────────│
     │                     │
     │── serialize to JSON │
     │── send HTTP response│
```

**Key invariant**: No Swing API call (reading properties, walking children, executing actions) ever happens outside the EDT. The `runOnEDT()` method enforces this:

```java
public static <T> T runOnEDT(Callable<T> callable) throws Exception {
    if (SwingUtilities.isEventDispatchThread()) {
        return callable.call();  // Already on EDT
    }
    AtomicReference<T> result = new AtomicReference<>();
    AtomicReference<Exception> error = new AtomicReference<>();
    SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
            try {
                result.set(callable.call());
            } catch (Exception e) {
                error.set(e);
            }
        }
    });
    if (error.get() != null) throw error.get();
    return result.get();
}
```

This uses `invokeAndWait()` (blocking) rather than `invokeLater()` (async) because the HTTP handler needs the result synchronously to send the response.

### Component ID Assignment

Each Swing component gets a stable integer ID:

1. When `ComponentTreeWalker` encounters a `JComponent`, it checks `getClientProperty("swingmcp.id")`.
2. If an ID exists, it's reused (stable across requests).
3. If no ID exists, a new one is assigned from an `AtomicInteger` counter and stored via `putClientProperty("swingmcp.id", id)`.
4. For non-JComponent instances (like `Window`/`Frame`), `System.identityHashCode()` is used as a stable identifier.

IDs are never recycled. If a component is removed and a new one is created, the new component gets a new ID. This prevents stale references.

### Component Tree Serialization

The tree is serialized as a **flat array** with parent references rather than nested JSON:

```json
[
  {"id": 1, "type": "JFrame", "parent": null, ...},
  {"id": 5, "type": "JButton", "parent": 1, ...},
  {"id": 6, "type": "JTextField", "parent": 1, ...}
]
```

**Why flat?** Nested JSON creates deeply indented structures that are harder for LLMs to navigate. With flat arrays:
- Each component is self-contained with all its properties
- Parent-child relationships are explicit via the `parent` field
- Components can be referenced by ID without traversing a tree
- The array can be filtered/sliced without restructuring

### Component Filtering

The tree walker applies three levels of filtering:

1. **Skip types**: JPanel, JLayeredPane, JRootPane, CellRendererPane, JViewport, and unnamed JScrollPane are skipped (but their children are still traversed). These layout containers clutter the tree without adding semantic value.

2. **Interactable filter** (`interactable=true`): Returns only components that users can interact with: JButton, JToggleButton, JCheckBox, JRadioButton, JTextField, JTextArea, JEditorPane, JPasswordField, JComboBox, JTable, JTree, JList, JMenu, JMenuItem, JCheckBoxMenuItem, JRadioButtonMenuItem, JMenuBar, JSpinner, JSlider, JTabbedPane.

3. **Type filter** (`types=JButton,JTextField`): Returns only the specified component types.

The walker always traverses children even when a parent is filtered out. Filtered-out parents pass their parent ID through to their children, maintaining a coherent parent chain.

### Component Lookup

`findComponent(String nameOrId)` searches by ID or name:

- **Numeric input**: Walks all visible windows, checks each JComponent's `getClientProperty("swingmcp.id")` for a match.
- **String input**: Walks all visible windows, checks `getName()` for an exact match.
- **JMenuBar handling**: JMenuBars are searched separately because they aren't always reachable through the standard `Container.getComponent()` traversal.

### Text Extraction

Text is extracted differently based on component type:

| Component Type | Extraction Method |
|---------------|-------------------|
| JFrame, JDialog, JInternalFrame | `getTitle()` |
| JButton, JToggleButton, JCheckBox, JRadioButton | `getText()` |
| JLabel | `getText()` |
| JTextField, JTextArea, JEditorPane, JPasswordField | `getText()` (truncated to 500 chars for state, 100 chars for tree) |
| JComboBox | `String.valueOf(getSelectedItem())` |
| JTable | `"[rows=N, cols=M]"` summary |
| JTree | `"[rows=N]"` summary |
| JMenuBar | `"[menus=N]"` summary |
| JMenu, JMenuItem | `getText()` |
| All others | `getAccessibleContext().getAccessibleName()` |

### Interaction Execution

The `InteractionExecutor` supports 13 action types. Key implementation details:

- **click**: Uses `doClick()` for `AbstractButton` subclasses (JButton, JCheckBox, etc.) because it properly fires ActionListeners. For non-buttons, falls back to `java.awt.Robot` to simulate a physical click at the component's screen center.
- **type**: Uses `setText()` for `JTextComponent` subclasses (replaces content). For non-text components, falls back to Robot key events.
- **select_tree**: Parses a path string like `"Portfolio > Equities > AAPL"`, walks the TreeModel to find the matching node, then calls `setSelectionPath()`.
- **menu**: Navigates the JMenuBar hierarchy by menu text, then calls `doClick()` on the target JMenuItem.
- **check/uncheck**: Calls `doClick()` only if the current selection state differs from the desired state.

After every action, the executor waits 100ms for the UI to settle (event processing, repainting), then extracts and returns the target component's updated state.

### Screenshot Capture

Uses `java.awt.Robot.createScreenCapture(Rectangle)` with the component's screen bounds (via `getLocationOnScreen()`). The result is a `BufferedImage` encoded as PNG bytes, which can be returned as:
- **base64**: JSON response with `{"image": "data:image/png;base64,...", "width": N, "height": N}`
- **raw**: PNG bytes with `Content-Type: image/png`

### WCAG Contrast Checking

The `ContrastChecker` walks all visible windows and checks every text-bearing component:

1. **Get foreground color**: `component.getForeground()`
2. **Get effective background color**: `component.getBackground()`. If the component's background is the same as its parent's (indicating transparency/inheritance), walk up the parent chain until finding a component with `isOpaque() == true`.
3. **Compute relative luminance**: Uses the WCAG 2.1 formula:
   ```
   L = 0.2126 * linearize(R) + 0.7152 * linearize(G) + 0.0722 * linearize(B)
   ```
   where `linearize(c)` converts sRGB (0-255) to linear light values accounting for gamma.
4. **Compute contrast ratio**: `(L_lighter + 0.05) / (L_darker + 0.05)`
5. **Check WCAG thresholds**: AA requires 4.5:1 for normal text, AAA requires 7:1.

The demo app intentionally includes a contrast bug: in dark mode, status panel labels use `Color(60,60,60)` foreground on `Color(50,50,50)` background (ratio ~1.18:1), which the checker flags.

## Python Orchestrator Architecture

### Agent Loop

The orchestrator implements a standard Claude tool-use agent loop:

```
1. Send task + message history to Claude
2. If Claude responds with text → done, print result
3. If Claude responds with tool_use → execute tools, append results, goto 1
```

### Tool Dispatch

The orchestrator maps Claude's tool calls to HTTP API calls:

| Claude Tool | HTTP Request |
|-------------|-------------|
| `get_component_tree` | `GET /tree?interactable=...` |
| `get_component_state` | `GET /component/{target}?rows=...` |
| `click` | `POST /action {"action":"click","target":"..."}` |
| `type_text` | `POST /action {"action":"type","target":"...","text":"..."}` |
| `select_combo_item` | `POST /action {"action":"select_combo","target":"...","value":"..."}` |
| `select_table_row` | `POST /action {"action":"select_row","target":"...","row":N}` |
| `select_tree_node` | `POST /action {"action":"select_tree","target":"...","path":"..."}` |
| `click_menu` | `POST /action {"action":"menu","path":"..."}` |
| `take_screenshot` | `GET /screenshot?component=...` |
| `check_contrast` | `GET /contrast` |

### Context Window Management

This is the most critical part of the orchestrator. Without it, the agent fails after ~10 tool calls.

**The problem**: Every turn adds the full assistant response and full tool results to the message history. Component trees are 2K-4K tokens, screenshots ~1.6K tokens, table data ~1.5K tokens. After 15 turns the conversation can exceed 50K tokens.

**Solution — sliding window compaction**:

1. **Token estimation**: Before each API call, estimate total tokens using `len(json.dumps(messages)) / 4` for text plus 1,600 per image.

2. **Threshold**: When estimated tokens exceed 80,000, trigger compaction.

3. **Compaction rules** (applied to tool results older than the 5 most recent turns):
   - **Screenshots**: Remove image data, replace with `"[Screenshot removed to save context]"`
   - **Component trees**: Replace with `"[Component tree: N components. Named: list...]"`
   - **Table data**: Replace with `"[Table 'name': N rows, columns: ...]"`
   - **Contrast results**: Replace with `"[Contrast check: N issues out of M checked]"`
   - **Action results**: Keep intact (they're small and Claude needs to remember what it did)

4. **Logging**: Token estimates are printed at each turn for observability.

## Data Flow Example

Here's a complete flow for "click the Submit button and verify the table updates":

```
1. Orchestrator → Claude: "Click submitButton and verify table updates"

2. Claude → tool_use: click(target="submitButton")

3. Orchestrator → SwingMCP: POST /action {"action":"click","target":"submitButton"}

4. SwingMCP (HTTP thread):
   └─ runOnEDT():
      └─ EDT:
         ├─ findComponent("submitButton")  → JButton
         ├─ button.doClick()               → fires ActionListener
         ├─ Thread.sleep(100)              → UI settles
         └─ extractState(button)           → Map{text, enabled, ...}

5. SwingMCP → Orchestrator: {"success":true,"componentState":{...}}

6. Orchestrator → Claude: tool_result with JSON

7. Claude → tool_use: get_component_state(target="ordersTable")

8. Orchestrator → SwingMCP: GET /component/ordersTable

9. SwingMCP (EDT):
   └─ extractState(table) → Map with columns, data, rowCount, etc.

10. SwingMCP → Orchestrator: {table JSON with all rows}

11. Orchestrator → Claude: tool_result with table data

12. Claude → text: "The order was submitted. Table now has 16 rows..."
```

## Known Limitations

### JMenuBar Duplication in Full Tree
When requesting the full tree (`interactable=false`), JMenuBar and its children may appear twice: once from the standard Container traversal and once from the explicit JFrame.getJMenuBar() traversal. This doesn't affect functionality or interactable-mode results.

### Window Type Restriction
The tree walker only processes `JFrame` instances. `JDialog`, `JWindow`, and other window types are not traversed unless they are children of a JFrame. This is sufficient for the demo but may need extension for real applications.

### Robot-Based Screenshots
Screenshots use `java.awt.Robot` which captures what's actually on screen. If another window overlaps the Swing app, it will appear in the screenshot. For headless testing environments, this approach won't work.

### No Concurrent Action Safety
The system assumes one client at a time. Multiple concurrent action requests could cause race conditions on the EDT. The 100ms settle delay after each action is a heuristic, not a guarantee.

### Java 8 Ternary Autoboxing
The most subtle bug encountered during development: mixing `int` and `Integer` in ternary expressions causes NullPointerException when the `Integer` is null. See the assignment document's Java 8 Compatibility section for details.
