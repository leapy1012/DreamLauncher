"""Capture LTC Menu at 1x1, 2x1, 1x2, 2x2 after morph drawable fix."""
import re, subprocess, time, sys

SERIAL = "0123456789ABCDEF"
OUT = r"D:\Work\AOSP\D960\DreamLauncher"

def adb(*args):
    return subprocess.run(["adb", "-s", SERIAL] + list(args), capture_output=True, text=True)

def home():
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(1.5)

def screenshot(name):
    adb("shell", "screencap", "-p", f"/sdcard/{name}.png")
    adb("pull", f"/sdcard/{name}.png", f"{OUT}/{name}.png")
    print("saved", name)

def find_ltc():
    adb("shell", "uiautomator", "dump", "/sdcard/home.xml")
    adb("pull", "/sdcard/home.xml", f"{OUT}/home.xml")
    xml = open(f"{OUT}/home.xml", encoding="utf-8", errors="replace").read()
    for m in re.finditer(r'text="LTC Menu"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        x1,y1,x2,y2 = map(int, m.groups())
        return (x1+x2)//2, (y1+y2)//2
    return None

def long_press(x, y):
    adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), "1500")

def drag_handle(fx, fy, tx, ty):
    adb("shell", "input", "swipe", str(fx), str(fy), str(tx), str(ty), "700")

def tap_outside():
    adb("shell", "input", "tap", "600", "800")

home()
pt = find_ltc()
if not pt:
    print("LTC Menu not found"); sys.exit(1)
cx, cy = pt
print("LTC at", cx, cy)

# Close any open frame
tap_outside()
time.sleep(0.5)

# 1x1 - reset by dragging handle inward if needed; screenshot current
screenshot("mtk_step_1x1")

def resize_via_handle(cx, cy, dx, dy):
    long_press(cx, cy)
    time.sleep(1)
    # parse frame from log not available - use offset from icon
    adb("logcat", "-c")
    long_press(cx, cy)
    time.sleep(1)
    # handle ~ bottom-right of icon area
    hx, hy = cx + 80, cy + 80
    drag_handle(hx, hy, hx + dx, hy + dy)
    time.sleep(0.8)
    tap_outside()
    time.sleep(0.5)
    home()
    time.sleep(1)
    pt = find_ltc()
    if pt:
        return pt
    return cx, cy

# Try 2x1
cx, cy = find_ltc()
resize_via_handle(cx, cy, 250, 0)
screenshot("mtk_step_2x1")

# 1x2 from 2x1 - drag down
cx, cy = find_ltc()
resize_via_handle(cx, cy, 0, 250)
screenshot("mtk_step_1x2")

# 2x2
cx, cy = find_ltc()
resize_via_handle(cx, cy, 250, 250)
screenshot("mtk_step_2x2")

print("done")
