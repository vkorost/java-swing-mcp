# Test Case: Expand/Collapse Icons Visible on Sector Nodes

## Objective
Verify that the portfolio tree (InstrumentPanel) displays +/- expand/collapse icons (root handles) on sector nodes.

## Preconditions
- App is running on localhost:9222
- Instrument panel is visible

## Steps

### Step 1: Take initial screenshot to verify tree is visible
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-expand-icons-1.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 2: Get tree component state to verify it exists
```bash
curl -s http://localhost:9222/component/instrumentTree
```

### Step 3: Collapse a sector node that may be expanded (Technology)
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"collapse_tree","target":"instrumentTree","path":"root > Technology"}'
```

### Step 4: Take screenshot - verify the collapse icon (-) is NOT shown, expand icon (+) IS shown
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-expand-icons-2.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 5: Expand the Technology sector
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"expand_tree","target":"instrumentTree","path":"root > Technology"}'
```

### Step 6: Take screenshot - verify the expand icon is now a collapse icon and child nodes are visible
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-expand-icons-3.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

## Expected Results
- Sector nodes (Technology, Financials, Healthcare, Energy, ETFs) show +/- handle icons
- Collapsed sectors show + icon
- Expanded sectors show - icon and child instrument nodes are visible
- Leaf nodes (individual instruments) do NOT show +/- icons
