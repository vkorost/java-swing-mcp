# Test Case: Single Click Expands/Collapses Sector

## Objective
Verify that a single click on a sector node toggles its expand/collapse state after a short delay.

## Preconditions
- App is running on localhost:9222
- Instrument panel is visible

## Steps

### Step 1: Collapse Technology sector to start from known state
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"collapse_tree","target":"instrumentTree","path":"root > Technology"}'
```

### Step 2: Take screenshot - Technology should be collapsed
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-single-click-1.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 3: Single click on Technology sector node (uses path-aware clicking)
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"click","target":"instrumentTree","path":"root > Technology"}'
```

### Step 4: Wait for single-click timer (300ms + buffer)
```bash
sleep 1
```

### Step 5: Take screenshot - Technology should now be expanded (single click toggled it)
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-single-click-2.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 6: Single click again on Technology to collapse it
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"click","target":"instrumentTree","path":"root > Technology"}'
```

### Step 7: Wait and take screenshot - Technology should be collapsed again
```bash
sleep 1
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-single-click-3.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

## Expected Results
- Single click on collapsed sector expands it after ~300ms delay
- Single click on expanded sector collapses it after ~300ms delay
- Toggle is visible in screenshots (child nodes appear/disappear)
