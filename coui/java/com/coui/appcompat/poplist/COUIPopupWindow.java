package com.coui.appcompat.poplist;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.PopupWindow;

import androidx.core.content.ContextCompat;

import com.coui.appcompat.R;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.uiutil.ShadowUtils;

public class COUIPopupWindow extends PopupWindow {
    private Context mContext;
    protected boolean mIsOutLineBackgroundInPopupWindow = true;
    private boolean mSetElevationInPopupwindow;
    protected WindowSpacingControlHelper mWindowSpacingControlHelper = new WindowSpacingControlHelper();

    public COUIPopupWindow(Context context) {
        this(context, null);
    }

    public COUIPopupWindow(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.popupWindowStyle);
    }

    public COUIPopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Widget_COUI_PopupWindow);
    }

    public COUIPopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPopupWindow(context);
    }

    public COUIPopupWindow(View contentView, int width, int height) {
        this(contentView, width, height, false);
    }

    public COUIPopupWindow(View contentView, int width, int height, boolean focusable) {
        super(contentView, width, height, focusable);
        initPopupWindow(contentView.getContext());
    }

    private void initPopupWindow(Context context) {
        mContext = context;
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.couiPopupWindowBackground});
        initPopupWindowBackground(context, a);
        a.recycle();
        setClippingEnabled(false);
        // Oppo: window elevation stays 0; card owns the shadow via OPlus light-source.
        // MTK AOSP: keep style android:popupElevation — zeroing it breaks TYPE_APPLICATION_SUB_PANEL
        // menus on launcher overlay panels (Quick Glance), where the window needs Z to composite.
        if (ShadowUtils.checkOPlusViewElevationSDK()) {
            setElevation(0f);
        }
        setExitTransition(null);
        setEnterTransition(null);
        setAnimationStyle(R.style.Animation_COUI_PopupListWindow);
    }

    public void addSpacingControlUtil(Context context, int resId, WindowSpacingControlHelper.AnchorViewTypeEnum anchorViewTypeEnum) {
        if (context != null) {
            addSpacingControlUtil(context.getResources().getDimensionPixelSize(resId), anchorViewTypeEnum);
        }
    }

    public void addSpacingControlUtil(int spacing, WindowSpacingControlHelper.AnchorViewTypeEnum anchorViewTypeEnum) {
        mWindowSpacingControlHelper.addAnchorViewSpacingMap(spacing, anchorViewTypeEnum);
    }

    public int getAnchorViewSpacing(WindowSpacingControlHelper.AnchorViewTypeEnum anchorViewTypeEnum) {
        return mWindowSpacingControlHelper.isUtilMapInit()
                ? mWindowSpacingControlHelper.getAnchorViewSpacing(anchorViewTypeEnum)
                : 0;
    }

    public int getAnchorViewSpacing(View view, WindowSpacingControlHelper.AnchorViewTypeEnum anchorViewTypeEnum) {
        return mWindowSpacingControlHelper.isUtilMapInit()
                ? mWindowSpacingControlHelper.getAnchorViewSpacing(view, anchorViewTypeEnum)
                : 0;
    }

    public void initElevationInPopupwindow() {
        if (!mSetElevationInPopupwindow || getContentView() == null) {
            return;
        }
        setBackgroundDrawable(null);
        View content = getContentView();
        // Fullscreen list root: elevate RoundFrameLayout cards only (Oppo). Elevating the
        // root flattens MTK AOSP shadows and clips them.
        if (content instanceof COUIPopupMenuRootView) {
            ((COUIPopupMenuRootView) content).ensureCardShadows();
            return;
        }
        ShadowUtils.setElevationToView(content, ShadowUtils.SHADOW_LV4);
        applyAospShadowPadding(content);
    }

    public void initOutlineRoundRectBackground() {
        if (!mIsOutLineBackgroundInPopupWindow || getContentView() == null) {
            return;
        }
        // Fullscreen menu root must not clipToOutline — that kills card shadows on AOSP.
        if (getContentView() instanceof COUIPopupMenuRootView) {
            getContentView().setClipToOutline(false);
            return;
        }
        final float radius = resolvePopupCornerRadius(getContentView());
        getContentView().setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        getContentView().setClipToOutline(true);
    }

    public void initPopupWindowBackground(Context context, TypedArray typedArray) {
        setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.coui_free_bottom_alert_poplist_background));
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        initOutlineRoundRectBackground();
        initElevationInPopupwindow();
    }

    public void setDismissTouchOutside(boolean dismissTouchOutside) {
        if (dismissTouchOutside) {
            setTouchable(true);
            setFocusable(true);
            setOutsideTouchable(true);
        } else {
            setFocusable(false);
            setOutsideTouchable(false);
        }
        update();
    }

    public void setElevationInPopupwindow(boolean elevationInPopupwindow) {
        mSetElevationInPopupwindow = elevationInPopupwindow;
    }

    private static float resolvePopupCornerRadius(View view) {
        int radius = COUIContextUtil.getAttrDimens(view.getContext(), R.attr.couiRoundCornerM);
        if (radius <= 0) {
            radius = view.getResources().getDimensionPixelSize(R.dimen.coui_round_corner_m);
        }
        return radius;
    }

    /**
     * Tight PopupWindow surfaces clip AOSP elevation. Pad non-OEM content so LV4 shadows show.
     */
    private static void applyAospShadowPadding(View content) {
        int pad = ShadowUtils.getAospShadowPaddingPx(content, ShadowUtils.SHADOW_LV4);
        if (pad <= 0) {
            return;
        }
        if (content.getPaddingLeft() < pad || content.getPaddingTop() < pad
                || content.getPaddingRight() < pad || content.getPaddingBottom() < pad) {
            content.setPadding(
                    Math.max(content.getPaddingLeft(), pad),
                    Math.max(content.getPaddingTop(), pad),
                    Math.max(content.getPaddingRight(), pad),
                    Math.max(content.getPaddingBottom(), pad));
        }
        if (content instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) content;
            group.setClipChildren(false);
            group.setClipToPadding(false);
        }
    }
}
