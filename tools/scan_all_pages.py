import re, subprocess, time

SERIAL = "3L1G41E6ELSEADU1"
OUT = r"D:\Work\AOSP\D960\DreamLauncher"

def adb(*args):
    return subprocess.run(["adb", "-s", SERIAL] + list(args), capture_output=True, text=True)

adb("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(1)
all_apps = []
for i in range(5):
    adb("shell", "uiautomator", "dump", f"/sdcard/wp{i}.xml")
    adb("pull", f"/sdcard/wp{i}.xml", f"{OUT}/wp{i}.xml")
    xml = open(f"{OUT}/wp{i}.xml", encoding="utf-8").read()
    for m in re.finditer(r'(?:text|content-desc)="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        t = m.group(1).strip()
        if not t:
            continue
        x1,y1,x2,y2 = map(int, m.groups()[1:])
        cx, cy = (x1+x2)//2, (y1+y2)//2
        if "coui" in t.lower() or "demo" in t.lower() or "greendream" in t.lower():
            all_apps.append((i, t, cx, cy))
    if i < 4:
        adb("shell", "input", "swipe", "900", "1300", "100", "1300", "350")
        time.sleep(0.8)
print("Found:", all_apps)
