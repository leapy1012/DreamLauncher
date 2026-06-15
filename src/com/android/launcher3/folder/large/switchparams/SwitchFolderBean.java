package com.android.launcher3.folder.large.switchparams;

import android.view.View;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;

public class SwitchFolderBean {
    private int cellX;
    private int cellY;
    private HxyLargeFolderIcon child;
    private Launcher launcher;
    private int screenId;
    private int spanX;
    private int spanY;
    private CellLayout targetLayout;

    public SwitchFolderBean(Launcher launcher2, CellLayout targetLayout2, HxyLargeFolderIcon child2, int screenId2, int cellX2, int cellY2, int spanX2, int spanY2) {
        this.launcher = launcher2;
        this.targetLayout = targetLayout2;
        this.child = child2;
        this.screenId = screenId2;
        this.cellX = cellX2;
        this.cellY = cellY2;
        this.spanX = spanX2;
        this.spanY = spanY2;
    }

    public void release() {
        this.launcher = null;
        this.targetLayout = null;
        this.child = null;
        this.screenId = -1;
        this.cellX = -1;
        this.cellY = -1;
        this.spanX = -1;
        this.spanY = -1;
    }

    public void updateFolderData() {
        updateFolderData(this.launcher, this.targetLayout, this.child, this.screenId, this.cellX, this.cellY, this.spanX, this.spanY);
    }

    private static void updateFolderData(Launcher launcher2, CellLayout cellLayout, View child2, int screenId2, int cellX2, int cellY2, int spanX2, int spanY2) {
        int i = cellX2;
        int i2 = cellY2;
        cellLayout.markCellsAsUnoccupiedForView(child2);
        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child2.getLayoutParams();
        lp.setCellX(i);
        lp.setCellY(i2);
        lp.setTmpCellX(i);
        lp.setTmpCellY(i2);
        lp.cellHSpan = spanX2;
        lp.cellVSpan = spanY2;
        ItemInfo info = (ItemInfo) child2.getTag();
        info.cellX = lp.getCellX();
        info.cellY = lp.getCellY();
        info.spanX = lp.cellHSpan;
        info.spanY = lp.cellVSpan;
        info.minSpanX = info.spanX;
        info.minSpanY = info.spanY;
        cellLayout.markCellsAsOccupiedForView(child2);
        launcher2.getModelWriter().modifyItemInDatabase(info, -100, screenId2, info.cellX, info.cellY, info.spanX, info.spanY);
    }

    public Launcher getLauncher() {
        return this.launcher;
    }

    public CellLayout getTargetLayout() {
        return this.targetLayout;
    }

    public HxyLargeFolderIcon getChild() {
        return this.child;
    }

    public int getScreenId() {
        return this.screenId;
    }

    public int getCellX() {
        return this.cellX;
    }

    public int getCellY() {
        return this.cellY;
    }

    public int getSpanX() {
        return this.spanX;
    }

    public int getSpanY() {
        return this.spanY;
    }
}
