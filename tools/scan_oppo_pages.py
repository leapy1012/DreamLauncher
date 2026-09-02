import re, subprocess, time, os

SERIAL = "3L1G41E6ELSEADU1"
OUT = r"D:\Work\AOSP\D960\DreamLauncher"

def adb(*args):
    return subprocess.run(["adb", "-s", SERIAL] + list(args), capture_output=True, text=True)

def dump_page(name):
    adb("shell", "uiautomator", "dump", f"/sdcard/{name}.xml")
    adb("pull", f"/sdcard/{name}.xml", f"{OUT}/{name}.xml")
    xml = open(f"{OUT}/{name}.xml", encoding="utf-8").read()
    hits = []
    for m in re.finditer(r'(?:text|content-desc)="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        t = m.group(1)
        if t and ("coui" in t.lower() or "demo" in t.lower()):
            x1, y1, x2, y2 = map(int, m.groups()[1:])
            hits.append((t, (x1+x2)//2, (y1+y2)//2))
    return hits

adb("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(1)
all_hits = []
for i in range(4):
    hits = dump_page(f"oppo_page_{i}")
    print(f"page {i}: {hits}")
    all_hits.extend(hits)
    if i < 3:
        adb("shell", "input", "swipe", "900", "1300", "100", "1300", "350")
        time.sleep(1)
print("total COUI hits:", all_hits)
