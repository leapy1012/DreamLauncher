import re, subprocess, time, sys

SERIAL = "0123456789ABCDEF"
OUT = r"D:\Work\AOSP\D960\DreamLauncher"
PKG = "com.android.launcher3"

def adb(*args):
    r = subprocess.run(["adb", "-s", SERIAL] + list(args), capture_output=True, text=True)
    return r.stdout + r.stderr

def log(msg):
    print(msg, flush=True)

adb("logcat", "-c")
adb("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(1)
adb("shell", "input", "keyevent", "KEYCODE_BACK")
time.sleep(0.5)
adb("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(2)

adb("shell", "uiautomator", "dump", "/sdcard/mtk_home.xml")
adb("pull", "/sdcard/mtk_home.xml", f"{OUT}/mtk_home.xml")
xml = open(f"{OUT}/mtk_home.xml", encoding="utf-8", errors="replace").read()

# Find workspace app icons (BubbleTextView / clickable with text, in workspace area)
candidates = []
for m in re.finditer(
    r'(?:text|content-desc)="([^"]+)"[^>]*(?:resource-id="[^"]*")?[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',
    xml):
    t = m.group(1).strip()
    if not t or t in ("Search", "Phone", "Messages", "Browser", "Camera"):
        continue
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
    # workspace icons typically y > 400 and not dock
    if cy < 400 or cy > 1900:
        continue
    if x2 - x1 < 40 or y2 - y1 < 40:
        continue
    candidates.append((t, cx, cy, x2 - x1, y2 - y1))

log(f"Candidates ({len(candidates)}):")
for c in candidates[:15]:
    log(f"  {c}")

if not candidates:
    log("No workspace icons found!")
    sys.exit(1)

# Prefer LTC Menu or first app-like icon
target = None
for c in candidates:
    if c[0] == "LTC Menu":
        target = c
        break
for c in candidates:
    if target is None and c[0] not in ("Off", "On", "Home", "Mobile data", "Bluetooth",
            "Not playing", "Airplane mode", "Ring", "Vibrate", "Mute", "Automatic"):
        if "Menu" in c[0] or len(c[0]) > 3:
            target = c
            break
if target is None:
    target = candidates[0]

name, cx, cy, w, h = target
log(f"\nLong-pressing '{name}' at ({cx}, {cy})")
adb("shell", "input", "swipe", str(cx), str(cy), str(cx), str(cy), "1500")
time.sleep(1.5)

adb("shell", "screencap", "-p", "/sdcard/mtk_resize_test.png")
adb("pull", "/sdcard/mtk_resize_test.png", f"{OUT}/mtk_resize_test.png")
log("Screenshot: mtk_resize_test.png")

logs = adb("logcat", "-d", "-s", "AppIconResizeFrame:D", "IconResizeHelper:D")
log("\n--- Logcat (resize) ---")
print(logs if logs.strip() else "(no resize logs)")

# Parse frame position from log if available
handle_x, handle_y = cx + 78, cy + 78
for line in logs.splitlines():
    if "frame at" in line:
        import re as re2
        m = re2.search(r"frame at (\d+),(\d+) (\d+)x(\d+)", line)
        if m:
            fx, fy, fw, fh = map(int, m.groups())
            handle_x = fx + fw - 20
            handle_y = fy + fh - 20
            break
log(f"\nDragging handle from ({handle_x},{handle_y}) to expand...")
adb("shell", "input", "swipe", str(handle_x), str(handle_y), str(handle_x + 250), str(handle_y + 250), "800")
time.sleep(1)
adb("shell", "screencap", "-p", "/sdcard/mtk_resize_after.png")
adb("pull", "/sdcard/mtk_resize_after.png", f"{OUT}/mtk_resize_after.png")
log("Screenshot after drag: mtk_resize_after.png")

logs2 = adb("logcat", "-d", "-s", "AppIconResizeFrame:D", "IconResizeHelper:D")
log("\n--- Logcat after drag ---")
print(logs2 if logs2.strip() else "(no resize logs)")
