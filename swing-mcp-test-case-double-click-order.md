# Test Case: Double Click Leaf Opens Order Ticket with Correct Data

## Objective
Verify that double-clicking a leaf instrument node opens an order ticket pre-populated with the correct symbol and quote data.

## Preconditions
- App is running on localhost:9222
- Instrument panel is visible
- Technology sector is expanded

## Steps

### Step 1: Expand Technology sector
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"expand_tree","target":"instrumentTree","path":"root > Technology"}'
```

### Step 2: Take initial screenshot
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-dbl-order-1.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 3: Double click on AAPL leaf node
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"double_click","target":"instrumentTree","path":"root > Technology > AAPL - Apple Inc"}'
```

### Step 4: Wait for UI
```bash
sleep 1
```

### Step 5: Take screenshot - order ticket should be open with AAPL data
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-dbl-order-2.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 6: Verify order frame exists and has correct symbol
```bash
curl -s http://localhost:9222/component/orderFrame_AAPL
```

### Step 7: Test another sector - expand Financials and double click JPM
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"expand_tree","target":"instrumentTree","path":"root > Financials"}'
sleep 0.5
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"double_click","target":"instrumentTree","path":"root > Financials > JPM - JPMorgan Chase"}'
```

### Step 8: Wait and take screenshot
```bash
sleep 1
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-dbl-order-3.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 9: Verify JPM order frame
```bash
curl -s http://localhost:9222/component/orderFrame_JPM
```

## Expected Results
- Double clicking AAPL opens order ticket titled "Order Ticket - AAPL"
- Order ticket shows Last: 242.50, Change: +1.85 (+0.77%) in green
- Quantity defaults to 100
- Double clicking JPM opens a separate order ticket with JPM data (Last: 258.40)
- Both order tickets are independently visible
