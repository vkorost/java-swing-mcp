# Test Case: Full Workflow - Expand, Order, Place, Verify Blotter

## Objective
End-to-end test: expand a sector, double-click a leaf instrument to open order ticket, fill in order details, place the order, and verify it appears in the blotter.

## Preconditions
- App is running on localhost:9222
- Fresh state preferred (no previous orders)

## Steps

### Step 1: Take initial screenshot
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-workflow-1.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 2: Expand Energy sector via single click
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"expand_tree","target":"instrumentTree","path":"root > Energy"}'
```

### Step 3: Wait and take screenshot - Energy expanded
```bash
sleep 1
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-workflow-2.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 4: Double click XOM leaf to open order ticket
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"double_click","target":"instrumentTree","path":"root > Energy > XOM - ExxonMobil"}'
```

### Step 5: Wait and take screenshot - order ticket open
```bash
sleep 1
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-workflow-3.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 6: Verify order frame for XOM exists
```bash
curl -s http://localhost:9222/component/orderFrame_XOM
```

### Step 7: Change side to SELL
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"select_combo","target":"sideCombo","value":"SELL"}'
```

### Step 8: Change quantity to 500
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"clear","target":"qtyField"}'
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"type","target":"qtyField","text":"500"}'
```

### Step 9: Take screenshot before placing order
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-workflow-4.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 10: Place the order
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"click","target":"placeOrder"}'
```

### Step 11: Wait for order processing and take screenshot - blotter should be visible
```bash
sleep 2
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-workflow-5.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 12: Verify blotter has the order
```bash
curl -s http://localhost:9222/component/blotterTable
```

### Step 13: Get tree state to confirm it's unchanged
```bash
curl -s http://localhost:9222/component/instrumentTree
```

## Expected Results
1. Energy sector expands showing XOM, CVX, COP, SLB
2. Double-clicking XOM opens order ticket with:
   - Title: "Order Ticket - XOM"
   - Last: 108.50
   - Change: +0.90 (+0.84%) in GREEN
3. Side changed to SELL, quantity to 500
4. After placing order:
   - Order appears in blotter with XOM, SELL, 500 shares
   - Blotter frame is visible at bottom of desktop
   - Order ticket shows red border (SELL)
5. Energy sector remains expanded in the tree
