# Java Swing MCP Server — Complete Recap

## What Is It?

`swing-mcp-server` is a lightweight HTTP server library that **embeds inside any running Java Swing application**, giving AI agents and external tools structured, semantic access to the live component tree. Instead of brittle pixel scraping or fragile coordinate-based clicking, consumers get typed JSON describing every button, table, tree, combo box, and text field — and can interact with them by name.

The system was built as a proof-of-concept but has grown into a practical toolkit for **AI-driven testing, user action recording, accessibility auditing, and developer diagnostics** of legacy and modern Swing applications.

---

## Architecture

### Three-Process Design

```
┌──────────────────────────────────────────┐
│  Java Swing Application (any app)        │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │  Swing UI (Event Dispatch Thread)  │  │
│  │  Components, listeners, rendering  │  │
│  └──────────────┬─────────────────────┘  │
│                 │ invokeAndWait()         │
│  ┌──────────────┴─────────────────────┐  │
│  │  SwingMcpServer (embedded library) │  │
│  │  JDK HttpServer on localhost:9222  │  │
│  │  JSON via Gson                     │  │
│  └──────────────┬─────────────────────┘  │
│                 │ HTTP                    │
└─────────────────┼────────────────────────┘
                  │
┌─────────────────┼────────────────────────┐
│  Python Orchestrator                     │
│  SwingClient (HTTP) ←→ Agent Loop        │
│  Context management, tool dispatch       │
│                 │ Anthropic API           │
└─────────────────┼────────────────────────┘
                  │
┌─────────────────┼────────────────────────┐
│  Claude (cloud)                          │
│  Receives component data + screenshots   │
│  Reasons about UI state                  │
│  Issues tool calls to interact           │
└──────────────────────────────────────────┘
```

### Key Architectural Decision: Embedded, Not Agent

The MCP server runs **inside** the Swing app's JVM, started by a single line of code:

```java
SwingMcpServer.start(9222);
```

This gives it direct access to the component hierarchy without bytecode instrumentation, JVM agent flags, or remote debugging. The trade-off — requiring a source code change — is minimal compared to the reliability gained.

### Threading Model

The JDK `HttpServer` dispatches requests on a thread pool. Swing components must only be touched on the Event Dispatch Thread (EDT). The `runOnEDT()` bridge enforces this invariant:

```
HTTP Thread Pool              EDT
     │                         │
     │   invokeAndWait()       │
     ├────────────────────────>│
     │                         │── read component state
     │                         │── execute action
     │                         │── serialize to Map
     │<────────────────────────│
     │                         │
     │── serialize Map to JSON │
     │── send HTTP response    │
```

All Swing API calls — reading properties, walking children, executing clicks — happen exclusively on the EDT. `invokeAndWait()` is used (not `invokeLater()`) because the HTTP handler needs the result synchronously.

---

## Library Internals (swing-mcp-lib)

### Package: `com.swingmcp.server`

| Class | Purpose |
|-------|---------|
| `SwingMcpServer` | Entry point. Creates/starts JDK `HttpServer`. One static method: `start(port)`. |
| `HttpApiHandler` | HTTP routing + EDT bridge. Dispatches to all other classes. Adds CORS headers. |
| `ComponentTreeWalker` | Recursive component discovery. Assigns stable integer IDs via `putClientProperty("swingmcp.id", id)`. Filters by type, interactability, depth. Flattens tree to array. |
| `ComponentStateExtractor` | Type-specific state extraction. Returns detailed Maps for JTable (rows, columns, data, pagination), JTree (nodes, expanded/selected paths), JComboBox (items, selection), text components, buttons, menus. |
| `InteractionExecutor` | Executes 13 action types (click, double_click, right_click, type, clear, select_combo, select_row, select_tree, expand_tree, collapse_tree, check, uncheck, menu). Uses `doClick()` for buttons, `java.awt.Robot` for mouse simulation, `setText()` for text fields. Waits 100ms after each action for UI to settle. |
| `ScreenshotCapture` | Uses `java.awt.Robot.createScreenCapture()`. Returns PNG as byte array. Supports full-window or single-component capture. |
| `ContrastChecker` | WCAG 2.1 contrast ratio validation. Walks parent chain for effective background. Reports AA/AAA compliance per component. |
| `UserActionRecorder` | Records user interactions via global `AWTEventListener`. Captures mouse clicks, text changes (focus-based), combo selections, menu clicks. Deduplicates phantom single-clicks before double-clicks. Outputs timestamped Markdown with action table + curl replay commands. |

### Model DTOs

| Class | Fields |
|-------|--------|
| `ComponentNode` | id, type, name, parent, bounds, screenBounds, visible, enabled, focused, text, accessibleRole, childCount |
| `ActionRequest` | action, target, text, value, index, row, path |
| `ActionResult` | success, action, target, componentState, error |
| `TableData` | Full table state with pagination support |

### Component Tree Serialization: Flat, Not Nested

The tree is serialized as a **flat JSON array** where each node carries a `parent` ID:

```json
[
  {"id": 1, "type": "JFrame", "name": "mainFrame", "parent": null, ...},
  {"id": 5, "type": "JButton", "name": "submitButton", "parent": 1, ...},
  {"id": 6, "type": "JTextField", "name": "symbolField", "parent": 1, ...}
]
```

**Why flat?** Nested JSON creates deeply indented structures that are harder for LLMs to navigate. Flat arrays let each component be self-contained, referenced by ID at any depth, and filtered/sliced without restructuring. Truncation at 200 nodes is clean — no orphaned subtrees.

### Component Filtering

Three layers of filtering reduce noise:

1. **Skip types**: JPanel, JLayeredPane, JRootPane, CellRendererPane, JViewport, unnamed JScrollPane — layout containers that clutter the tree without adding semantic value. Their children are still traversed.
2. **Interactable filter** (default on): Only returns components users can interact with — buttons, text fields, combo boxes, tables, trees, lists, menus, sliders, spinners, tabbed panes.
3. **Type filter**: Explicit whitelist like `types=JButton,JTextField`.

### Stable Component IDs

Every `JComponent` gets a persistent integer ID stored via `putClientProperty("swingmcp.id", id)`. IDs are never recycled. This means external clients can reference a component by ID across multiple API calls without worrying about instability. Components can also be referenced by their `getName()` string.

### Dependencies

- **Runtime**: Gson 2.11.0 (JSON serialization) — the only external dependency
- **JDK built-in**: `com.sun.net.httpserver.HttpServer` for HTTP transport
- **Java 8 compatible**: No Java 9+ features used anywhere

---

## HTTP API Reference

### Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/tree` | GET | Component tree (flat JSON array) |
| `/component/{nameOrId}` | GET | Detailed state of one component |
| `/action` | POST | Execute interaction (click, type, select, menu, etc.) |
| `/screenshot` | GET | PNG screenshot (base64 JSON or raw bytes) |
| `/contrast` | GET | WCAG 2.1 contrast ratio audit |
| `/health` | GET | Server status (ok, component count, uptime) |
| `/record/start` | POST | Start recording user actions |
| `/record/stop` | POST | Stop recording, write Markdown file |
| `/record/status` | GET | Check recording status |

### GET /tree

Returns all interactive components (default) or full tree.

**Query params**: `interactable` (bool, default true), `types` (comma-separated), `depth` (int)

```bash
curl http://localhost:9222/tree
curl "http://localhost:9222/tree?types=JButton,JTextField"
```

### GET /component/{nameOrId}

Returns type-specific detailed state.

- **JTable**: columns, data (paginated, default 50 rows), selectedRows, rowCount, hasMore
- **JTree**: nodes with paths, expandedPaths, selectedPath
- **JComboBox**: items, selectedItem, selectedIndex, editable
- **Text fields**: text, editable, caretPosition, selectionStart/End
- **Common**: id, type, name, bounds, screenBounds, visible, enabled, foreground, background

```bash
curl http://localhost:9222/component/blotterTable
curl "http://localhost:9222/component/blotterTable?rows=0-9"
```

### POST /action

Execute one of 13 supported actions:

| Action | Key Fields | Behavior |
|--------|-----------|----------|
| `click` | target | `doClick()` for buttons; Robot click for others. Supports `row` for JTable, `path` for JTree |
| `double_click` | target | Robot double-click. Supports `row`, `path` |
| `right_click` | target | Robot right-click |
| `type` | target, text | `setText()` for text components |
| `clear` | target | `setText("")` |
| `select_combo` | target, value/index | By display value or numeric index |
| `select_row` | target, row | Table row selection |
| `select_tree` | target, path | Tree node selection by path (e.g. `"root > Technology > AAPL"`) |
| `expand_tree` | target, path | Expand tree node |
| `collapse_tree` | target, path | Collapse tree node |
| `check` | target | Check checkbox (no-op if already checked) |
| `uncheck` | target | Uncheck checkbox |
| `menu` | path | Navigate menu bar by path (e.g. `"File > Save Layout"`) |

```bash
curl -X POST http://localhost:9222/action \
  -H "Content-Type: application/json" \
  -d '{"action":"click","target":"submitButton"}'

curl -X POST http://localhost:9222/action \
  -H "Content-Type: application/json" \
  -d '{"action":"menu","path":"File > Save Layout"}'
```

Every action returns the updated component state after a 100ms settle delay:

```json
{"success": true, "action": "click", "target": "submitButton", "componentState": {...}, "error": null}
```

### GET /screenshot

```bash
curl http://localhost:9222/screenshot                              # base64 JSON
curl "http://localhost:9222/screenshot?component=blotterTable"     # specific component
curl "http://localhost:9222/screenshot?format=raw" > app.png       # raw PNG
```

Response (base64 mode):
```json
{"image": "data:image/png;base64,...", "width": 1400, "height": 900}
```

Also saves a timestamped PNG file to disk automatically.

### GET /contrast

Audits all text-bearing components against WCAG 2.1:

```json
{
  "issues": [
    {
      "id": 22, "type": "JLabel", "name": "ordersCountLabel",
      "text": "Orders: 15",
      "foreground": "#3C3C3C", "background": "#323232",
      "contrastRatio": 1.18, "wcagAA": false, "wcagAAA": false,
      "minimumRequired": 4.5
    }
  ],
  "totalChecked": 45,
  "totalIssues": 1
}
```

### Recording Endpoints

```bash
curl -X POST http://localhost:9222/record/start    # Start recording
curl http://localhost:9222/record/status            # {"recording": true, "actions": 7}
curl -X POST http://localhost:9222/record/stop      # Stop + write file
```

---

## User Action Recording & Replay

### How It Works

The `UserActionRecorder` registers a **global AWTEventListener** that captures all mouse clicks and focus events across the entire Swing application — no per-component wiring needed. It also attaches `ItemListener` to combo boxes for selection tracking and monitors text field changes via focus gain/loss diffing.

### What Gets Recorded

- Mouse clicks (single, double, right) on buttons, tables, trees, tabbed panes
- Tree path selections and expand/collapse
- Table row/column clicks
- Menu item clicks (with full menu path)
- Text field changes (captured when focus leaves the field)
- Combo box selections

### Smart Deduplication

When a user double-clicks, Swing fires both a single-click and a double-click event. The recorder removes phantom single-clicks that occur within 500ms before a double-click on the same target.

### Output: Markdown with Replay Commands

Recording produces a timestamped Markdown file with two sections:

**Actions Table:**

| # | Time | Action | Target | Type | Details |
|---|------|--------|--------|------|---------|
| 1 | 08:38:58.902 | click | instrumentTree | JTree | path=root > Technology |
| 2 | 08:39:01.077 | double_click | instrumentTree | JTree | path=root > Technology |
| 3 | 08:39:03.357 | double_click | quoteTable_Technology | JTable | row=1, column=0 |

**Replay Commands:**

```bash
curl -X POST http://localhost:9222/action \
  -H "Content-Type: application/json" \
  -d '{"action":"click","target":"instrumentTree","path":"root > Technology"}'

curl -X POST http://localhost:9222/action \
  -H "Content-Type: application/json" \
  -d '{"action":"double_click","target":"quoteTable_Technology","row":1}'
```

These curl commands can be pasted directly into a terminal or incorporated into test scripts to **replay the exact user session**.

---

## Python Orchestrator (AI Agent Loop)

### Components

| File | Purpose |
|------|---------|
| `orchestrator.py` | Claude agent loop with context management, token estimation, sliding-window compaction |
| `swing_client.py` | HTTP client wrapper for all MCP endpoints |
| `tool_definitions.py` | 10 Claude tool schemas (get_component_tree, click, type_text, take_screenshot, etc.) |

### How the Agent Loop Works

1. User provides a task description (e.g., "Submit a BUY order for 100 AAPL")
2. Orchestrator sends task + message history to Claude with tool definitions
3. Claude responds with `tool_use` calls (e.g., `get_component_tree`, `click`, `take_screenshot`)
4. Orchestrator executes tools against the MCP server HTTP API
5. Tool results (JSON + images) are appended to conversation
6. Loop continues until Claude responds with final text (no more tool calls)

### Context Window Management

This is the most sophisticated piece of the orchestrator. Without it, the agent fails after ~10 tool calls as screenshots and component trees fill the context window.

**Strategy — Sliding Window Compaction:**

- **Token estimation**: `len(json.dumps(messages)) / 4` for text + 1,600 per image
- **Threshold**: 80,000 tokens triggers compaction
- **Compaction rules** (applied to tool results older than 5 most recent turns):
  - Screenshots: Strip image data → `"[Screenshot removed to save context]"`
  - Component trees: Summarize → `"[Component tree: N components. Named: list...]"`
  - Table data: Summarize → `"[Table 'name': N rows, columns: ...]"`
  - Contrast results: Summarize → `"[Contrast check: N issues out of M checked]"`
  - Action results: Keep intact (small and essential for reasoning)

### Running the Orchestrator

```bash
cd orchestrator
pip install -r requirements.txt
export ANTHROPIC_API_KEY="sk-..."

python orchestrator.py "Explore the app, submit a BUY order for AAPL, verify it in the blotter"
```

---

## Demo Application (Trading Blotter)

### Overview

The demo app is an MDI (Multiple Document Interface) trading application built with Java Swing and the FlatLaf Darcula theme. It serves as the test target for the MCP server.

### Windows

| Window | Type | Description |
|--------|------|-------------|
| Main Frame | JFrame | MDI parent with menu bar (File, View, Tools, Help) |
| Portfolios | JInternalFrame | JTree with 5 sectors (Technology, Financials, Healthcare, Energy, ETFs) containing ~30 instruments |
| Quote Windows | JInternalFrame | Sector-specific quote tables with real-time-style data (Symbol, Last, Change, Bid, Ask, Volume, etc.) |
| Order Tickets | JInternalFrame | Full order entry form (Symbol, Side, Qty, Order Type, Limit/Stop Price, Account, TIF, Route) |
| Blotter | JInternalFrame | Order log table (Symbol, Side, Qty, Price, Value, Account, Time, Status) |

### Interaction Flows

- **Single-click** a sector node → toggles expand/collapse after 300ms delay
- **Double-click** a sector node → opens quote window for that sector
- **Double-click** an instrument leaf → opens order ticket with benchmark data
- **Double-click** a quote table row → opens order ticket for that symbol
- **Place Order** button → adds to blotter with "Pending" status, auto-executes after 5 seconds

### Layout Persistence (File > Save Layout)

The app saves and restores the complete window layout:

- **Main frame**: position, size, maximized state
- **All internal frames**: position, size, visible, iconified, maximized
- **Portfolio tree state**: expanded paths and selected node
- **Quote windows**: automatically recreated on startup if they were open when saved
- **Order tickets**: automatically recreated with correct benchmark data on startup

Saved to `~/.swingmcp/layout.properties`. Off-screen window positions are clamped to visible screen bounds (at least 50x50px overlap required).

### Named Components

All interactive components have `setName()` for API access:

| Name | Type | Description |
|------|------|-------------|
| `instrumentTree` | JTree | Portfolio tree |
| `quoteTable_{sector}` | JTable | Quote table (e.g., `quoteTable_Technology`) |
| `blotterTable` | JTable | Order blotter |
| `placeOrderButton` | JButton | Submit order |
| `cancelButton` | JButton | Cancel order |
| `sideCombo` | JComboBox | BUY/SELL/SHORT |
| `orderTypeCombo` | JComboBox | MARKET/LIMIT/STOP/STOP LIMIT |
| `accountCombo` | JComboBox | Trading accounts |
| `orderQtyField` | JTextField | Quantity |
| `orderLimitPrice` | JTextField | Limit price |
| `statusBar` | JTextField | Status messages |

---

## What You Can Do With This Server

### 1. AI-Driven Exploratory Testing

Give Claude a natural language task and let it explore, interact, and report:

```bash
python orchestrator.py "Find all interactive components, submit an order for TSLA,
  verify it appears in the blotter, then check for accessibility violations"
```

Claude will autonomously navigate the tree, click through menus, fill forms, submit, and validate results — all through the structured API.

### 2. Automated Regression Testing

Write test cases as curl command sequences (or use the recorded actions):

```bash
# Expand sector, open quote, submit order, verify blotter
curl -X POST http://localhost:9222/action -d '{"action":"expand_tree","target":"instrumentTree","path":"root > Technology"}'
curl -X POST http://localhost:9222/action -d '{"action":"select_tree","target":"instrumentTree","path":"root > Technology > AAPL - Apple Inc"}'
curl -X POST http://localhost:9222/action -d '{"action":"double_click","target":"instrumentTree","path":"root > Technology > AAPL - Apple Inc"}'
# ... fill order form, click submit ...
curl http://localhost:9222/component/blotterTable  # verify order appears
```

### 3. Record User Sessions → Generate Test Scripts

Start recording, perform actions manually in the app, stop recording. Get a Markdown file with:
- A table of every action with timestamps
- Ready-to-run curl commands that replay the exact session

This turns manual QA into repeatable automated tests with zero scripting effort.

### 4. WCAG Accessibility Auditing

One API call audits every text-bearing component for WCAG 2.1 contrast compliance:

```bash
curl http://localhost:9222/contrast
```

Returns specific components that fail AA (4.5:1) or AAA (7.0:1) thresholds, with exact foreground/background colors and computed ratios.

### 5. Visual Regression via Screenshots

Capture screenshots of the entire app or individual components:

```bash
curl "http://localhost:9222/screenshot?format=raw" > baseline.png
# ... make changes ...
curl "http://localhost:9222/screenshot?format=raw" > after.png
# diff with any image comparison tool
```

### 6. Component State Inspection / Debugging

Query any component's full state at runtime:

```bash
curl http://localhost:9222/component/blotterTable    # all table data, columns, selection
curl http://localhost:9222/component/instrumentTree   # expanded paths, selected node, all nodes
curl http://localhost:9222/component/sideCombo        # items, selected item, editable
```

This is more convenient than attaching a debugger for UI state inspection.

### 7. External Tooling Integration

The HTTP API makes the Swing app controllable from any language or tool:
- Python scripts, shell scripts, CI/CD pipelines
- Test frameworks (pytest, JUnit via HTTP calls)
- Monitoring dashboards
- Custom GUIs that drive the Swing app remotely

---

## Test Cases Developed

Seven MCP-based test cases have been written and verified:

| Test Case | What It Validates |
|-----------|-------------------|
| `swing-mcp-test-case-full-workflow.md` | End-to-end: expand sector → open quote → open order ticket → fill form → submit → verify in blotter |
| `swing-mcp-test-case-double-click-order.md` | Double-clicking instruments (AAPL, JPM) opens order tickets with correct quote data |
| `swing-mcp-test-case-double-click-quote.md` | Double-clicking sectors opens quote windows with correct data |
| `swing-mcp-test-case-single-click-toggle.md` | Single-click on sector toggles expand/collapse after 300ms delay timer |
| `swing-mcp-test-case-expand-icons.md` | Expand/collapse icons visible on sector nodes |
| `swing-mcp-test-case-negative-change-color.md` | Red text for negative price changes, green for positive |
| `swing-mcp-test-case-save-restore-layout.md` | Save Layout persists all windows, tree state; restart restores everything; off-screen clamping works |

Each test case is a step-by-step Markdown document with curl commands, screenshot captures, and expected results.

---

## Integration Guide for Java Developers

### Step 1: Add the Library

```gradle
dependencies {
    implementation project(':swing-mcp-lib')  // local
    // or from Maven: implementation 'com.swingmcp:swing-mcp-lib:1.0'
}
```

### Step 2: Start the Server

Add one line after your UI is initialized:

```java
import com.swingmcp.server.SwingMcpServer;

public class YourApp {
    public static void main(String[] args) {
        // ... initialize your Swing UI ...
        SwingMcpServer.start(9222);
    }
}
```

### Step 3: Name Your Components

Components with `setName()` are referenceable by name in API calls:

```java
JButton submit = new JButton("Submit");
submit.setName("submitButton");  // Now accessible as "submitButton" via API

JTable orders = new JTable(model);
orders.setName("ordersTable");   // Full table data available via /component/ordersTable
```

Components without names are still accessible via auto-assigned numeric IDs.

### Step 4: Use the API

```bash
curl http://localhost:9222/health                    # Verify server running
curl http://localhost:9222/tree                       # See all interactive components
curl http://localhost:9222/component/ordersTable      # Inspect table data
curl -X POST http://localhost:9222/action \
  -d '{"action":"click","target":"submitButton"}'    # Click a button
```

### What Developers Get For Free

- **Zero-config component discovery**: Every Swing component is automatically found and exposed
- **Type-aware state extraction**: Tables return row data, trees return paths, combos return items
- **Thread-safe**: All access goes through EDT — no risk of Swing threading bugs
- **Lightweight**: One dependency (Gson), no framework overhead, ~12 Java files
- **Java 8 compatible**: Works in legacy enterprise environments

---

## Known Issues & Limitations

| # | Issue | Impact | Mitigation |
|---|-------|--------|------------|
| 1 | Robot-based screenshots capture screen pixels, not component paint | Overlapping windows appear in screenshots | Bring app to front before capturing; or use component-specific screenshots |
| 2 | Single-client assumption | Concurrent action requests may race on EDT | Use one client at a time; 100ms settle delay is best-effort |
| 3 | JMenuBar duplication in full tree | Menu items may appear twice when `interactable=false` | Use default `interactable=true` |
| 4 | Java 8 ternary autoboxing | Mixing `int` and nullable `Integer` in ternary causes NPE | Always use `Integer.valueOf()` when mixing types |
| 5 | Context window exhaustion in orchestrator | Agent fails after ~10-15 tool calls without compaction | Sliding window compaction at 80K tokens; keep screenshots sparse |
| 6 | Window type restriction | Only `JFrame` instances are traversed | JDialog/JWindow support needs extension for some apps |

---

## What Other Developers Would Want to Know

### Q: Does this work with any Swing app or just the demo?

The library (`swing-mcp-lib`) is fully generic. It walks `Window.getWindows()` to find all visible JFrames, then recursively traverses the component hierarchy. The demo app has no special hooks — the library discovers everything automatically. Any Swing app that calls `SwingMcpServer.start(port)` gets the full API.

### Q: Can I use this without Claude / without the Python orchestrator?

Yes. The HTTP API is a standard REST-like interface. Use curl, Postman, any HTTP client in any language, or write your own scripts. The orchestrator is optional — it just adds an AI agent loop on top.

### Q: How does it handle custom/third-party components?

Custom components inheriting from standard Swing classes (JPanel, JComponent, etc.) are traversed automatically. Their accessible name and basic properties are extracted. For type-specific state extraction (like custom table implementations), the component would need to extend a recognized type (JTable, JTree, etc.) or you'd add a custom extractor.

### Q: Performance impact on the host app?

Minimal. The HTTP server uses JDK's built-in lightweight server. Component tree walks and state extraction happen on the EDT via `invokeAndWait()`, so they briefly pause UI event processing (typically <10ms for trees under 200 components). Screenshots use Robot which is a fast native call.

### Q: Can I use this in CI/CD for headless testing?

Partially. Component tree inspection, state queries, and programmatic actions (click, type, select) work headlessly. Screenshots require a display (Robot needs screen pixels). Use Xvfb on Linux for headless screenshot support.

### Q: What about security?

The server binds to `localhost` only — it's not accessible from the network. CORS headers are set to `*` for local development convenience. For production use, consider adding authentication or restricting to specific origins.

### Q: Can the recorded actions be used for load testing?

The replay curl commands are sequential. For load testing, you'd need to parallelize them and handle response validation. The server's single-client design means concurrent requests need care.

### Q: How do I add new action types?

Add a new case in `InteractionExecutor.execute()`, implement the action logic, and optionally add a corresponding tool definition in `tool_definitions.py` for the orchestrator.

---

## Project File Structure

```
swing-mcp-server/
├── settings.gradle                          # Multi-module: swing-mcp-lib, demo-app
├── gradlew / gradlew.bat                    # Gradle 8.5 wrapper
├── README.md                                # Full API docs + integration guide
├── KNOWN_ISSUES.md                          # Bug tracker + build notes
├── docs/
│   └── ARCHITECTURE.md                      # Deep dive: threading, IDs, data flow
│
├── swing-mcp-lib/                           # The reusable library
│   ├── build.gradle                         # Java 8, depends on Gson 2.11.0
│   └── src/main/java/com/swingmcp/server/
│       ├── SwingMcpServer.java              # Entry point: start(port)
│       ├── HttpApiHandler.java              # HTTP routing + EDT bridge
│       ├── ComponentTreeWalker.java         # Component discovery + ID assignment
│       ├── ComponentStateExtractor.java     # Type-specific state extraction
│       ├── InteractionExecutor.java         # 13 action types
│       ├── ScreenshotCapture.java           # Robot-based PNG capture
│       ├── ContrastChecker.java             # WCAG 2.1 contrast audit
│       ├── UserActionRecorder.java          # Record + replay user sessions
│       └── model/
│           ├── ComponentNode.java           # Tree node DTO
│           ├── TableData.java               # Table data DTO
│           ├── ActionRequest.java           # Action request DTO
│           └── ActionResult.java            # Action result DTO
│
├── demo-app/                                # Trading blotter demo
│   ├── build.gradle                         # Java 8, depends on swing-mcp-lib + FlatLaf
│   └── src/main/java/com/swingmcp/demo/
│       ├── TradingApp.java                  # MDI app with layout persistence
│       ├── FrameBuilder.java                # MDI frame construction
│       ├── ComponentManager.java            # Window lifecycle management
│       ├── LayoutManager.java               # Save/restore layout to disk
│       ├── InstrumentPanel.java             # Portfolio JTree
│       ├── QuotePanel.java                  # Quote data JTable
│       ├── BlotterPanel.java                # Order log JTable
│       ├── Order.java / OrderFrame.java     # Order ticket
│       ├── MenuManager.java                 # Menu bar definition
│       ├── MenuActionListener.java          # Menu action dispatch
│       └── ResourceManager.java             # Shared state (desktop pane, main frame)
│
├── orchestrator/                            # Python AI agent
│   ├── requirements.txt                     # anthropic, requests, Pillow
│   ├── orchestrator.py                      # Agent loop + context compaction
│   ├── swing_client.py                      # HTTP client wrapper
│   └── tool_definitions.py                  # 10 Claude tool schemas
│
└── [test cases]                             # MCP-based test cases (7 files)
    ├── swing-mcp-test-case-full-workflow.md
    ├── swing-mcp-test-case-double-click-order.md
    ├── swing-mcp-test-case-double-click-quote.md
    ├── swing-mcp-test-case-single-click-toggle.md
    ├── swing-mcp-test-case-expand-icons.md
    ├── swing-mcp-test-case-negative-change-color.md
    └── swing-mcp-test-case-save-restore-layout.md
```

---

## Summary

The Java Swing MCP Server bridges the gap between legacy Swing applications and modern AI tooling. By embedding a lightweight HTTP server that exposes the live component tree with semantic actions, it enables:

- **AI agents** to understand and interact with Swing UIs through structured data instead of pixels
- **QA teams** to record manual sessions and replay them as automated tests
- **Developers** to inspect component state at runtime without a debugger
- **Accessibility auditors** to check WCAG compliance programmatically
- **CI/CD pipelines** to validate UI behavior as part of build verification

All of this with a single line of code: `SwingMcpServer.start(9222)`.
