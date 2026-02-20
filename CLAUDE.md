# Claude Code Instructions for Swing MCP Project

## Project Overview
An Equity Trading demo app built with Java Swing, with an embedded MCP (Model Context Protocol) HTTP server on localhost:9222 for programmatic UI interaction and testing.

## Project Structure
```
c:\WORKSPACE\Claude.Swing.Test.MCP\
├── swing-mcp-server/              # Main project (Gradle multi-module)
│   ├── build.gradle               # Root build file
│   ├── settings.gradle            # Includes swing-mcp-lib + demo-app
│   ├── gradlew / gradlew.bat      # Gradle 8.5 wrapper
│   ├── swing-mcp-lib/             # MCP server library (SwingMcpServer)
│   │   └── src/main/java/com/swingmcp/server/
│   └── demo-app/                  # Trading app demo
│       └── src/main/java/com/swingmcp/demo/
│           ├── TradingApp.java          # Main entry point
│           ├── FrameBuilder.java        # Main frame + internal frames
│           ├── ComponentManager.java    # Component creation + management
│           ├── ResourceManager.java     # Global resources (frame, desktop, status bar)
│           ├── LayoutManager.java       # Window position persistence
│           ├── PanelBuilder.java        # Panel factory
│           ├── InstrumentPanel.java     # Portfolio tree (sectors + symbols)
│           ├── QuotePanel.java          # Live market data table (ticks 3x/sec)
│           ├── BlotterPanel.java        # Order blotter (11 columns)
│           ├── OrderFrame.java          # Order ticket layout (null layout)
│           ├── Order.java               # Order ticket logic + click handlers
│           ├── StatusBarPanel.java      # Status bar with system info
│           ├── MenuManager.java         # Menu bar setup
│           ├── MenuActionListener.java  # Menu action handlers
│           ├── RequestHandler.java      # Async order processing
│           ├── AppLogger.java           # Logging + status bar
│           ├── AbstractPanel.java       # Base panel class
│           └── TradingBlotterApp.java   # Standalone blotter demo
└── orchestrator/                  # Python MCP orchestrator
    └── (requires anthropic, requests, Pillow)
```

## Build & Run
```bash
# Build (from swing-mcp-server directory)
./gradlew build

# Run (starts Swing app + MCP server on localhost:9222)
./gradlew :demo-app:run

# Kill old app (MUST do before relaunch — port 9222 bind conflict)
taskkill //F //IM java.exe
# Wait 3+ seconds before relaunching
```

## Java 8 Compatibility — CRITICAL
The system runs Java 8 (1.8.0_202). **All code must be Java 8 compatible.**

### NOT allowed:
- Switch expressions (`case X ->`) — use `switch/case/break`
- Pattern matching instanceof — use traditional `instanceof` + explicit cast
- Records — use regular classes
- `Map.of()`, `Set.of()`, `List.of()` — use `new HashMap<>()`, `Arrays.asList()`
- `String.readAllBytes()` — use manual byte reading
- `URLDecoder.decode(String, Charset)` — use `URLDecoder.decode(String, "UTF-8")`
- Text blocks (`"""`) — use string concatenation
- `var` keyword — use explicit types

### Allowed:
- Lambdas (Java 8 feature)
- Streams (Java 8 feature)
- `Optional` (Java 8 feature)

## MCP Server HTTP API (localhost:9222)
The app embeds an HTTP server for programmatic UI interaction.

### Key Endpoints
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/screenshot` | GET | Capture screenshot (returns base64 PNG in JSON) |
| `/action` | POST | Execute UI actions (click, type, select, etc.) |
| `/component/{name}` | GET | Get component state |
| `/tree` | GET | Get component hierarchy |
| `/health` | GET | Health check |

### Action Types
```json
{"action": "click|double_click|type|select_combo|select_tree|expand_tree|menu", "target": "componentName"}
```
- `click/double_click`: Add `"row": N` for JTable rows
- `select_tree`: Use `"path": "root > Technology"` format
- `select_combo`: Use `"value": "LIMIT"` or `"index": 1`
- `type`: Use `"text": "content"` for text fields

### Component Names (Order Ticket)
| Name | Type | Description |
|------|------|-------------|
| `orderBidField` | JTextField | Bid price (green, read-only) |
| `orderBidSizeField` | JTextField | Bid size (green, read-only) |
| `orderLastField` | JTextField | Last price (gold, read-only) |
| `orderAskSizeField` | JTextField | Ask size (red, read-only) |
| `orderAskField` | JTextField | Ask price (red, read-only) |
| `orderSideCombo` | JComboBox | BUY/SELL/SHORT |
| `orderQtySpinner` | JSpinner | Quantity (step 100) |
| `orderTypeCombo` | JComboBox | MARKET/LIMIT/STOP/STOP LIMIT |
| `orderLimitPrice` | JSpinner | Limit price (step 0.01) |
| `orderStopPrice` | JSpinner | Stop price (step 0.01) |
| `orderAccountCombo` | JComboBox | Account selection |
| `orderTifCombo` | JComboBox | DAY/GTC/IOC/FOK |
| `orderRouteCombo` | JComboBox | SMART/NYSE/NASDAQ/ARCA/BATS |
| `placeOrderButton` | JButton | Place Order |
| `closeButton` | JButton | Cancel (closes ticket) |

### Other Component Names
- `instrumentTree` — Portfolio JTree
- `quoteTable_{sector}` — Quote tables (Technology, Financials, Healthcare, Energy, ETFs)
- `blotterTable` — Order blotter table

### Sector Row Indices (for double_click row parameter)
- **Technology**: 0=AAPL, 1=MSFT, 2=GOOGL, 3=NVDA, 4=META, 5=AMZN, 6=TSLA, 7=AMD
- **Financials**: 0=JPM, 1=BAC, 2=GS, 3=MS, 4=WFC, 5=C
- **Healthcare**: 0=JNJ, 1=UNH, 2=PFE, 3=ABBV, 4=MRK
- **Energy**: 0=XOM, 1=CVX, 2=COP, 3=SLB
- **ETFs**: 0=SPY, 1=QQQ, 2=IWM, 3=DIA

## Order Ticket Behavior

### Order Type → Price Field Enabling
| Order Type | Limit Price | Stop Price |
|------------|-------------|------------|
| MARKET     | disabled (blank) | disabled (blank) |
| LIMIT      | enabled     | disabled (blank) |
| STOP       | disabled (blank) | enabled    |
| STOP LIMIT | enabled     | enabled    |

### MD Field Click Handlers
- **Bid/Last/Ask click** → populates limit price spinner (if enabled), else stop price spinner (if enabled)
- **Bid Size/Ask Size click** → populates quantity spinner

### JSpinner Notes
- MCP `/component` endpoint does NOT return spinner `.getValue()` — use screenshots to verify
- `commitEdit()` is called before reading values during order placement (already implemented)

## Blotter Columns
Time | Status | Side | Qty | Symbol | Order Type | Limit Price | TIF | Route | Account | Value

Right-aligned: Qty, Limit Price, Value. Color-coded: Side (green=BUY, red=SELL), Status (yellow=Pending, green=Executed).

## UI Details
- **App icon**: `/icons/dollars3.png`
- **Portfolios icon**: `/icons/book.png`
- **Status bar**: Bottom of main frame — status message | user | host | timezone | datetime | memory (updates 1s)
- **Quote panels**: Tick at 333ms (3x/sec). Right-aligned numeric columns. Color-coded changes.
- **Order ticket**: 600x595, resizable. ESC key closes. Bold + larger symbol label with full firm name.
- **Menus**: File (Save Layout, Exit), View (Portfolios, Blotter, sector quotes), Help (About)
- **About dialog**: dollars3.png icon 48x48, title "Equity Trading", message "Java Swing MCP Server demo application - 2026"

## Common MCP Testing Patterns

### Open order ticket for a symbol
```bash
# 1. Select sector in tree
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" \
  -d '{"action":"select_tree","target":"instrumentTree","path":"root > ETFs"}'

# 2. Double-click symbol row in quote table
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" \
  -d '{"action":"double_click","target":"quoteTable_ETFs","row":1}'
```

### Place a LIMIT order
```bash
# Set order type to LIMIT
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" \
  -d '{"action":"select_combo","target":"orderTypeCombo","value":"LIMIT"}'

# Click bid to set limit price
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" \
  -d '{"action":"click","target":"orderBidField"}'

# Place order
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" \
  -d '{"action":"click","target":"placeOrderButton"}'
```

### Take and save screenshot
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img_data = data['image'].split(',',1)[1]
with open('/tmp/screenshot.png','wb') as f:
    f.write(base64.b64decode(img_data))
"
```
