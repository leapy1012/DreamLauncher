#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate coui values-ko / values-zh-rCN strings from ColorOS APK dumps + hand translations."""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(r"D:\Work\AOSP\D960\DreamLauncher")
COUI_RES = ROOT / "coui" / "res"
REF = Path(r"D:\Work\AOSP\D960\Reference")

BASE_FILES = [
    COUI_RES / "values" / "strings.xml",
    COUI_RES / "values" / "strings_lockview.xml",
    COUI_RES / "values" / "strings_touchsearch.xml",
]

# Prefer richer dumps first.
LOCALE_SOURCES = {
    "ko": [
        REF / "com.oplus.settings" / "resources" / "res" / "values-ko" / "strings.xml",
        REF / "com.coloros.calculator.apktool" / "res" / "values-ko" / "strings.xml",
        REF / "coloros-systemui-apktool" / "res" / "values-ko" / "strings.xml",
        REF / "com.oplus.launcher" / "resources" / "res" / "values-ko" / "strings.xml",
    ],
    "zh-rCN": [
        REF / "com.oplus.settings" / "resources" / "res" / "values-zh-rCN" / "strings.xml",
        REF / "com.coloros.calculator.apktool" / "res" / "values-zh-rCN" / "strings.xml",
        REF / "coloros-systemui-apktool" / "res" / "values-zh-rCN" / "strings.xml",
        REF / "com.oplus.launcher" / "resources" / "res" / "values-zh-rCN" / "strings.xml",
    ],
}

# Keys that must stay identical (asset paths / class names / fonts).
KEEP_AS_IS_PREFIXES = (
    "coui_lottie_",
    "coui_loading_rotating",
    "coui_view_inflater_class",
    "bottom_sheet_behavior",
    "ttf_path",
)

KEEP_AS_IS = {
    "coui_view_inflater_class",
    "bottom_sheet_behavior",
    "coui_lottie_loading_large_dark_json",
    "coui_lottie_loading_large_json",
    "coui_lottie_loading_large_light_json",
    "coui_lottie_loading_small_dark_json",
    "coui_lottie_loading_small_json",
    "coui_lottie_loading_small_light_json",
    "coui_loading_rotating_json",
    "coui_loading_rotating_json_dark",
    "coui_loading_rotating_json_light",
    "ttf_path",
    "loading_button_dots",
    "coui_install_load_progress_apostrophe",
    "fast_scroller_dots",
    "red_dot_more",
    "coui_touchsearch_dot",
    "well_character",
    # Latin keypad labels stay Latin.
    "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz",
}

# Lunar / Chinese calendar terms stay Chinese even in Korean (ColorOS does this).
KEEP_CHINESE = {
    "coui_lunar_leap_string",
    "lunar_january", "lunar_februry", "lunar_march", "lunar_april",
    "lunar_may", "lunar_june", "lunar_july", "lunar_august",
    "lunar_september", "lunar_october", "lunar_november", "lunar_december",
}

# Manual fallbacks when dump misses a key.
FALLBACK = {
    "ko": {
        "red_dot_description": "새 알림",
        "coui_accessibility_unselected": "선택되지 않음",
        "item_view_role_description": "탭",
        "indicator_content_description": "페이지 표시기",
        "indicator_content_end": "두 번 탭하여 선택",
        "minus_content": "빼기",
        "coui_toolar_close_button_description": "제목 간략히 표시",
        "coui_toolar_expand_button_description": "제목 전체 표시",
        "morning": "오전",
        "afternoon": "오후",
        "coui_hour_abbreviation": "시",
        "coui_minute_abbreviation": "분",
        "coui_hour": "시",
        "coui_minute": "분",
        "ymdw": "yyyy년 M월 d일 EEEE",
        "ymdwhm": "yyyy년 M월 d일 EEEE HH:mm",
        "ymdwshm": "yyyy년 M월 d일 EEE HH:mm",
        "coui_year": "년",
        "coui_month": "월",
        "coui_day": "일",
        "coui_time_picker_day": "일",
        "coui_time_picker_today": "오늘",
        "coui_accessibility_checked": "선택됨",
        "coui_accessibility_partchecked": "일부 선택됨",
        "coui_accessibility_unchecked": "선택되지 않음",
        "coui_accessibility_select": "선택",
        "coui_accessibility_select_all": "모두 선택",
        "coui_accessibility_switch": "스위치",
        "coui_inputview_show_password_description": "비밀번호 표시",
        "coui_slide_delete": "삭제",
        "switch_loading": "로드 중",
        "switch_off": "끔",
        "switch_on": "켬",
        "coui_install_download_progress_textview": "다운로드",
        "coui_loading_view_access_string": "로드 중",
        "coui_seek_bar_role_description": "슬라이더",
        "dialog_cancel": "취소",
        "dialog_ok": "확인",
        "coui_search_clear_button_description": "텍스트 지우기",
        "coui_search_edit_box_description": "검색창",
        "coui_search_share_button_description": "공유",
        "coui_search_view_cancel": "취소",
        "coui_search_view_text": "취소",
        "bottom_recommended_header_title": "다음을 찾고 있을 수 있습니다:",
        "support_abc_searchview_description_clear": "검색어 지우기",
        "support_abc_searchview_description_search": "검색",
        "support_abc_searchview_description_submit": "검색어 제출",
        "support_abc_searchview_description_voice": "음성 검색",
        "abc_capital_on": "켜짐",
        "abc_capital_off": "꺼짐",
        "abc_action_menu_overflow_description": "추가 옵션",
        "coui_toolbar_navigation_back": "뒤로",
        "coui_tool_tips_delete_icon_description": "이 팁 닫기",
        "coui_allow_text": "동의하고 계속",
        "coui_lock_screen_next_button_description": "다음",
        "coui_notification_close_button_description": "알림 닫기",
        "coui_panel_please_input_text": "입력",
        "coui_reject_text": "종료",
        "coui_security_alertdialog_checkbox_msg": "다시 표시하지 않음",
        "coui_security_alertdailog_privacy": "개인정보 처리방침",
        "coui_security_alertdailog_statement": "자세한 내용은 %1$s을(를) 탭하여 확인하세요.",
        "coui_guide_dialog_known_text": "확인",
        "coui_guide_dialog_next_text": "다음",
        "coui_guide_dialog_skip_text": "건너뛰기",
        "support_abc_toolbar_collapse_description": "접기",
        "byteShort": "&#160;B",
        "byteSpeed": "&#160;B/s",
        "gigaByteSpeed": "&#160;GB/s",
        "gigabyteShort": "&#160;GB",
        "kiloByteSpeed": "&#160;KB/s",
        "kilobyteShort": "&#160;KB",
        "megaByteSpeed": "&#160;MB/s",
        "megabyteShort": "&#160;MB",
        "more_time_download": "&#160;천",
        "most_time_download": "&#160;만",
        "petaByteSpeed": "&#160;PB/s",
        "petabyteShort": "&#160;PB",
        "teraByteSpeed": "&#160;TB/s",
        "terabyteShort": "&#160;TB",
        "coui_number_keyboard_delete": "삭제",
        "coui_numeric_keyboard_sure": "완료",
        "coui_simple_lock_access_description": "비밀번호 입력 영역입니다. x자리를 입력했습니다. 총 y자리를 입력해야 합니다.",
        "lockscreen_access_pattern_area": "패턴 잠금 영역",
        "lockscreen_access_pattern_cell_added_verbose": "점 %1$s 추가됨",
        "lockscreen_access_pattern_cleared": "패턴이 지워짐",
        "lockscreen_access_pattern_detected": "패턴 완료",
        "lockscreen_access_pattern_start": "지금 패턴을 그리고 있습니다",
        "coui_touchsearch_description": "%s 문자 선택됨",
        "Jan": "1월", "Feb": "2월", "Mar": "3월", "Apr": "4월",
        "May": "5월", "Jun": "6월", "Jul": "7월", "Aug": "8월",
        "Sep": "9월", "Oct": "10월", "Nov": "11월", "Dec": "12월",
    },
    "zh-rCN": {
        "red_dot_description": "新通知",
        "coui_accessibility_unselected": "未选中",
        "item_view_role_description": "标签",
        "indicator_content_description": "页面指示器",
        "indicator_content_end": "双击即可选择",
        "minus_content": "减",
        "coui_toolar_close_button_description": "收起标题",
        "coui_toolar_expand_button_description": "展开完整标题",
        "morning": "上午",
        "afternoon": "下午",
        "coui_hour_abbreviation": "时",
        "coui_minute_abbreviation": "分",
        "coui_hour": "时",
        "coui_minute": "分",
        "ymdw": "yyyy年M月d日 EEEE",
        "ymdwhm": "yyyy年M月d日 EEEE HH:mm",
        "ymdwshm": "yyyy年M月d日 EEE HH:mm",
        "coui_year": "年",
        "coui_month": "月",
        "coui_day": "日",
        "coui_time_picker_day": "日",
        "coui_time_picker_today": "今天",
        "coui_accessibility_checked": "已勾选",
        "coui_accessibility_partchecked": "部分勾选",
        "coui_accessibility_unchecked": "未勾选",
        "coui_accessibility_select": "选择",
        "coui_accessibility_select_all": "全选",
        "coui_accessibility_switch": "开关",
        "coui_inputview_show_password_description": "显示密码",
        "coui_slide_delete": "删除",
        "switch_loading": "加载中",
        "switch_off": "关闭",
        "switch_on": "开启",
        "coui_install_download_progress_textview": "下载",
        "coui_loading_view_access_string": "加载中",
        "coui_seek_bar_role_description": "滑块",
        "dialog_cancel": "取消",
        "dialog_ok": "确定",
        "coui_search_clear_button_description": "清除文本",
        "coui_search_edit_box_description": "搜索框",
        "coui_search_share_button_description": "分享",
        "coui_search_view_cancel": "取消",
        "coui_search_view_text": "取消",
        "bottom_recommended_header_title": "您可能在找：",
        "support_abc_searchview_description_clear": "清除查询",
        "support_abc_searchview_description_search": "搜索",
        "support_abc_searchview_description_submit": "提交查询",
        "support_abc_searchview_description_voice": "语音搜索",
        "abc_capital_on": "开启",
        "abc_capital_off": "关闭",
        "abc_action_menu_overflow_description": "更多选项",
        "coui_toolbar_navigation_back": "返回",
        "coui_tool_tips_delete_icon_description": "关闭此提示",
        "coui_allow_text": "同意并继续",
        "coui_lock_screen_next_button_description": "下一步",
        "coui_notification_close_button_description": "关闭通知",
        "coui_panel_please_input_text": "输入",
        "coui_reject_text": "退出",
        "coui_security_alertdialog_checkbox_msg": "不再提醒",
        "coui_security_alertdailog_privacy": "隐私政策",
        "coui_security_alertdailog_statement": "点按并查看%1$s了解更多信息。",
        "coui_guide_dialog_known_text": "知道了",
        "coui_guide_dialog_next_text": "下一步",
        "coui_guide_dialog_skip_text": "跳过",
        "support_abc_toolbar_collapse_description": "收起",
        "byteShort": "&#160;B",
        "byteSpeed": "&#160;B/s",
        "gigaByteSpeed": "&#160;GB/s",
        "gigabyteShort": "&#160;GB",
        "kiloByteSpeed": "&#160;KB/s",
        "kilobyteShort": "&#160;KB",
        "megaByteSpeed": "&#160;MB/s",
        "megabyteShort": "&#160;MB",
        "more_time_download": "&#160;千",
        "most_time_download": "&#160;万",
        "petaByteSpeed": "&#160;PB/s",
        "petabyteShort": "&#160;PB",
        "teraByteSpeed": "&#160;TB/s",
        "terabyteShort": "&#160;TB",
        "coui_number_keyboard_delete": "删除",
        "coui_numeric_keyboard_sure": "完成",
        "coui_simple_lock_access_description": "密码输入区域。您已输入 x 位。总共需要输入 y 位。",
        "lockscreen_access_pattern_area": "图案锁区域",
        "lockscreen_access_pattern_cell_added_verbose": "已添加圆点 %1$s",
        "lockscreen_access_pattern_cleared": "图案已清除",
        "lockscreen_access_pattern_detected": "图案已完成",
        "lockscreen_access_pattern_start": "正在绘制图案",
        "coui_touchsearch_description": "已选择字符 %s",
        "Jan": "1月", "Feb": "2月", "Mar": "3月", "Apr": "4月",
        "May": "5月", "Jun": "6月", "Jul": "7月", "Aug": "8月",
        "Sep": "9月", "Oct": "10月", "Nov": "11月", "Dec": "12月",
    },
}

PLURAL_FALLBACK = {
    "ko": {
        "coui_page_indicator_description": {
            "one": "%2$d페이지 중 %1$d페이지",
            "other": "%2$d페이지 중 %1$d페이지",
        },
        "red_dot_with_number_description": {
            "one": "새 알림 %d개",
            "other": "새 알림 %d개",
        },
    },
    "zh-rCN": {
        "coui_page_indicator_description": {
            "one": "第 %1$d 页，共 %2$d 页",
            "other": "第 %1$d 页，共 %2$d 页",
        },
        "red_dot_with_number_description": {
            "one": "%d 条新通知",
            "other": "%d 条新通知",
        },
    },
}

# Launcher ColorOS missing strings for ko / zh-rCN gaps.
LAUNCHER_FALLBACK = {
    "ko": {
        "coloros_add_app_to_home": "새 앱을 홈 화면에 추가",
        "coloros_show_app_suggestions": "앱 추천 표시",
        "coloros_drawer_default_view": "기본 보기",
        "coloros_drawer_layout": "서랍 레이아웃",
        "coloros_drawer_show_app_names": "앱 이름 표시",
        "coloros_home_screen_in_use": "사용 중",
        "coloros_switch_home_screen_mode_message": "홈 화면 모드를 전환하면 아이콘 배열이 바뀔 수 있습니다. 전환할까요?",
        "coloros_layout_hide_icon_names": "아이콘 이름 숨기기",
        "coloros_layout_hide_icon_names_summary": "홈 화면 레이아웃이 변경됩니다.",
        "launcher_icon_fallen_title": "아이콘 끌어내리기 제스처",
        "launcher_icon_fallen_sub_title": "한 손으로 홈 화면 앱을 엽니다.",
        "launcher_icon_fallen_tip": "화면 왼쪽 또는 오른쪽에서 위로 밀어 앱 아이콘을 끌어내리세요. 손가락을 떼지 말고 원하는 아이콘까지 계속 민 다음 손을 떼면 앱이 열립니다.",
        "launcher_icon_fallen_toast": "끌어내릴 앱 아이콘이 없습니다",
        "coloros_floating_tab_all": "전체",
        "coloros_floating_tab_category": "카테고리",
        "coloros_all_apps_search_hint": "앱 검색",
        "coloros_drawer_menu": "추가 옵션",
        "coloros_drawer_columns_four": "4열",
        "coloros_drawer_columns_five": "5열",
        "coloros_drawer_app_sort_select": "선택",
        "coloros_drawer_app_sort_sort": "정렬",
        "coloros_drawer_category_settings": "설정",
        "coloros_drawer_select_cancel": "취소",
        "coloros_drawer_add_app_to_launcher": "홈 화면에 추가",
        "coloros_drawer_select_uninstall": "제거",
        "coloros_drawer_select_max": "아이콘은 최대 %1$d개까지 선택할 수 있습니다",
        "coloros_drawer_select_none_uninstallable": "선택한 앱은 제거할 수 없습니다",
        "coloros_category_recently_installed": "최근 설치됨",
        "coloros_alphabet_sort": "이름순",
        "coloros_frequence_sort": "사용량순",
        "coloros_time_sort": "설치 시간순",
    },
    "zh-rCN": {
        "coloros_all_apps_search_hint": "搜索应用",
        "coloros_drawer_menu": "更多选项",
        "coloros_drawer_app_sort_select": "选择",
        "coloros_drawer_app_sort_sort": "排序",
        "coloros_drawer_category_settings": "设置",
        "coloros_drawer_select_cancel": "取消",
        "coloros_drawer_add_app_to_launcher": "添加到主屏幕",
        "coloros_drawer_select_uninstall": "卸载",
        "coloros_drawer_select_max": "最多可选择 %1$d 个图标",
        "coloros_drawer_select_none_uninstallable": "所选应用无法卸载",
        "coloros_category_recently_installed": "最近安装",
        "coloros_alphabet_sort": "按名称",
        "coloros_frequence_sort": "按使用频率",
        "coloros_time_sort": "按安装时间",
    },
}

LAUNCHER_PLURALS = {
    "ko": {
        "coloros_drawer_select_count": {
            "one": "%d개 선택됨",
            "other": "%d개 선택됨",
        },
    },
    "zh-rCN": {
        "coloros_drawer_select_count": {
            "one": "已选择 %d 项",
            "other": "已选择 %d 项",
        },
    },
}


def parse_resources(path: Path) -> dict:
    text = path.read_text(encoding="utf-8", errors="replace")
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
    out: dict = {}
    # Self-closing empties first: <string name="x" />
    for m in re.finditer(r'<string\s+name="([^"]+)"([^>/]*?)/>', text):
        out[m.group(1)] = ("string", m.group(2).strip(), "")
    # Normal strings: attrs must not contain '/' so we never cross a self-close.
    for m in re.finditer(
        r'<string\s+name="([^"]+)"([^>/]*)>(.*?)</string>', text, re.S
    ):
        out[m.group(1)] = ("string", m.group(2).strip(), m.group(3))
    for m in re.finditer(
        r'<plurals\s+name="([^"]+)"([^>]*)>(.*?)</plurals>', text, re.S
    ):
        items = re.findall(
            r'<item\s+quantity="([^"]+)">(.*?)</item>', m.group(3), re.S
        )
        out[m.group(1)] = ("plurals", m.group(2).strip(), items)
    for m in re.finditer(
        r'<string-array\s+name="([^"]+)"([^>]*)>(.*?)</string-array>', text, re.S
    ):
        items = re.findall(r"<item>(.*?)</item>", m.group(3), re.S)
        out[m.group(1)] = ("array", m.group(2).strip(), items)
    return out


def merge_sources(paths: list[Path]) -> dict:
    merged: dict = {}
    for p in paths:
        if not p.exists():
            print(f"  skip missing: {p}")
            continue
        d = parse_resources(p)
        print(f"  loaded {p} ({len(d)} keys)")
        for k, v in d.items():
            if k not in merged:
                merged[k] = v
    return merged


def should_keep_as_is(name: str) -> bool:
    if name in KEEP_AS_IS or name in KEEP_CHINESE:
        return True
    return name.startswith(KEEP_AS_IS_PREFIXES) or name.startswith(
        "coui_touchsearch_character_"
    )


def translate_value(name: str, kind: str, en_payload, locale: str, dump: dict):
    if should_keep_as_is(name):
        return en_payload

    if kind == "string":
        if name in dump and dump[name][0] == "string":
            return dump[name][2]
        return FALLBACK[locale].get(name, en_payload)

    if kind == "plurals":
        if name in dump and dump[name][0] == "plurals":
            return dump[name][2]
        fb = PLURAL_FALLBACK[locale].get(name)
        if fb:
            # preserve quantities from English
            return [(q, fb.get(q, fb.get("other", v))) for q, v in en_payload]
        return en_payload

    if kind == "array":
        if name in dump and dump[name][0] == "array":
            return dump[name][2]
        # Translate literal month names if present; keep @string refs.
        out = []
        for item in en_payload:
            s = item.strip()
            if s.startswith("@string/"):
                out.append(s)
            else:
                out.append(FALLBACK[locale].get(s, s))
        return out

    return en_payload


def attrs_str(attrs: str) -> str:
    return f" {attrs}" if attrs else ""


def emit_string(name: str, attrs: str, value: str) -> str:
    a = attrs_str(attrs)
    if value == "":
        return f'    <string name="{name}"{a} />'
    return f'    <string name="{name}"{a}>{value}</string>'


def emit_resources(base: dict, locale: str, dump: dict):
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- Generated COUI {locale} strings. Prefer ColorOS dump values when available. -->",
        "<resources>",
    ]
    missing = []
    for name, (kind, attrs, payload) in base.items():
        translated = translate_value(name, kind, payload, locale, dump)
        if kind == "string":
            if (
                not should_keep_as_is(name)
                and name not in dump
                and name not in FALLBACK[locale]
                and translated == payload
                and (payload or "").strip()
            ):
                missing.append(name)
            lines.append(emit_string(name, attrs, translated if isinstance(translated, str) else ""))
        elif kind == "plurals":
            lines.append(f'    <plurals name="{name}"{attrs_str(attrs)}>')
            for q, v in translated:
                lines.append(f'        <item quantity="{q}">{v}</item>')
            lines.append("    </plurals>")
        elif kind == "array":
            lines.append(f'    <string-array name="{name}"{attrs_str(attrs)}>')
            for item in translated:
                lines.append(f"        <item>{item}</item>")
            lines.append("    </string-array>")
    lines.append("</resources>")
    lines.append("")
    return "\n".join(lines), missing


def write_launcher_locales():
    en = parse_resources(ROOT / "res-hxy" / "values" / "strings.xml")
    wanted = [
        k
        for k in en
        if k.startswith("coloros_")
        or k.startswith("launcher_icon_fallen_")
    ]
    # zh-rCN already exists; append only missing keys.
    zh_path = ROOT / "res-hxy" / "values-zh-rCN" / "strings.xml"
    zh = parse_resources(zh_path)
    zh_missing = [k for k in wanted if k not in zh]
    if zh_missing:
        text = zh_path.read_text(encoding="utf-8")
        insert = []
        for k in zh_missing:
            kind, attrs, payload = en[k]
            if kind == "string":
                val = LAUNCHER_FALLBACK["zh-rCN"].get(k, payload)
                insert.append(
                    f'    <string name="{k}"{attrs_str(attrs)}>{val}</string>'
                )
            elif kind == "plurals":
                fb = LAUNCHER_PLURALS["zh-rCN"].get(k, {})
                insert.append(f'    <plurals name="{k}"{attrs_str(attrs)}>')
                for q, v in payload:
                    insert.append(
                        f'        <item quantity="{q}">{fb.get(q, fb.get("other", v))}</item>'
                    )
                insert.append("    </plurals>")
        block = (
            "\n    <!-- Missing ColorOS / Icon Fallen strings -->\n"
            + "\n".join(insert)
            + "\n"
        )
        if "</resources>" in text:
            text = text.replace("</resources>", block + "</resources>")
            zh_path.write_text(text, encoding="utf-8")
            print(f"Appended {len(zh_missing)} keys to {zh_path}")
        else:
            print("WARN: no </resources> in zh-rCN")
    else:
        print("zh-rCN launcher strings already complete")

    # Create values-ko for res-hxy
    ko_dir = ROOT / "res-hxy" / "values-ko"
    ko_dir.mkdir(parents=True, exist_ok=True)
    ko_path = ko_dir / "strings.xml"
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!-- Korean translations for ColorOS / Icon Fallen launcher strings. -->",
        "<resources>",
    ]
    for k in wanted:
        kind, attrs, payload = en[k]
        if kind == "string":
            val = LAUNCHER_FALLBACK["ko"].get(k, payload)
            lines.append(f'    <string name="{k}"{attrs_str(attrs)}>{val}</string>')
        elif kind == "plurals":
            fb = LAUNCHER_PLURALS["ko"].get(k, {})
            lines.append(f'    <plurals name="{k}"{attrs_str(attrs)}>')
            for q, v in payload:
                lines.append(
                    f'        <item quantity="{q}">{fb.get(q, fb.get("other", v))}</item>'
                )
            lines.append("    </plurals>")
    lines.append("</resources>")
    lines.append("")
    ko_path.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {ko_path} ({len(wanted)} keys)")


def main():
    base: dict = {}
    for f in BASE_FILES:
        part = parse_resources(f)
        print(f"base {f.name}: {len(part)}")
        base.update(part)
    print(f"total base keys: {len(base)}")

    for locale, paths in LOCALE_SOURCES.items():
        print(f"\n=== {locale} ===")
        dump = merge_sources(paths)
        hits = sum(1 for k in base if k in dump)
        print(f"dump hits: {hits}/{len(base)}")
        xml, missing = emit_resources(base, locale, dump)
        out_dir = COUI_RES / f"values-{locale}"
        out_dir.mkdir(parents=True, exist_ok=True)
        out_path = out_dir / "strings.xml"
        out_path.write_text(xml, encoding="utf-8")
        print(f"wrote {out_path}")
        if missing:
            print(f"untranslated leftover ({len(missing)}): {missing}")

    print("\n=== launcher locales ===")
    write_launcher_locales()


if __name__ == "__main__":
    main()
