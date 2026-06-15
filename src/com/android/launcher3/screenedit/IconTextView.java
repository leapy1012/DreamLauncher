package com.android.launcher3.screenedit;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.android.launcher3.util.DimenUtils;
import com.android.launcher3.R;

public class IconTextView extends AppCompatTextView {

    public static final float PRESSED_SCALE = 0.92f;

    // 图标大小
    public int iconSize;
    // 图标大小的 dp 值
    public float iconSizeInDp;
    // 文本大小的 sp 值
    public float textSizeInSp;
    // 复合 drawable 的间距 dp 值
    public float compoundDrawablePaddingInDp;
    // 是否选中状态
    public boolean isSelected;

    public IconTextView(Context context) {
        this(context, (AttributeSet) null, 0);
    }

    public final void drawSelectedIcon(Canvas canvas) {
        if (this.isSelected) {
            canvas.save();
            int selectedIconSize = DimenUtils.dipToPx(getContext(), 15.0f);
            canvas.translate(((float) ((getWidth() + this.iconSize) - selectedIconSize)) / 2.0f, 0.0f);
            Drawable drawable = getContext().getDrawable(R.drawable.ic_selected);
            drawable.setBounds(0, 0, selectedIconSize, selectedIconSize);
            drawable.draw(canvas);
            canvas.restore();
        }
    }

    public final void setViewScale(float scale) {
        setScaleX(scale);
        setScaleY(scale);
    }

    public final void updateScaleBasedOnPressState() {
        if (isPressed()) {
            setViewScale(PRESSED_SCALE);
        } else {
            setViewScale(1.0f);
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        ViewGroup viewGroup = (ViewGroup) getParent();
        viewGroup.setPadding(getPaddingLeft(), getResources().getDimensionPixelSize(R.dimen.overview_panel_main_menu_padding_top), getPaddingRight(), getPaddingBottom());
        viewGroup.requestLayout();
        this.iconSize = (int) (getResources().getDisplayMetrics().density * this.iconSizeInDp);
        setTextSize(this.textSizeInSp);
        setCompoundDrawablePadding(DimenUtils.dipToPx(getContext(), this.compoundDrawablePaddingInDp));
        Drawable[] compoundDrawables = getCompoundDrawables();
        setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], compoundDrawables[2], compoundDrawables[3]);
        invalidate();
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawSelectedIcon(canvas);
    }

    public void setCompoundDrawables(@Nullable Drawable left, @Nullable Drawable top, @Nullable Drawable right, @Nullable Drawable bottom) {
        if (left != null) {
            left.setBounds(0, 0, this.iconSize, this.iconSize);
        }
        if (top != null) {
            top.setBounds(0, 0, this.iconSize, this.iconSize);
        }
        if (right != null) {
            right.setBounds(0, 0, this.iconSize, this.iconSize);
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, this.iconSize, this.iconSize);
        }
        super.setCompoundDrawables(left, top, right, bottom);
    }

    public void setPressed(boolean isPressed) {
        super.setPressed(isPressed);
        updateScaleBasedOnPressState();
    }

    public void setSelect(boolean isSelected) {
        this.isSelected = isSelected;
        invalidate();
    }

    public IconTextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public IconTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.iconSize = -1;
        this.isSelected = false;
        // 获取图标默认大小
        int defaultIconSize = context.getResources().getDimensionPixelSize(R.dimen.icon_textview_default_icon_size);
        this.iconSize = defaultIconSize;
        this.iconSizeInDp = (float) DimenUtils.pxToDp(context, (float) defaultIconSize);
        this.textSizeInSp = (float) DimenUtils.pxToSp(context, getTextSize());
        this.compoundDrawablePaddingInDp = (float) DimenUtils.pxToDp(context, (float) getCompoundDrawablePadding());
        Drawable[] compoundDrawables = getCompoundDrawables();
        setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], compoundDrawables[2], compoundDrawables[3]);
    }
}
