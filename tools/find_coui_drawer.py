import subprocess, time, re

SERIAL = "3L1G41E6ELSEADU1"
OUT = r"D:\Work\AOSP\D960\DreamLauncher"

def adb(*args):
    return subprocess.run(["adb", "-s", SERIAL] + list(args), capture_output=True, text=True)

adb("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(1)
# Open all apps - swipe up from bottom
adb("shell", "input", "swipe", "540", "2200", "540", "800", "500")
time.sleep(2)
adb("shell", "input", "text", "COUI%20Demo")
time.sleep(1.5)
adb("shell", "uiautomator", "dump", "/sdcard/drawer_coui.xml")
adb("pull", "/sdcard/drawer_coui.xml", f"{OUT}/drawer_coui.xml")
xml = open(f"{OUT}/drawer_coui.xml", encoding="utf-8").read()
for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    t = m.group(1)
    if t and ("coui" in t.lower() or "demo" in t.lower() or "greendream" in t.lower()):
        x1,y1,x2,y2 = map(int, m.groups()[1:])
        print(t, (x1+x2)//2, (y1+y2)//2)
