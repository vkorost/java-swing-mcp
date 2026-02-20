# Test Case: Save & Restore Window Layout

## Objective
Verify that File > Save Layout persists all window positions/sizes to disk, and on restart the app restores them. Also verify off-screen clamping.

## Preconditions
- App is running on localhost:9222
- Instrument panel and blotter are visible
- No prior `~/.swingmcp/layout.properties` file (delete if present)

## Steps

### Step 1: Take baseline screenshot
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-layout-1-baseline.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 2: Move the instrument frame to a new position
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"move","target":"instrumentFrame","x":100,"y":50}'
```

### Step 3: Move the blotter frame to a new position
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"move","target":"blotterFrame","x":0,"y":400}'
```

### Step 4: Take screenshot showing moved windows
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-layout-2-moved.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 5: Trigger Save Layout via menu click
```bash
curl -s -X POST http://localhost:9222/action -H "Content-Type: application/json" -d '{"action":"click","target":"Save Layout"}'
```

### Step 6: Verify the layout properties file was created
```bash
cat ~/.swingmcp/layout.properties
```
Expected: File exists with `main.x`, `main.y`, `frame.instrumentFrame.x`, `frame.blotterFrame.x`, etc.

### Step 7: Restart the app
Kill the running app and relaunch:
```bash
taskkill //F //IM java.exe
sleep 3
cd swing-mcp-server && ./gradlew :demo-app:run &
sleep 8
```

### Step 8: Take screenshot after restart — positions should match step 4
```bash
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-layout-3-restored.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

### Step 9: Test off-screen clamping — edit properties to extreme values
```bash
python -c "
import os
path = os.path.expanduser('~/.swingmcp/layout.properties')
with open(path, 'r') as f:
    content = f.read()
content = content.replace('frame.instrumentFrame.x=100', 'frame.instrumentFrame.x=9999')
content = content.replace('frame.instrumentFrame.y=50', 'frame.instrumentFrame.y=9999')
with open(path, 'w') as f:
    f.write(content)
print('Properties edited with off-screen coords')
"
```

### Step 10: Restart and verify clamping
```bash
taskkill //F //IM java.exe
sleep 3
cd swing-mcp-server && ./gradlew :demo-app:run &
sleep 8
curl -s http://localhost:9222/screenshot | python -c "
import sys, json, base64
data = json.load(sys.stdin)
img = data['image'].split(',',1)[1]
open('test-layout-4-clamped.png','wb').write(base64.b64decode(img))
print('Screenshot saved')
"
```

## Expected Results
- Step 5: `~/.swingmcp/layout.properties` is created with all window positions
- Step 8: Windows restore to the same positions as step 4 (screenshots match)
- Step 10: Instrument frame is visible on screen (clamped from x=9999 to within desktop bounds)
- No errors in console/log at any point
