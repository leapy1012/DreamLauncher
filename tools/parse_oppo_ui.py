import re, sys
xml = open(r'D:\Work\AOSP\D960\DreamLauncher\oppo_ui2.xml', encoding='utf-8').read()
for m in re.finditer(r'text="([^"]*)"[^>]*long-clickable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    t, x1, y1, x2, y2 = m.group(1), *map(int, m.groups()[1:])
    if t and y1 > 400:
        cx, cy = (x1+x2)//2, (y1+y2)//2
        print(f'{t!r:20} center=({cx},{cy}) bounds=[{x1},{y1}][{x2},{y2}]'.encode('ascii','backslashreplace').decode())
