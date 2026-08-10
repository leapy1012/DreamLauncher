package com.coui.appcompat.springchain.api;

import android.view.View;


public interface IChainItem {
    int getItemHeight();

    int getItemWidth();

    int getItemX();

    int getItemY();

    boolean getSkipSpringChainCalc();

    void setItemHeight(int i2);

    void setItemSize(int i2, int i6);

    void setItemWidth(int i2);

    void setItemX(int i2);

    void setItemXY(int i2, int i6);

    void setItemY(int i2);

    void setProxyView(View view);

    void setSkipSpringChainCalc(boolean z6);

    void updateSpringX(float f2);

    void updateSpringY(float f2);
}
