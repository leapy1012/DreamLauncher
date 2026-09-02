import subprocess, time

SERIAL = "3L1G41E6ELSEADU1"
OUT = r"D:\Work\AOSP\D960\DreamLauncher"

def adb(*args):
    return subprocess.run(["adb", "-s", SERIAL] + list(args), capture_output=True, text=True)

adb("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(1)
for _ in range(4):
    adb("shell", "input", "swipe", "900", "1300", "100", "1300", "350")
    time.sleep(0.8)
# COUI Demo center
adb("shell", "input", "swipe", "294", "409", "294", "409", "1500")
time.sleep(1.5)
adb("shell", "screencap", "-p", "/sdcard/coui_demo_lp.png")
adb("pull", "/sdcard/coui_demo_lp.png", f"{OUT}/oppo_coui_demo_longpress.png")
print("saved oppo_coui_demo_longpress.png")
