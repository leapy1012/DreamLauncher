package com.coui.appcompat.springchain;

import android.util.Log;
import com.coui.appcompat.springchain.api.IChainItem;
import com.coui.appcompat.springchain.api.ISpringUpdateListener;
import com.facebook.rebound.OrigamiValueConverter;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public final class COUIGridSpringChain {
    public static final int ATTACHMENT_FRICTION = 10;
    public static final int ATTACHMENT_TENSION = 150;
    public static final int BACK_TO_BOTTOM = 2;
    public static final int BACK_TO_LEFT = 3;
    public static final int BACK_TO_RIGHT = 4;
    public static final int BACK_TO_TOP = 1;
    public static final boolean DEBUG = true;
    public static final int MAIN_FRICTION = 10;
    public static final int MAIN_TENSION = 150;
    public static final int MAX_X = 50;
    public static final int MAX_Y = 50;
    public static final String TAG = "COUIGridSpringChain";
    private List<List<IChainItem>> allItems;
    private TransCalculator backToBottomTC;
    private TransCalculator backToLeftTC;
    private TransCalculator backToRightTC;
    private TransCalculator backToTopTC;
    private float[] curSpringXArray;
    private float[] curSpringYArray;
    private int currentMaxX;
    private int currentMaxY;
    private boolean enableAutoAcc;
    private int lastMoveDirection;
    private int lastReboundDirection;
    private float[] lastSpringXArray;
    private float[] lastSpringYArray;
    private int maxXSize;
    private int maxYSize;
    private COUISpringChain springChainX;
    private COUISpringChain springChainY;
    private SpringSystem springSystem;
    private ISpringUpdateListener springUpdateListener;
    private int xDirection;
    private int yDirection;

    public static final class DefaultTransCalculator implements TransCalculator {
        @Override
        public float getTrans(int index, float distance, int direction) {
            return distance * 0.1f;
        }
    }

    public final class GridSpringListener extends SimpleSpringListener {
        private int direction;
        private int index;

        public GridSpringListener(int index, int direction) {
            this.index = index;
            this.direction = direction;
        }

        public final int getDirection() {
            return this.direction;
        }

        public final int getIndex() {
            return this.index;
        }

        @Override
        public void onSpringUpdate(Spring spring) {
            Objects.requireNonNull(spring, "spring");
            if (COUIGridSpringChain.this.yDirection != -1 && COUIGridSpringChain.this.isPortrait(this.direction)) {
                COUIGridSpringChain.this.springUpdateTranslation(this.index, (float) spring.getCurrentValue(), COUIGridSpringChain.this.yDirection);
            } else {
                if (COUIGridSpringChain.this.xDirection == -1 || COUIGridSpringChain.this.isPortrait(this.direction)) {
                    return;
                }
                COUIGridSpringChain.this.springUpdateTranslation(this.index, (float) spring.getCurrentValue(), COUIGridSpringChain.this.xDirection);
            }
        }

        public final void setDirection(int direction) {
            this.direction = direction;
        }

        public final void setIndex(int index) {
            this.index = index;
        }
    }

    public interface TransCalculator {
        float getTrans(int index, float distance, int direction);
    }

    public COUIGridSpringChain() {
        this(MAIN_TENSION, MAIN_FRICTION, ATTACHMENT_TENSION, ATTACHMENT_FRICTION,
                MAIN_TENSION, MAIN_FRICTION, ATTACHMENT_TENSION, ATTACHMENT_FRICTION,
                MAX_X, MAX_Y);
    }

    private final float calculateTranslation(int i2, float f2, int i6) {
        int i10 = i6 == 4 ? this.currentMaxX - i2 : i2;
        if (i6 == 2) {
            i10 = this.currentMaxY - i2;
        }
        if (!this.enableAutoAcc) {
            return getTrans(i10, f2, i6);
        }
        float trans = 0.0f;
        if (i10 < 0) {
            return 0.0f;
        }
        int i11 = 0;
        while (true) {
            if (!skipCumulativeCalculate(i11, i10, i6)) {
                trans += getTrans(i11, f2, i6);
            }
            if (i11 == i10) {
                return trans;
            }
            i11++;
        }
    }

    private final boolean checkItemsCount(int i2, int i6) {
        return this.allItems.size() > i2 && this.allItems.get(i2).size() > i6;
    }

    private final int getCurrentMaxSize(int i2) {
        return isPortrait(i2) ? this.currentMaxY : this.currentMaxX;
    }

    private final float getTrans(int i2, float f2, int i6) {
        if (i6 == 1) {
            TransCalculator transCalculator = this.backToTopTC;
            if (transCalculator != null) {
                return transCalculator.getTrans(i2, f2, i6);
            }
            return 0.0f;
        }
        if (i6 == 2) {
            TransCalculator transCalculator2 = this.backToBottomTC;
            if (transCalculator2 != null) {
                return transCalculator2.getTrans(i2, f2, i6);
            }
            return 0.0f;
        }
        if (i6 == 3) {
            TransCalculator transCalculator3 = this.backToLeftTC;
            if (transCalculator3 != null) {
                return transCalculator3.getTrans(i2, f2, i6);
            }
            return 0.0f;
        }
        if (i6 == 4) {
            TransCalculator transCalculator4 = this.backToRightTC;
            if (transCalculator4 != null) {
                return transCalculator4.getTrans(i2, f2, i6);
            }
            return 0.0f;
        }
        Log.e(TAG, "getDelta: error direction=" + i6);
        return 0.0f;
    }

    private final float getTranslation(float f2, IChainItem iChainItem, int i2) {
        int itemIndex;
        if (i2 == BACK_TO_TOP) {
            itemIndex = iChainItem.getItemY();
        } else if (i2 == BACK_TO_BOTTOM) {
            itemIndex = (iChainItem.getItemY() + iChainItem.getItemHeight()) - 1;
        } else if (i2 == BACK_TO_LEFT) {
            itemIndex = iChainItem.getItemX();
        } else if (i2 == BACK_TO_RIGHT) {
            itemIndex = (iChainItem.getItemX() + iChainItem.getItemWidth()) - 1;
        } else {
            itemIndex = 0;
        }
        return calculateTranslation(itemIndex, f2, i2);
    }


    public final boolean isPortrait(int i2) {
        if (i2 == 1 || i2 == 2) {
            return true;
        }
        if (i2 == 3 || i2 == 4) {
            return false;
        }
        throw new IllegalArgumentException("isPortrait: wrong dir=" + i2);
    }

    private final void removeItem(IChainItem iChainItem) {
        int itemX = iChainItem.getItemX();
        int itemY = iChainItem.getItemY();
        int itemWidth = iChainItem.getItemWidth() + itemX;
        int itemHeight = iChainItem.getItemHeight() + itemY;
        if (itemWidth > this.currentMaxX || itemHeight > this.currentMaxY) {
            Log.e(TAG, "can not remove a item that over gridSpringChain size");
            return;
        }
        while (itemY < itemHeight) {
            for (int i2 = itemX; i2 < itemWidth; i2++) {
                if (checkItemsCount(itemY, i2)) {
                    this.allItems.get(itemY).set(i2, null);
                }
            }
            itemY++;
        }
        boolean z6 = itemWidth == this.currentMaxX;
        boolean z10 = itemHeight == this.currentMaxY;
        if (z6) {
            while (z6 && this.currentMaxX > 0) {
                int i6 = this.currentMaxY;
                int i10 = 0;
                while (true) {
                    if (i10 >= i6) {
                        break;
                    }
                    if (checkItemsCount(i10, this.currentMaxX - 1) && this.allItems.get(i10).get(this.currentMaxX - 1) != null) {
                        z6 = false;
                        break;
                    }
                    i10++;
                }
                if (z6) {
                    this.currentMaxX--;
                }
            }
        }
        if (z10) {
            while (z10 && this.currentMaxY > 0) {
                int i11 = this.currentMaxX;
                int i12 = 0;
                while (true) {
                    if (i12 >= i11) {
                        break;
                    }
                    if (checkItemsCount(this.currentMaxY - 1, i12) && this.allItems.get(this.currentMaxY - 1).get(i12) != null) {
                        z10 = false;
                        break;
                    }
                    i12++;
                }
                if (z10) {
                    this.currentMaxY--;
                }
            }
        }
    }

    private final boolean skipCumulativeCalculate(int i2, int i6, int i10) {
        if (i2 != i6) {
            if (isPortrait(i10)) {
                if (i10 == 2) {
                    i2 = this.currentMaxY - i2;
                }
                int i11 = this.currentMaxX;
                for (int i12 = 0; i12 < i11; i12++) {
                    if (checkItemsCount(i2, i12) && this.allItems.get(i2).get(i12) != null) {
                        IChainItem iChainItem = this.allItems.get(i2).get(i12);
                        if (iChainItem.getSkipSpringChainCalc()) {
                            return true;
                        }
                    }
                }
            } else {
                if (i10 == 4) {
                    i2 = this.currentMaxX - i2;
                }
                int i13 = this.currentMaxY;
                for (int i14 = 0; i14 < i13; i14++) {
                    if (checkItemsCount(i14, i2) && this.allItems.get(i14).get(i2) != null) {
                        IChainItem iChainItem2 = this.allItems.get(i14).get(i2);
                        if (iChainItem2.getSkipSpringChainCalc()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private final void updateSpring(float f2, IChainItem iChainItem, int i2) {
        float translation = getTranslation(f2, iChainItem, i2);
        if (isPortrait(i2)) {
            int itemY = iChainItem.getItemY();
            float[] fArr = this.curSpringYArray;
            float f10 = translation + this.lastSpringYArray[itemY];
            fArr[itemY] = f10;
            iChainItem.updateSpringY(f10);
            return;
        }
        int itemX = iChainItem.getItemX();
        float[] fArr2 = this.curSpringXArray;
        float f11 = translation + this.lastSpringXArray[itemX];
        fArr2[itemX] = f11;
        iChainItem.updateSpringX(f11);
    }

    private final void updateSpringChain(int i2) {
        COUISpringChain cOUISpringChain = isPortrait(i2) ? this.springChainY : this.springChainX;
        int currentMaxSize = getCurrentMaxSize(i2);
        for (int size = cOUISpringChain.getAllSprings().size(); size < currentMaxSize; size++) {
            cOUISpringChain.addSpring(new GridSpringListener(size, i2));
        }
    }

    public final void addItem(IChainItem gridSpringItem) {
        IChainItem iChainItem;
        Objects.requireNonNull(gridSpringItem, "gridSpringItem");
        int itemX = gridSpringItem.getItemX();
        int itemY = gridSpringItem.getItemY();
        int itemWidth = gridSpringItem.getItemWidth() + itemX;
        int itemHeight = gridSpringItem.getItemHeight() + itemY;
        if (itemHeight > this.maxYSize || itemWidth > this.maxXSize) {
            Log.d(TAG, "can not addItem for the gridSpringChain is full");
            return;
        }
        for (int i2 = itemY; i2 < itemHeight; i2++) {
            for (int i6 = itemX; i6 < itemWidth; i6++) {
                if (checkItemsCount(i2, i6) && (iChainItem = this.allItems.get(i2).get(i6)) != null) {
                    removeItem(iChainItem);
                }
            }
        }
        while (itemY < itemHeight) {
            for (int i10 = itemX; i10 < itemWidth; i10++) {
                while (this.allItems.size() <= itemY) {
                    this.allItems.add(new ArrayList());
                }
                while (this.allItems.get(itemY).size() <= i10) {
                    this.allItems.get(itemY).add(null);
                }
                this.allItems.get(itemY).set(i10, gridSpringItem);
            }
            itemY++;
        }
        if (itemWidth > this.currentMaxX) {
            this.currentMaxX = itemWidth;
        }
        if (itemHeight > this.currentMaxY) {
            this.currentMaxY = itemHeight;
        }
    }

    public final void clearAllItems() {
        IChainItem iChainItem;
        int i2 = this.currentMaxY;
        for (int i6 = 0; i6 < i2; i6++) {
            int i10 = this.currentMaxX;
            for (int i11 = 0; i11 < i10; i11++) {
                if (checkItemsCount(i6, i11) && (iChainItem = this.allItems.get(i6).get(i11)) != null) {
                    removeItem(iChainItem);
                }
            }
        }
        this.currentMaxX = 0;
        this.currentMaxY = 0;
    }

    public final float getCurrentSpringX(int i2) {
        if (i2 <= -1) {
            return -1.0f;
        }
        float[] fArr = this.curSpringXArray;
        if (i2 < fArr.length) {
            return fArr[i2];
        }
        return -1.0f;
    }

    public final float getCurrentSpringY(int i2) {
        if (i2 <= -1) {
            return -1.0f;
        }
        float[] fArr = this.curSpringYArray;
        if (i2 < fArr.length) {
            return fArr[i2];
        }
        return -1.0f;
    }

    public final float getLastTranslationX() {
        return this.lastReboundDirection == 4 ? this.lastSpringXArray[this.currentMaxX - 1] : this.lastSpringXArray[0];
    }

    public final float getLastTranslationY() {
        return this.lastReboundDirection == 2 ? this.lastSpringYArray[this.currentMaxY - 1] : this.lastSpringYArray[0];
    }

    public final COUISpringChain getSpringChainX() {
        return this.springChainX;
    }

    public final COUISpringChain getSpringChainY() {
        return this.springChainY;
    }

    public final int isSpringSystemRunning() {
        if (isSpringSystemIdle()) {
            return 0;
        }
        return this.lastReboundDirection;
    }

    private boolean isSpringSystemIdle() {
        if (this.springSystem == null) {
            return true;
        }
        for (Spring spring : this.springSystem.getAllSprings()) {
            if (!spring.isAtRest()) {
                return false;
            }
        }
        return true;
    }

    public final void releaseSpring() {
        for (Spring spring : this.springChainY.getAllSprings()) {
            spring.setEndValue(spring.getCurrentValue());
            spring.setAtRest();
        }
        this.yDirection = -1;
        for (Spring spring : this.springChainX.getAllSprings()) {
            spring.setEndValue(spring.getCurrentValue());
            spring.setAtRest();
        }
        this.xDirection = -1;
    }

    public final void releaseSpringAndLoc() {
        releaseSpringComplete();
        resetTranslation();
    }

    public final void releaseSpringComplete() {
        for (Spring spring : this.springChainY.getAllSprings()) {
            spring.setEndValue(0.0d);
            spring.setAtRest();
        }
        this.yDirection = -1;
        for (Spring spring : this.springChainX.getAllSprings()) {
            spring.setEndValue(0.0d);
            spring.setAtRest();
        }
        this.xDirection = -1;
    }

    public final void resetTranslation() {
        Arrays.fill(this.lastSpringXArray, 0.0f);
        Arrays.fill(this.lastSpringYArray, 0.0f);
        updateMoveTranslation(0.0f, 1);
        updateMoveTranslation(0.0f, 3);
    }

    public final void setCurrentSpringX(int i2, float f2) {
        if (i2 > -1) {
            float[] fArr = this.curSpringXArray;
            if (i2 < fArr.length) {
                fArr[i2] = f2;
            }
        }
    }

    public final void setCurrentSpringY(int i2, float f2) {
        if (i2 > -1) {
            float[] fArr = this.curSpringYArray;
            if (i2 < fArr.length) {
                fArr[i2] = f2;
            }
        }
    }

    public final void setEnableAutoAcc(boolean z6) {
        this.enableAutoAcc = z6;
    }

    public final void setSpringUpdateListener(ISpringUpdateListener springUpdateListener) {
        this.springUpdateListener = Objects.requireNonNull(springUpdateListener,
                "springUpdateListener");
    }

    public final void setTranCalculator(int i2) {
        if (i2 == 1) {
            this.backToTopTC = new DefaultTransCalculator();
            return;
        }
        if (i2 == 2) {
            this.backToBottomTC = new DefaultTransCalculator();
            return;
        }
        if (i2 == 3) {
            this.backToLeftTC = new DefaultTransCalculator();
        } else if (i2 != 4) {
            Log.e(TAG, "the direction is not illegal!");
        } else {
            this.backToRightTC = new DefaultTransCalculator();
        }
    }

    public final void springUpdateTranslation(int i2, float f2, int i6) {
        IChainItem iChainItem;
        IChainItem iChainItem2;
        IChainItem iChainItem3;
        IChainItem iChainItem4;
        int i10 = 0;
        if (!isPortrait(i6)) {
            if (i6 == 4) {
                while (i10 < this.currentMaxY) {
                    if (checkItemsCount(i10, i2) && (iChainItem2 = this.allItems.get(i10).get(i2)) != null && iChainItem2.getItemY() == i10 && (iChainItem2.getItemX() + iChainItem2.getItemWidth()) - 1 == i2) {
                        iChainItem2.updateSpringX(f2);
                    }
                    i10++;
                }
            } else {
                while (i10 < this.currentMaxY) {
                    if (checkItemsCount(i10, i2) && (iChainItem = this.allItems.get(i10).get(i2)) != null && iChainItem.getItemY() == i10 && iChainItem.getItemX() == i2) {
                        iChainItem.updateSpringX(f2);
                    }
                    i10++;
                }
            }
            this.curSpringXArray[i2] = f2;
            this.lastSpringXArray[i2] = f2;
            return;
        }
        if (i6 == 2) {
            while (i10 < this.currentMaxX) {
                if (checkItemsCount(i2, i10) && (iChainItem4 = this.allItems.get(i2).get(i10)) != null && (iChainItem4.getItemY() + iChainItem4.getItemHeight()) - 1 == i2 && iChainItem4.getItemX() == i10) {
                    iChainItem4.updateSpringY(f2);
                    ISpringUpdateListener iSpringUpdateListener = this.springUpdateListener;
                    if (iSpringUpdateListener != null) {
                        iSpringUpdateListener.onUpdate(i2, f2, i6, iChainItem4);
                    }
                }
                i10++;
            }
        } else {
            while (i10 < this.currentMaxX) {
                if (checkItemsCount(i2, i10) && (iChainItem3 = this.allItems.get(i2).get(i10)) != null && iChainItem3.getItemY() == i2 && iChainItem3.getItemX() == i10) {
                    iChainItem3.updateSpringY(f2);
                    ISpringUpdateListener iSpringUpdateListener2 = this.springUpdateListener;
                    if (iSpringUpdateListener2 != null) {
                        iSpringUpdateListener2.onUpdate(i2, f2, i6, iChainItem3);
                    }
                }
                i10++;
            }
        }
        this.curSpringYArray[i2] = f2;
        this.lastSpringYArray[i2] = f2;
    }

    public final void startRebound(int i2) {
        if (i2 != 0) {
            this.lastReboundDirection = i2;
        } else {
            i2 = this.lastReboundDirection;
            if (i2 == 0) {
                return;
            }
        }
        boolean zIsPortrait = isPortrait(i2);
        updateSpringChain(4);
        updateSpringChain(2);
        COUISpringChain cOUISpringChain = zIsPortrait ? this.springChainY : this.springChainX;
        int i6 = zIsPortrait ? this.currentMaxY : this.currentMaxX;
        float[] fArr = zIsPortrait ? this.curSpringYArray : this.curSpringXArray;
        if (zIsPortrait) {
            this.yDirection = i2;
        } else {
            this.xDirection = i2;
        }
        List<Spring> allSprings = cOUISpringChain.getAllSprings();
        int i10 = -1;
        for (int i11 = 0; i11 < i6; i11++) {
            Spring spring = allSprings.get(i11);
            double d2 = fArr[i11];
            if ((d2 >= 0.0d || i11 == i6 - 1) && i10 == -1) {
                i10 = i11;
            }
            spring.setCurrentValue(d2, false);
            spring.setVelocity(0.0d);
        }
        if (i10 == -1) {
            Log.d(TAG, "startRebound failed : chain is empty");
            return;
        }
        Log.d(TAG, "startRebound : ctrIndex=:" + i10 + " ,endValue=:0.0");
        cOUISpringChain.setControlSpringIndex(i10).getControlSpring().setEndValue(0.0d);
    }

    public final void updateMoveTranslation(float f2, int i2) {
        IChainItem iChainItem;
        if (i2 != 0) {
            this.lastMoveDirection = i2;
        } else {
            i2 = this.lastMoveDirection;
        }
        updateSpringChain(4);
        updateSpringChain(2);
        if (isPortrait(i2)) {
            this.yDirection = i2;
        } else {
            this.xDirection = i2;
        }
        int i6 = this.currentMaxY;
        for (int i10 = 0; i10 < i6; i10++) {
            int i11 = this.currentMaxX;
            for (int i12 = 0; i12 < i11; i12++) {
                if (checkItemsCount(i10, i12) && (iChainItem = this.allItems.get(i10).get(i12)) != null && iChainItem.getItemY() == i10 && iChainItem.getItemX() == i12) {
                    updateSpring(f2, iChainItem, i2);
                }
            }
        }
    }

    public final void updateSpringChainConfig(double d2, double d7, boolean z6) {
        COUISpringChain cOUISpringChain = z6 ? this.springChainX : this.springChainY;
        cOUISpringChain.getMainSpringConfig().friction = OrigamiValueConverter.frictionFromOrigamiValue(d2);
        cOUISpringChain.getMainSpringConfig().tension = OrigamiValueConverter.tensionFromOrigamiValue(d7);
        cOUISpringChain.getAttachmentSpringConfig().friction = OrigamiValueConverter.frictionFromOrigamiValue(d2);
        cOUISpringChain.getAttachmentSpringConfig().friction = OrigamiValueConverter.frictionFromOrigamiValue(d7);
    }

    public COUIGridSpringChain(int i2, int i6, int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17) {
        this.xDirection = -1;
        this.yDirection = -1;
        this.curSpringYArray = new float[i17];
        this.curSpringXArray = new float[i16];
        this.lastSpringYArray = new float[i17];
        this.lastSpringXArray = new float[i16];
        this.allItems = new ArrayList();
        this.springSystem = SpringSystem.create();
        this.enableAutoAcc = true;
        COUISpringChain cOUISpringChainCreate = COUISpringChain.create(this.springSystem, i12, i13, i14, i15);
        this.springChainX = cOUISpringChainCreate;
        COUISpringChain cOUISpringChainCreate2 = COUISpringChain.create(this.springSystem, i2, i6, i10, i11);
        this.springChainY = cOUISpringChainCreate2;
        this.maxXSize = i16;
        this.maxYSize = i17;
        this.lastMoveDirection = 2;
    }

    public final void setTranCalculator(TransCalculator transCalculator, int i2) {
        Objects.requireNonNull(transCalculator, "transCalculator");
        if (i2 == 1) {
            this.backToTopTC = transCalculator;
            return;
        }
        if (i2 == 2) {
            this.backToBottomTC = transCalculator;
            return;
        }
        if (i2 == 3) {
            this.backToLeftTC = transCalculator;
        } else if (i2 != 4) {
            Log.e(TAG, "the direction is not illegal!");
        } else {
            this.backToRightTC = transCalculator;
        }
    }

    public final void releaseSpring(int i2) {
        if (isPortrait(i2)) {
            for (Spring spring : this.springChainY.getAllSprings()) {
                spring.setEndValue(spring.getCurrentValue());
                spring.setAtRest();
            }
            this.yDirection = -1;
            return;
        }
        for (Spring spring : this.springChainX.getAllSprings()) {
            spring.setEndValue(spring.getCurrentValue());
            spring.setAtRest();
        }
        this.xDirection = -1;
    }
}
