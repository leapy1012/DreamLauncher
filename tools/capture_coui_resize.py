import subprocess, time, os

SERIAL = "3L1G41E6ELSEADU1"
OUT = r"D:\Work\AOSP\D960\DreamLauncher"

def adb(*args):
    return subprocess.run(["adb", "-s", SERIAL] + list(args), capture_output=True, text=True)

def go_home():
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(1)

def swipe_to_page3():
    go_home()
    for _ in range(3):
        adb("shell", "input", "swipe", "900", "1300", "100", "1300", "350")
        time.sleep(0.8)

def long_press(x, y, ms=1200):
    adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), str(ms))

def screenshot(name):
    adb("shell", "screencap", "-p", f"/sdcard/{name}.png")
    adb("pull", f"/sdcard/{name}.png", f"{OUT}/{name}.png")
    print("saved", f"{OUT}/{name}.png")

swipe_to_page3()
# COUI Expressive center from scan
long_press(663, 544, 1500)
time.sleep(1.5)
screenshot("oppo_coui_longpress")
adb("shell", "uiautomator", "dump", "/sdcard/coui_lp.xml")
adb("pull", "/sdcard/coui_lp.xml", f"{OUT}/coui_lp.xml")
