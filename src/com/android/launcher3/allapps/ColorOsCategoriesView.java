package com.android.launcher3.allapps;

import static com.android.launcher3.touch.ItemLongClickListener.INSTANCE_ALL_APPS;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Process;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.launcher3.R;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.views.ActivityContext;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ColorOS-style smart category surface. Categories are derived locally from Android application
 * metadata and stable package/title rules; no network service or private OPPO API is required.
 */
public class ColorOsCategoriesView extends ScrollView implements AllAppsStore.OnUpdateListener {

    private static final PathInterpolator ENTER_INTERPOLATOR =
            new PathInterpolator(0.2f, 0f, 0f, 1f);
    private final LinearLayout mContent;
    private AllAppsStore mStore;
    private ActivityContext mActivityContext;
    private boolean mListening;

    public ColorOsCategoriesView(Context context) {
        this(context, null);
    }

    public ColorOsCategoriesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipToPadding(false);
        setFillViewport(true);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setVerticalScrollBarEnabled(false);
        int horizontal = getResources().getDimensionPixelSize(
                R.dimen.coloros_category_horizontal_margin);
        int top = getResources().getDimensionPixelSize(R.dimen.coloros_all_apps_header_height)
                + dp(5);
        setPadding(horizontal, top, horizontal, dp(24));

        mContent = new LinearLayout(context);
        mContent.setOrientation(LinearLayout.VERTICAL);
        addView(mContent, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void initialize(AllAppsStore store, ActivityContext activityContext) {
        mStore = store;
        mActivityContext = activityContext;
        if (isAttachedToWindow()) startListening();
        rebuild();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startListening();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mListening && mStore != null) {
            mStore.removeUpdateListener(this);
            mListening = false;
        }
        super.onDetachedFromWindow();
    }

    private void startListening() {
        if (!mListening && mStore != null) {
            mStore.addUpdateListener(this);
            mListening = true;
        }
    }

    @Override
    public void onAppsUpdated() {
        rebuild();
    }

    public void showWithAnimation() {
        setVisibility(VISIBLE);
        setAlpha(0f);
        setTranslationY(dp(18));
        animate().alpha(1f).translationY(0).setDuration(320)
                .setInterpolator(ENTER_INTERPOLATOR).start();
        for (int i = 0; i < mContent.getChildCount(); i++) {
            View child = mContent.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(dp(12));
            child.animate().alpha(1f).translationY(0).setStartDelay(35L * Math.min(i, 6))
                    .setDuration(300).setInterpolator(ENTER_INTERPOLATOR).start();
        }
    }

    private void rebuild() {
        if (mStore == null || mActivityContext == null) return;
        mContent.removeAllViews();

        List<AppInfo> apps = new ArrayList<>();
        for (AppInfo app : mStore.getApps()) {
            if (Process.myUserHandle().equals(app.user)) apps.add(app);
        }
        Collator collator = Collator.getInstance();
        apps.sort((left, right) -> collator.compare(left.title, right.title));
        if (apps.isEmpty()) return;

        List<AppInfo> recent = new ArrayList<>(apps);
        recent.sort(Comparator.comparingLong(this::getInstallTime).reversed()
                .thenComparing(app -> String.valueOf(app.title)));
        addRecentSection(recent.subList(0, Math.min(4, recent.size())));

        Map<Category, List<AppInfo>> groups = new LinkedHashMap<>();
        for (Category category : Category.values()) groups.put(category, new ArrayList<>());
        for (AppInfo app : apps) groups.get(classify(app)).add(app);

        List<CategoryBlock> blocks = new ArrayList<>();
        for (Map.Entry<Category, List<AppInfo>> entry : groups.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                blocks.add(new CategoryBlock(entry.getKey().label, entry.getValue()));
            }
        }
        for (int i = 0; i < blocks.size(); i += 2) {
            addCategoryRow(blocks.get(i), i + 1 < blocks.size() ? blocks.get(i + 1) : null);
        }
    }

    private void addRecentSection(List<AppInfo> apps) {
        LinearLayout section = createSection(R.string.coloros_category_recent);
        CategoryCard card = new CategoryCard(getContext(), apps, true);
        section.addView(card, 0, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.coloros_category_recent_height)));
        mContent.addView(section, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private void addCategoryRow(CategoryBlock left, CategoryBlock right) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        row.addView(createCategorySection(left), weightedParams(false));
        if (right != null) {
            row.addView(createCategorySection(right), weightedParams(true));
        } else {
            View spacer = new View(getContext());
            row.addView(spacer, weightedParams(true));
        }
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = getResources().getDimensionPixelSize(R.dimen.coloros_category_row_gap);
        mContent.addView(row, rowParams);
    }

    private LinearLayout.LayoutParams weightedParams(boolean endColumn) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                LayoutParams.WRAP_CONTENT, 1f);
        int halfGap = getResources().getDimensionPixelSize(
                R.dimen.coloros_category_column_gap) / 2;
        if (endColumn) params.leftMargin = halfGap;
        else params.rightMargin = halfGap;
        return params;
    }

    private LinearLayout createCategorySection(CategoryBlock block) {
        LinearLayout section = createSection(block.label);
        section.addView(new CategoryCard(getContext(), block.apps, false), 0,
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return section;
    }

    private LinearLayout createSection(int labelRes) {
        LinearLayout section = new LinearLayout(getContext());
        section.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(getContext());
        label.setText(labelRes);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.coloros_category_label_size));
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = getResources().getDimensionPixelSize(
                R.dimen.coloros_category_label_gap);
        section.addView(label, labelParams);
        return section;
    }

    private long getInstallTime(AppInfo app) {
        try {
            PackageInfo info = getContext().getPackageManager().getPackageInfo(
                    app.componentName.getPackageName(), 0);
            return info.firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private Category classify(AppInfo app) {
        String packageName = app.componentName.getPackageName().toLowerCase(Locale.ROOT);
        String key = packageName + " " + String.valueOf(app.title).toLowerCase(Locale.ROOT);
        int androidCategory = ApplicationInfo.CATEGORY_UNDEFINED;
        try {
            androidCategory = getContext().getPackageManager()
                    .getApplicationInfo(packageName, 0).category;
        } catch (PackageManager.NameNotFoundException ignored) { }

        if (androidCategory == ApplicationInfo.CATEGORY_GAME || contains(key,
                "game", "gaming")) return Category.GAMES;
        if (androidCategory == ApplicationInfo.CATEGORY_SOCIAL || contains(key,
                "dialer", "phone", "contact", "message", "mms", "weibo", "social",
                "facebook", "whatsapp", "telegram", "instagram")) return Category.SOCIAL;
        if (androidCategory == ApplicationInfo.CATEGORY_IMAGE || contains(key,
                "camera", "photo", "gallery", "image")) return Category.PHOTOGRAPHY;
        if (androidCategory == ApplicationInfo.CATEGORY_AUDIO
                || androidCategory == ApplicationInfo.CATEGORY_VIDEO || contains(key,
                "music", "video", "media", "youtube", "radio")) return Category.ENTERTAINMENT;
        if (androidCategory == ApplicationInfo.CATEGORY_MAPS || contains(key,
                "maps", "navigation", "travel", "ride", "taxi")) return Category.TRAVEL;
        if (contains(key, "shop", "shopping", "store", "amazon", "market")) {
            return Category.SHOPPING;
        }
        if (androidCategory == ApplicationInfo.CATEGORY_PRODUCTIVITY || contains(key,
                "office", "document", "notes", "mail", "drive")) return Category.PRODUCTIVITY;
        if (contains(key, "settings", "calculator", "calendar", "clock", "browser", "file",
                "recorder", "compass", "weather", "tool", "logger", "flashlight",
                "downloads")) return Category.TOOLS;
        return Category.OTHER;
    }

    private static boolean contains(String value, String... needles) {
        return Arrays.stream(needles).anyMatch(value::contains);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private enum Category {
        SOCIAL(R.string.coloros_category_social),
        TOOLS(R.string.coloros_category_tools),
        PHOTOGRAPHY(R.string.coloros_category_photography),
        ENTERTAINMENT(R.string.coloros_category_entertainment),
        GAMES(R.string.coloros_category_games),
        SHOPPING(R.string.coloros_category_shopping),
        TRAVEL(R.string.coloros_category_travel),
        PRODUCTIVITY(R.string.coloros_category_productivity),
        OTHER(R.string.coloros_category_other);

        final int label;
        Category(int label) { this.label = label; }
    }

    private static class CategoryBlock {
        final int label;
        final List<AppInfo> apps;
        CategoryBlock(int label, List<AppInfo> apps) {
            this.label = label;
            this.apps = apps;
        }
    }

    /** Folder-preview card: four large slots, with the fourth becoming a 2x2 mini-grid. */
    private class CategoryCard extends ViewGroup {
        private final boolean mRecent;

        CategoryCard(Context context, List<AppInfo> apps, boolean recent) {
            super(context);
            mRecent = recent;
            setClipChildren(false);
            GradientDrawable background = new GradientDrawable();
            background.setColor(getResources().getColor(
                    R.color.coloros_category_card_surface, context.getTheme()));
            background.setCornerRadius(getResources().getDimension(
                    R.dimen.coloros_category_card_radius));
            setBackground(background);
            int count = Math.min(apps.size(), recent ? 4 : 7);
            for (int i = 0; i < count; i++) {
                addView(new AppIconCell(context, apps.get(i), !recent && i >= 3));
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = mRecent
                    ? resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec) : width;
            setMeasuredDimension(width, height);
            int halfWidth = width / 2;
            int halfHeight = height / 2;
            for (int i = 0; i < getChildCount(); i++) {
                boolean mini = !mRecent && i >= 3;
                int childWidth = mRecent ? width / Math.max(4, getChildCount())
                        : mini ? halfWidth / 2 : halfWidth;
                int childHeight = mRecent ? height : mini ? halfHeight / 2 : halfHeight;
                getChildAt(i).measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int width = r - l;
            int height = b - t;
            if (mRecent) {
                int cellWidth = width / Math.max(4, getChildCount());
                int contentWidth = cellWidth * getChildCount();
                int start = (width - contentWidth) / 2;
                for (int i = 0; i < getChildCount(); i++) {
                    getChildAt(i).layout(start + i * cellWidth, 0,
                            start + (i + 1) * cellWidth, height);
                }
                return;
            }
            int halfW = width / 2;
            int halfH = height / 2;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (i < 3) {
                    int left = (i % 2) * halfW;
                    int top = (i / 2) * halfH;
                    child.layout(left, top, left + halfW, top + halfH);
                } else {
                    int mini = i - 3;
                    int cellW = halfW / 2;
                    int cellH = halfH / 2;
                    int left = halfW + (mini % 2) * cellW;
                    int top = halfH + (mini / 2) * cellH;
                    child.layout(left, top, left + cellW, top + cellH);
                }
            }
        }
    }

    private class AppIconCell extends FrameLayout {
        private final ImageView mIcon;
        private final boolean mMini;

        AppIconCell(Context context, AppInfo app, boolean mini) {
            super(context);
            mMini = mini;
            setTag(app);
            setContentDescription(app.title);
            setClickable(true);
            setFocusable(true);
            setOnClickListener(mActivityContext.getItemOnClickListener());
            setOnLongClickListener(INSTANCE_ALL_APPS);
            setOnTouchListener((view, event) -> {
                if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
                    animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).start();
                } else if (event.getActionMasked() == android.view.MotionEvent.ACTION_UP
                        || event.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL) {
                    animate().scaleX(1f).scaleY(1f).setDuration(180)
                            .setInterpolator(ENTER_INTERPOLATOR).start();
                }
                return false;
            });
            mIcon = new ImageView(context);
            mIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mIcon.setImageDrawable(app.newIcon(context));
            addView(mIcon);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(width, height);
            int iconSize = dp(mMini ? 25 : 48);
            mIcon.measure(MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY));
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int iconWidth = mIcon.getMeasuredWidth();
            int iconHeight = mIcon.getMeasuredHeight();
            int x = (right - left - iconWidth) / 2;
            int y = (bottom - top - iconHeight) / 2;
            mIcon.layout(x, y, x + iconWidth, y + iconHeight);
        }
    }
}
