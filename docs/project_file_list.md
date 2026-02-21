# Project File List

Complete listing of every file intended for the GitHub repository, with descriptions.

---

## Root: `swing-mcp-server/`

| File | Description |
|------|-------------|
| `README.md` | Project overview, architecture diagram, HTTP API reference, quick start guide, usage examples, and integration instructions. The primary documentation entry point. |
| `LICENSE` | MIT License (Copyright 2026 SwingMCP). |
| `settings.gradle` | Gradle multi-module project configuration. Declares root project name `swing-mcp-server` and includes two subprojects: `swing-mcp-lib` and `demo-app`. |
| `gradlew` | Gradle Wrapper shell script for Unix/macOS. Allows building without a global Gradle installation. Downloads Gradle 8.5 automatically on first run. |
| `gradlew.bat` | Gradle Wrapper batch script for Windows. Same purpose as `gradlew` but for Windows CMD. |
| `.gitignore` | Git ignore rules. Excludes `build/`, `.gradle/`, `*.class`, `__pycache__/`, `.idea/`, `*.iml`, `.vscode/`, `venv/`, `*.pyc`, `.env`. |

### `swing-mcp-server/gradle/wrapper/`

| File | Description |
|------|-------------|
| `gradle-wrapper.jar` | Gradle Wrapper bootstrap JAR. Responsible for downloading and invoking the correct Gradle version (8.5). |
| `gradle-wrapper.properties` | Gradle Wrapper configuration. Specifies distribution URL for Gradle 8.5 (`gradle-8.5-bin.zip`), download paths, and network timeout (10000ms). |

### `swing-mcp-server/docs/`

| File | Description |
|------|-------------|
| `ARCHITECTURE.md` | Deep-dive architecture document. Describes the three-component system (Swing app, embedded MCP server, Python orchestrator), data flow between EDT and HTTP threads, component ID assignment strategy, and action execution pipeline. |
| `diagrams.md` | ASCII and text-based diagrams illustrating runtime flow (HTTP to EDT bridge), component tree structure, action execution pipeline, and screenshot capture sequence. |
| `fat_client_rewrite_essay.md` | Essay on the "Fat Client Problem" - why rewriting legacy desktop applications breaks every team that tries. Covers server vs. client complexity, implicit state machines, layout engines, and why understanding the original UI is the hardest part. |
| `KNOWN_ISSUES.md` | Documents build issues encountered (Java 8 compat, ternary autoboxing NPE, Gradle wrapper), functional issues (menu duplication, screenshot focus, single-client), and orchestrator notes (context window, screenshot token cost). |
| `project_file_list.md` | This file. Complete listing of every file in the GitHub repository with descriptions. |
| `java_swing_mcp_server_recap.md` | Comprehensive project recap document. Describes what the system is, how it works, design decisions, capabilities, and lessons learned from building the proof-of-concept. |
| `java-swing-mcp.jpg` | Architecture infographic showing the three-zone system (Swing app, Python orchestrator, Claude agent), all six MCP server capabilities, and key facts. Referenced by README.md. |
| `java-swing-mcp-robot-horizontal.jpg` | Project banner image showing a robot interacting with a trading application. Referenced by README.md. |

---

## MCP Server Library: `swing-mcp-server/swing-mcp-lib/`

The reusable Java library that embeds an HTTP server inside any Swing application, providing structured API access to the component tree, interactions, screenshots, and accessibility checking.

| File | Description |
|------|-------------|
| `build.gradle` | Build configuration for the library module. Uses `java-library` plugin, targets Java 8 (`sourceCompatibility = 1.8`). Single dependency: `com.google.code.gson:gson:2.11.0` for JSON serialization. |

### `swing-mcp-lib/src/main/java/com/swingmcp/server/`

| File | Description |
|------|-------------|
| `SwingMcpServer.java` | Entry point for the embedded HTTP server. Provides static `start(int port)` and `stop()` methods. Creates a `com.sun.net.httpserver.HttpServer` on the specified port (typically 9222) and registers all API endpoint handlers. Zero external transport dependencies — uses JDK built-in HTTP server. |
| `HttpApiHandler.java` | HTTP request router and EDT bridge. Handles all incoming HTTP requests, parses paths and query parameters, dispatches to the appropriate handler (`/tree`, `/component`, `/action`, `/screenshot`, `/contrast`, `/health`). All Swing component access goes through `SwingUtilities.invokeAndWait()` to ensure EDT safety. Returns JSON responses with proper content types and error handling. |
| `ComponentTreeWalker.java` | Recursive component hierarchy traversal. Walks the entire Swing component tree starting from all top-level windows (`Window.getWindows()`). Assigns stable integer IDs to components via `putClientProperty("swingmcp.id", id)`. Supports filtering by type and interactability. Outputs a flat JSON array with parent ID references (not nested) — designed for easy LLM consumption. |
| `ComponentStateExtractor.java` | Per-type detailed state extraction. Given a specific component, extracts its full state including type-specific properties: JTable (columns, data, selected rows, row count), JTree (nodes, paths, expanded/selected state), JComboBox (items, selected item/index), JTextField (text, editable, caret), JCheckBox (selected), JSpinner (bounds, enabled), and common properties (name, bounds, visibility, colors, tooltip). |
| `InteractionExecutor.java` | UI action execution engine. Implements all supported actions: `click`, `double_click`, `right_click` (using `java.awt.Robot` for coordinate-based clicks), `type`/`clear` (using `setText()` or Robot key events), `select_combo` (by value or index), `select_row`, `select_tree`/`expand_tree`/`collapse_tree` (by path string), `check`/`uncheck`, and `menu` (by path like "File > Exit"). All actions execute on EDT with a 100ms settle delay before returning updated component state. |
| `ScreenshotCapture.java` | Screenshot capture using `java.awt.Robot.createScreenCapture()`. Captures the full application window or a specific named component. Returns PNG bytes encoded as base64 within a JSON wrapper (`data:image/png;base64,...`) or raw PNG bytes. Captures actual screen pixels — other overlapping windows will appear in the capture. |
| `ContrastChecker.java` | WCAG 2.1 contrast ratio validation. Walks all text-bearing components (JLabel, JTextField, JButton, etc.), computes the contrast ratio between foreground and background colors using the WCAG relative luminance formula, and flags violations against AA (4.5:1) and AAA (7:1) thresholds. Returns a report with issue count, per-component details, and total components checked. |
| `UserActionRecorder.java` | User action recording engine. Attaches global AWT event listeners to capture user interactions (mouse clicks, key presses, combo selections, tree selections) in real time. Records actions as timestamped markdown entries with component names, types, and values. Generates human-readable action recording files (`.md`) that can serve as test scripts or audit trails. Started/stopped via HTTP API endpoints. |

### `swing-mcp-lib/src/main/java/com/swingmcp/server/model/`

| File | Description |
|------|-------------|
| `ComponentNode.java` | Data transfer object representing a single node in the component tree. Fields: `id`, `type`, `name`, `parent` (ID), `bounds`, `screenBounds`, `visible`, `enabled`, `focused`, `text`, `accessibleRole`, `childCount`. Serialized to JSON by Gson. |
| `TableData.java` | Data transfer object for JTable data responses. Contains `columns` (header names), `data` (2D array of cell values), `rowCount`, `dataRange`, `totalRows`, `hasMore` (for paginated responses). |
| `ActionRequest.java` | Data transfer object for incoming action requests. Fields: `action` (string), `target` (component name or ID), `text`, `value`, `index`, `row`, `path`. Deserialized from POST `/action` JSON body. |
| `ActionResult.java` | Data transfer object for action responses. Fields: `success` (boolean), `action`, `target`, `componentState` (post-action state of the target component), `error` (null on success, error message on failure). |

---

## Demo Trading App: `swing-mcp-server/demo-app/`

A fully-featured Equity Trading MDI (Multiple Document Interface) application used as the test target for the MCP server. Features live-ticking market data, order entry tickets, portfolio navigation, and an order blotter.

| File | Description |
|------|-------------|
| `build.gradle` | Build configuration for the demo app. Uses `application` plugin with main class `com.swingmcp.demo.TradingApp`. Targets Java 8. Dependencies: `swing-mcp-lib` (project dependency) and `com.formdev:flatlaf:3.6.1` (FlatLaf dark look-and-feel). |
| `start_swing_mcp_demo_app.cmd` | Windows batch script to launch the demo app. Changes to the project root directory and runs `gradlew.bat :demo-app:run`. Convenience script for double-click launching from Windows Explorer. |

### `demo-app/src/main/java/com/swingmcp/demo/`

#### Entry Points

| File | Description |
|------|-------------|
| `TradingApp.java` | Main entry point for the MDI trading application. Sets FlatLaf dark look-and-feel, creates the main JFrame via `FrameBuilder`, initializes all panels (instrument, blotter, quotes for each sector) via `ComponentManager` and `PanelBuilder`, restores window layout from disk via `LayoutManager`, and starts the MCP server on port 9222 via `SwingMcpServer.start()`. Wires all components together through the manager singletons. |
| `TradingBlotterApp.java` | Standalone single-class demo app (not used by the main trading app). A simpler trading blotter with an order entry panel, orders table, portfolio tree, and status panel. Contains an intentional dark-mode contrast bug for testing the WCAG contrast checker. Useful as a minimal integration example. |

#### Frame & Window Management

| File | Description |
|------|-------------|
| `FrameBuilder.java` | Factory for building the main JFrame and JInternalFrames. Sets the application icon (`/icons/dollars3.png`), configures the main frame title ("Equity Trading"), creates the JDesktopPane for MDI, and wires the `StatusBarPanel` at `BorderLayout.SOUTH`. Also creates internal frames for each panel with appropriate icons and titles. |
| `LayoutManager.java` | Window layout persistence. Saves and restores positions, sizes, and states of all JInternalFrames plus the main JFrame to `~/.swingmcp/layout.properties`. Also persists and restores JTree expand/collapse state. On restore, clamps windows to screen bounds to handle resolution changes. Invoked from File > Save Layout menu and on app startup. |
| `ResourceManager.java` | Singleton-style global resource provider. Holds references to the main JFrame, JDesktopPane, `StatusBarPanel`, and an auto-incrementing order ID counter. All managers and panels access shared resources through this class. `getStatusBar()` returns the `StatusBarPanel` instance for status message display. |
| `ComponentManager.java` | Component creation and lifecycle manager. Creates and configures all JInternalFrames (portfolios with `/icons/book.png`, blotter, quote windows, order tickets). Manages frame positioning — new order tickets open cascaded 30px offset from the last. Provides `getOrder()` factory, `getBlotterPanel()`, and `showBlotter()` methods. Tracks open order tickets to avoid duplicates for the same symbol. |

#### UI Panels

| File | Description |
|------|-------------|
| `AbstractPanel.java` | Abstract base class extending `JPanel`. Provides `title` and `preferredSize` properties with getters/setters and an `update()` convenience method that calls `revalidate()` + `repaint()`. All panels (BlotterPanel, QuotePanel, InstrumentPanel) extend this class. |
| `InstrumentPanel.java` | Portfolio tree navigator. Displays a `JTree` with sector nodes (Technology, Financials, Healthcare, Energy, ETFs) and leaf symbol nodes (e.g., "AAPL - Apple Inc"). Contains a static `SYMBOL_NAMES` HashMap mapping 27 ticker symbols to full company names and a `getFullName(symbol)` static method used by order tickets. Single-click on a sector selects it and opens the corresponding quote window. Double-click on a leaf symbol opens an order ticket pre-populated with quote data. Tree root handles are visible for expand/collapse. |
| `QuotePanel.java` | Live market data table for a sector. Displays a `JTable` with 11 columns: Symbol, Last, Change, Change%, Bid, Ask, Volume, High, Low, Open, Market Cap. Ticks at 333ms (3x/second) with randomized price deltas (±0.03 per tick). Maintains `basePrices`, `currentLasts`, and `bidAskSpreads` arrays for realistic price movement. Static `SECTOR_DATA` HashMap holds initial data for all sectors. Static `activePanels` registry allows Order tickets to find live data via `findQuoteData()`. All numeric columns are right-aligned. Change/Change% columns are color-coded (green for positive, red for negative). Double-clicking a row opens an order ticket. |
| `BlotterPanel.java` | Order execution blotter displaying all placed orders. `JTable` with 11 columns: Time, Status, Side, Qty, Symbol, Order Type, Limit Price, TIF, Route, Account, Value. Uses `BlotterDataModel` (extends `AbstractTableModel`) backed by a `String[][]` array. Numeric columns (Qty, Limit Price, Value) are right-aligned. `BlotterCellRenderer` color-codes Side (green=BUY, red=SELL/SHORT), Symbol (same colors), and Status (yellow=Pending, green=Executed). `addToBlotter()` appends a row, recreates the model, and re-applies renderers. `updateOrderStatus()` changes the status cell for a given order ID (tracked via `orderPlacement` HashMap). Starts with 2 sample executed orders (AAPL BUY, MSFT SELL). |
| `StatusBarPanel.java` | Rich status bar at the bottom of the main frame. `BorderLayout` with status message label (CENTER) and system info panel (EAST). Right side displays: logged-in user name, PC hostname, timezone abbreviation, current date/time, and free/total memory in MB — all separated by `" \| "` delimiters. A 1-second `javax.swing.Timer` updates time and memory continuously. Public `setStatusText(String)` and `setStatusColor(Color)` methods for programmatic status updates. |

#### Order Ticket

| File | Description |
|------|-------------|
| `OrderFrame.java` | JInternalFrame layout for the order entry ticket. Uses null (absolute) layout with `setBounds()` positioning. Contains: (1) `ticketLabel` — instrument name at top, bold+larger font; (2) Market data row — 5 read-only, non-focusable, right-aligned JTextFields: bidField (green), bidSizeField (green), lastField (gold/DAA520), askSizeField (red), askField (red); (3) Order fields row — sideCombo, qtySpinner (JSpinner step 100), orderTypeCombo, limitPriceSpinner (JSpinner step 0.01), stopPriceSpinner (JSpinner step 0.01); (4) Account row — accountCombo, tifCombo, routeCombo; (5) JTabbedPane with Order/Allocation/Risk/History tabs; (6) Notes/Comments JTextArea; (7) Place Order and Cancel buttons. ESC key binding closes the ticket. Frame is 600x595, resizable with minimum size 600x595. All spinners have right-aligned editors with 2 decimal places for prices. |
| `Order.java` | Order ticket business logic, extends `OrderFrame`. Key behaviors: `fillControls()` populates all combos from static arrays (SIDE_COMBO, ORDER_TYPE_COMBO, ACCOUNT_COMBO, TIF_COMBO, ROUTE_COMBO), sets ticket title/label to full firm name via `InstrumentPanel.getFullName()`, initializes bid/ask sizes randomly, starts a 333ms market data timer reading from shared `benchmarkData` array (reference to `QuotePanel.dataValues[row]`), sets up click handlers (Bid/Ask/Last → limit or stop price spinner; BidSize/AskSize → quantity spinner), and builds the 9-field HTML order summary. `enablePriceSpinner()` helper blanks the spinner text when disabled and refreshes on enable. `orderTypeCombo` ItemListener enables/disables limit and stop price spinners based on selected order type. `OrderAction` inner class handles Place Order: calls `commitEdit()` on all spinners, reads current values, recalculates order value, sets side-colored border (green=BUY, red=SELL), builds 11-column blotter row, adds to blotter, registers order for async execution. `executeOrder()` shows status message and closes the ticket. `dispose()` stops the market data timer. |

#### Menu System

| File | Description |
|------|-------------|
| `MenuManager.java` | Creates the application menu bar. File menu: Save Layout, separator, Exit. View menu: Portfolios (shows instrument panel), Blotter, plus one entry per sector quote window (Technology, Financials, Healthcare, Energy, ETFs). Help menu: About. All menu items delegate to `MenuActionListener`. |
| `MenuActionListener.java` | Handles all menu item actions. "save layout" → `LayoutManager.saveLayout()`. "exit" → `System.exit(0)`. "portfolios"/"instruments" → shows instrument frame. "blotter" → shows blotter frame. Sector names → shows corresponding quote frame. "about" → displays JOptionPane with `/icons/dollars3.png` scaled to 48x48, title "Equity Trading", message "Java Swing MCP Server demo application - 2026". |

#### Support Classes

| File | Description |
|------|-------------|
| `PanelBuilder.java` | Factory that creates and configures panel instances. Builds `BlotterPanel`, `QuotePanel` (one per sector), and `InstrumentPanel`. Sets the `AppLogger` reference on each panel and calls `populatePanel()` to initialize content. |
| `RequestHandler.java` | Asynchronous order processing. `registerOrder(Order)` spawns a background thread that waits 2 seconds (simulating exchange latency), then updates the blotter status from "Pending" to "Executed" via `BlotterPanel.updateOrderStatus()` and calls `Order.executeOrder()` on the EDT. Simulates a basic order lifecycle. |
| `AppLogger.java` | Static logging utility. `info(String)` and `error(String, Exception)` write timestamped messages to stdout/stderr. `showStatus(String)` and `showStatus(String, Color)` update the status bar via `ResourceManager.getStatusBar().setStatusText()` and `setStatusColor()` on the EDT. All demo app classes use this for logging. |

### `demo-app/src/main/resources/icons/`

Application icon images used throughout the UI.

| File | Used By | Description |
|------|---------|-------------|
| `dollars3.png` | `FrameBuilder`, `MenuActionListener` | Main application icon (taskbar/title bar). Also displayed scaled to 48x48 in the Help > About dialog. |
| `book.png` | `ComponentManager` | Portfolios (instrument panel) internal frame icon. |
| `blotter.png` | `ComponentManager` | Blotter internal frame icon. |
| `ticket.png` | `ComponentManager` | Order ticket internal frame icon. |
| `md.png` | `ComponentManager` | Market data / quote panel internal frame icon. |
| `moneybag.png` | `FrameBuilder` | Used for additional frame decoration. |
| `instrument.png` | — | Original instrument panel icon (replaced by `book.png`, kept for reference). |
| `dollar.png` | — | Original app icon (replaced by `dollars3.png`, kept for reference). |
| `dollars1.png` | — | Alternative dollar icon variant. |
| `dollars2.png` | — | Alternative dollar icon variant. |
| `bookshelf.png` | — | Unused icon, available for future use. |
| `info.png` | — | Unused icon, available for future use. |
| `hammer.png` | — | Unused icon, available for future use. |
| `vault.png` | — | Unused icon, available for future use. |
| `face1.png` | — | Unused icon, available for future use. |
| `face2.png` | — | Unused icon, available for future use. |

---

## Python Orchestrator: `swing-mcp-server/orchestrator/`

A Python agent loop that connects Claude (via Anthropic API) to the embedded MCP server, enabling AI-driven interaction with the Swing application.

| File | Description |
|------|-------------|
| `requirements.txt` | Python package dependencies: `anthropic>=0.40.0` (Claude API client), `requests>=2.31.0` (HTTP client for MCP server), `Pillow>=10.0.0` (image processing for screenshots). |
| `orchestrator.py` | Main Claude agent loop. Takes a user prompt as a command-line argument, sends it to Claude with tool definitions, executes tool calls against the MCP server, and returns results. Implements context window management with message compaction at ~80K tokens. Keeps at most 2 screenshots in history to control token usage. Runs until Claude returns a final text response without tool calls. |
| `swing_client.py` | HTTP client wrapper for the MCP server API. Provides Python functions for each endpoint: `get_tree()`, `get_component(name)`, `post_action(action_dict)`, `get_screenshot()`, `get_contrast()`, `get_health()`. Handles base URL configuration, JSON parsing, and error handling. |
| `tool_definitions.py` | Claude tool schema definitions. Declares all available tools that Claude can call: `get_component_tree`, `get_component_state`, `perform_action`, `take_screenshot`, `check_contrast`, `check_health`. Each tool has a name, description, and JSON Schema for its input parameters — formatted for the Anthropic API tool_use specification. |

---

## Other Root-Level Files

| File | Description |
|------|-------------|
| `save_screenshot.py` | Standalone Python utility script. Reads MCP screenshot JSON from stdin, base64-decodes the image data, and saves it to a PNG file (default: `screenshot.png`, or filename from command-line argument). Handles base64 padding. |
| `CLAUDE.md` | Claude Code project instructions. Contains build/run commands, Java 8 compatibility rules, MCP API reference, component naming conventions, sector-symbol row mappings, order ticket behavior docs, and common MCP testing patterns. Automatically loaded by Claude Code at session start. |

---

## Files NOT Included (excluded from GitHub)

These files exist in the working directory but should not be committed:

| Pattern | Reason |
|---------|--------|
| `*.png` (root and `swing-mcp-server/` root) | Test screenshots and work artifacts — transient debugging output. |
| `screenshot.json`, `screenshot_output.json` | Raw MCP screenshot API responses saved during testing. |
| `build/` directories | Gradle build output (compiled classes, JARs). Regenerated by `./gradlew build`. |
| `.gradle/` | Gradle cache and daemon state. |
| `.idea/`, `*.iml` | IntelliJ IDEA project files. |
| `.vscode/` | VS Code workspace settings. |
| `__pycache__/`, `*.pyc` | Python bytecode cache. |
| `venv/` | Python virtual environment. |
| `.env` | Environment variables (may contain API keys). |
| `~/.swingmcp/layout.properties` | User-specific window layout persistence file (created at runtime in user home). |
