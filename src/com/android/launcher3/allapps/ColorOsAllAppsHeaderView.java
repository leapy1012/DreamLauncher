package com.android.launcher3.allapps;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.launcher3.R;
import com.android.launcher3.settings.SettingsActivity;

/** ColorOS-style fixed mode selector shown above the All Apps grid. */
public class ColorOsAllAppsHeaderView extends FrameLayout implements FloatingHeaderRow {

    /** Receives mode changes without coupling the header to a Launcher activity implementation. */
    public interface OnModeChangeListener {
        void onModeChanged(boolean categories);
    }

    private static final PathInterpolator MODE_INTERPOLATOR =
            new PathInterpolator(0.2f, 0f, 0f, 1f);

    private View mSelection;
    private TextView mAll;
    private TextView mCategories;
    private boolean mShowingCategories;
    private OnModeChangeListener mModeChangeListener;

    public ColorOsAllAppsHeaderView(Context context) {
        this(context, null);
    }

    public ColorOsAllAppsHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSelection = findViewById(R.id.coloros_all_apps_selection);
        mAll = findViewById(R.id.coloros_all_apps_all);
        mCategories = findViewById(R.id.coloros_all_apps_categories);
        mAll.setOnClickListener(v -> setCategoriesMode(false, true));
        mCategories.setOnClickListener(v -> setCategoriesMode(true, true));
        ImageButton menuButton = (ImageButton) getChildAt(getChildCount() - 1);
        menuButton.setOnClickListener(this::showMenu);
    }

    public void setOnModeChangeListener(OnModeChangeListener listener) {
        mModeChangeListener = listener;
    }

    public void setCategoriesMode(boolean categories, boolean animate) {
        if (mShowingCategories == categories && mSelection.getWidth() > 0) return;
        mShowingCategories = categories;
        TextView selected = categories ? mCategories : mAll;
        int width = selected.getWidth();
        float targetX = selected.getLeft();
        if (width == 0) {
            post(() -> updateSelection(false));
        } else {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSelection.getLayoutParams();
            lp.width = width;
            mSelection.setLayoutParams(lp);
            if (animate) {
                mSelection.animate().translationX(targetX).setDuration(280)
                        .setInterpolator(MODE_INTERPOLATOR).start();
            } else {
                mSelection.setTranslationX(targetX);
            }
        }
        updateLabelStyles(animate);
        if (mModeChangeListener != null) mModeChangeListener.onModeChanged(categories);
    }

    private void updateSelection(boolean animate) {
        TextView selected = mShowingCategories ? mCategories : mAll;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSelection.getLayoutParams();
        lp.width = selected.getWidth();
        mSelection.setLayoutParams(lp);
        if (animate) {
            mSelection.animate().translationX(selected.getLeft()).setDuration(280)
                    .setInterpolator(MODE_INTERPOLATOR).start();
        } else {
            mSelection.setTranslationX(selected.getLeft());
        }
    }

    private void updateLabelStyles(boolean animate) {
        int active = 0xFF101010;
        int inactive = getResources().getColor(
                R.color.coloros_all_apps_text_secondary, getContext().getTheme());
        mAll.setTextColor(mShowingCategories ? inactive : active);
        mCategories.setTextColor(mShowingCategories ? active : inactive);
        mAll.setTypeface(null, mShowingCategories
                ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
        mCategories.setTypeface(null, mShowingCategories
                ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        if (animate) {
            mAll.animate().alpha(mShowingCategories ? 0.82f : 1f).setDuration(180).start();
            mCategories.animate().alpha(mShowingCategories ? 1f : 0.82f)
                    .setDuration(180).start();
        }
    }

    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(getContext(), anchor);
        menu.getMenu().add(R.string.coloros_all_apps_sort_alphabetically);
        menu.getMenu().add(R.string.settings_button_text);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals(getContext().getString(R.string.settings_button_text))) {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
            return true;
        });
        menu.show();
    }

    @Override
    public void setup(FloatingHeaderView parent, FloatingHeaderRow[] allRows, boolean tabsHidden) { }

    @Override
    public int getExpectedHeight() {
        return getResources().getDimensionPixelSize(R.dimen.coloros_all_apps_header_height);
    }

    @Override
    public boolean shouldDraw() {
        return getVisibility() == VISIBLE;
    }

    @Override
    public boolean hasVisibleContent() {
        return true;
    }

    @Override
    public void setVerticalScroll(int scroll, boolean isScrolledOut) {
        setVisibility(isScrolledOut ? INVISIBLE : VISIBLE);
        if (!isScrolledOut) {
            setTranslationY(scroll);
        }
    }

    @Override
    public Class<ColorOsAllAppsHeaderView> getTypeClass() {
        return ColorOsAllAppsHeaderView.class;
    }

    @Override
    public View getFocusedChild() {
        return null;
    }
}
