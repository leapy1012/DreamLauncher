package com.android.launcher3.big.add;

import com.android.launcher3.model.data.ItemInfoWithIcon;

public abstract class HxyItemInfoWithIcon extends ItemInfoWithIcon {
    public boolean isSelect = false;

    public HxyItemInfoWithIcon(ItemInfoWithIcon info) {
        super(info);
    }

    protected HxyItemInfoWithIcon() {
    }
}
