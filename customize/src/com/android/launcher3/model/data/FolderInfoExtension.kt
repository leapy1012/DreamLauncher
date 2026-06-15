package com.android.launcher3.model.data

fun FolderInfo.areContentSame(another: FolderInfo): Boolean {
    if (title != another.title) {
        return false
    }

    if (contents.size != another.contents.size) {
        return false
    }

    val size = contents.size
    for (i in 0 until size) {
        if (!contents[i].areContentSame(another.contents[i])) {
            return false
        }
    }

    return true
}