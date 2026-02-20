# Test Case: Order Ticket Shows Red for Negative Price Changes

## Objective
Verify that the order ticket correctly displays red color for instruments with negative price changes.

## Preconditions
- App is running on localhost:9222
- Instrument panel is visible

## Steps

### Step 1: Expand Technology sector
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"expand_tree","target":"instrumentTree","path":"root > Technology"}'
```

### Step 2: Double click MSFT (has negative change: -2.10, -0.51%)
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"double_click","target":"instrumentTree","path":"root > Technology > MSFT - Microsoft Corp"}'
```

### Step 3: Wait and take screenshot
```bash
sleep 1
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-neg-color-1.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 4: Verify MSFT order frame
```bash
curl -s http://localhost:9222/component/orderFrame_MSFT
```

### Step 5: Double click TSLA (also negative: -5.60, -1.56%)
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"double_click","target":"instrumentTree","path":"root > Technology > TSLA - Tesla Inc"}'
```

### Step 6: Wait and take screenshot
```bash
sleep 1
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-neg-color-2.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 7: For comparison, double click NVDA (positive: +4.20, +3.20%)
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"double_click","target":"instrumentTree","path":"root > Technology > NVDA - NVIDIA Corp"}'
```

### Step 8: Wait and take screenshot showing both red and green change labels
```bash
sleep 1
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-neg-color-3.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

## Expected Results
- MSFT order ticket: change label shows "-2.10 (-0.51%)" in RED (rgb 255,80,80)
- TSLA order ticket: change label shows "-5.60 (-1.56%)" in RED
- NVDA order ticket: change label shows "+4.20 (+3.20%)" in GREEN (rgb 0,200,0)
- Positive changes are green, negative changes are red
