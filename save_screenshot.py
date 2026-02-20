import sys, json, base64

data = json.load(sys.stdin)
img = data['image']
# Add padding if needed
padding = 4 - len(img) % 4
if padding != 4:
    img += '=' * padding
raw = base64.b64decode(img)

outfile = sys.argv[1] if len(sys.argv) > 1 else 'screenshot.png'
with open(outfile, 'wb') as f:
    f.write(raw)
print('Saved', len(raw), 'bytes to', outfile)
