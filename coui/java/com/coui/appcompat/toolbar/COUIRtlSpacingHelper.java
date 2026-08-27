package com.coui.appcompat.toolbar;

public class COUIRtlSpacingHelper {
    public static final int UNDEFINED = Integer.MIN_VALUE;
    private int mLeft = 0;
    private int mRight = 0;
    private int mStart = Integer.MIN_VALUE;
    private int mEnd = Integer.MIN_VALUE;
    private int mExplicitLeft = 0;
    private int mExplicitRight = 0;
    private boolean mIsRtl = false;
    private boolean mIsRelative = false;

    public int getEnd() {
        return this.mIsRtl ? this.mLeft : this.mRight;
    }

    public int getLeft() {
        return this.mLeft;
    }

    public int getRight() {
        return this.mRight;
    }

    public int getStart() {
        return this.mIsRtl ? this.mRight : this.mLeft;
    }

    public void setAbsolute(int i2, int i6) {
        this.mIsRelative = false;
        if (i2 != Integer.MIN_VALUE) {
            this.mExplicitLeft = i2;
            this.mLeft = i2;
        }
        if (i6 != Integer.MIN_VALUE) {
            this.mExplicitRight = i6;
            this.mRight = i6;
        }
    }

    public void setDirection(boolean z6) {
        if (z6 == this.mIsRtl) {
            return;
        }
        this.mIsRtl = z6;
        if (!this.mIsRelative) {
            this.mLeft = this.mExplicitLeft;
            this.mRight = this.mExplicitRight;
            return;
        }
        if (z6) {
            int i2 = this.mEnd;
            if (i2 == Integer.MIN_VALUE) {
                i2 = this.mExplicitLeft;
            }
            this.mLeft = i2;
            int i6 = this.mStart;
            if (i6 == Integer.MIN_VALUE) {
                i6 = this.mExplicitRight;
            }
            this.mRight = i6;
            return;
        }
        int i10 = this.mStart;
        if (i10 == Integer.MIN_VALUE) {
            i10 = this.mExplicitLeft;
        }
        this.mLeft = i10;
        int i11 = this.mEnd;
        if (i11 == Integer.MIN_VALUE) {
            i11 = this.mExplicitRight;
        }
        this.mRight = i11;
    }

    public void setRelative(int i2, int i6) {
        this.mStart = i2;
        this.mEnd = i6;
        this.mIsRelative = true;
        if (this.mIsRtl) {
            if (i6 != Integer.MIN_VALUE) {
                this.mLeft = i6;
            }
            if (i2 != Integer.MIN_VALUE) {
                this.mRight = i2;
                return;
            }
            return;
        }
        if (i2 != Integer.MIN_VALUE) {
            this.mLeft = i2;
        }
        if (i6 != Integer.MIN_VALUE) {
            this.mRight = i6;
        }
    }
}
