import re, sys
path = sys.argv[1] if len(sys.argv) > 1 else r'D:\Work\AOSP\D960\DreamLauncher\oppo_apps.xml'
xml = open(path, encoding='utf-8').read()
for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    t, x1, y1, x2, y2 = m.group(1), *map(int, m.groups()[1:])
    if 'coui' in t.lower() or 'demo' in t.lower() or 'COUI' in t:
        cx, cy = (x1+x2)//2, (y1+y2)//2
        print(f'{t!r} center=({cx},{cy}) bounds=[{x1},{y1}][{x2},{y2}]')
if 'coui' not in xml.lower():
    print('COUI not found in dump; searching all apps label...')
    for m in re.finditer(r'content-desc="([^"]*)"[^>]*clickable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        t = m.group(1)
        if t and len(t) < 40:
            x1,y1,x2,y2 = map(int, m.groups()[1:])
            if y1 > 200:
                print(f'desc={t!r} center=({(x1+x2)//2},{(y1+y2)//2})')
