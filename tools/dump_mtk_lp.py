import subprocess, time

SERIAL = "0123456789ABCDEF"

def adb(*args):
    subprocess.run(["adb", "-s", SERIAL] + list(args), capture_output=True)

adb("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(2)
# LTC Menu from prior test
adb("shell", "input", "swipe", "380", "1598", "380", "1598", "1500")
time.sleep(0.8)
adb("shell", "uiautomator", "dump", "/sdcard/mtk_lp.xml")
subprocess.run(["adb", "-s", SERIAL, "pull", "/sdcard/mtk_lp.xml", r"D:\Work\AOSP\D960\DreamLauncher\mtk_lp.xml"])
xml = open(r"D:\Work\AOSP\D960\DreamLauncher\mtk_lp.xml", encoding="utf-8", errors="replace").read()
for line in xml.split(">"):
    if "resize" in line.lower() or "AppIcon" in line or "Floating" in line:
        print(line[:200])
