# Test Case: Double Click Sector Opens Quote Screen (No Toggle)

## Objective
Verify that double-clicking a sector node opens a quote screen for that sector without toggling its expand/collapse state.

## Preconditions
- App is running on localhost:9222
- Instrument panel is visible

## Steps

### Step 1: Expand Technology sector to known state
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"expand_tree","target":"instrumentTree","path":"root > Technology"}'
```

### Step 2: Take screenshot - Technology expanded, no quote screen open
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-dbl-quote-1.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 3: Double click on Technology sector node
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"double_click","target":"instrumentTree","path":"root > Technology"}'
```

### Step 4: Wait for UI to settle
```bash
sleep 1
```

### Step 5: Take screenshot - quote screen should be open, Technology should still be expanded
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-dbl-quote-2.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 6: Verify quote frame exists
```bash
curl -s http://localhost:9222/component/quoteFrame_Technology
```

### Step 7: Verify the tree state - Technology should still be expanded (no toggle from double click)
```bash
curl -s http://localhost:9222/component/instrumentTree
```

## Expected Results
- Double clicking Technology sector opens a "Technology" quote screen
- The quote screen shows a table with AAPL, MSFT, GOOGL, NVDA, META, AMZN, TSLA, AMD
- Technology sector remains expanded (double click does NOT toggle)
- The single-click timer is cancelled (no delayed toggle fires)
