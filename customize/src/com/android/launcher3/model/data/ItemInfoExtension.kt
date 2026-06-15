package com.android.launcher3.model.data

fun ItemInfo.areContentSame(another: ItemInfo): Boolean {
    if (this::class != another::class) {
        return false
    }

    if (targetComponent?.flattenToShortString() !=
        another.targetComponent?.flattenToShortString()) {
        return false
    }

    if (this is ItemInfoWithIcon && another is ItemInfoWithIcon) {
        return this.bitmap == another.bitmap
    }

    return true
}