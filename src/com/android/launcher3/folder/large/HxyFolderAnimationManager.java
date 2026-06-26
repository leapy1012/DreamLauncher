package com.android.launcher3.folder.large;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.Property;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.util.DimenUtils;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherState;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.anim.PropertyResetListener;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.folder.ClippedFolderIconLayoutRule;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.FolderPagedView;
import com.android.launcher3.folder.PreviewBackground;
import com.android.launcher3.folder.PreviewItemDrawingParams;
import com.android.launcher3.folder.large.HxyFolderGridOrganizer;
import com.android.launcher3.statehandlers.DepthController;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.R;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class HxyFolderAnimationManager {
    public static final int FOLDER_BG_DURATION = 750;
    private static final int FOLDER_NAME_ALPHA_DURATION = 32;
    private FolderPagedView mContent;
    private Context mContext;
    private final int mDelay;
    private final int mDuration;
    private Folder mFolder;
    private GradientDrawable mFolderBackground;
    public FolderIcon mFolderIcon;
    private final DecelerateInterpolator mFolderInterpolator;
    private final boolean mIsOpening;
    private Launcher mLauncher;
    private PreviewBackground mPreviewBackground;
    private final HxyFolderGridOrganizer mPreviewVerifier;
    private final PreviewItemDrawingParams mTmpParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f);

    public HxyFolderAnimationManager(Folder folder, boolean isOpening) {
        this.mFolder = folder;
        this.mContent = folder.mContent;
        FolderIcon folderIcon = folder.getFolderIcon();
        this.mFolderIcon = folderIcon;
        this.mPreviewBackground = folderIcon.mBackground;
        this.mContext = folder.getContext();
        Launcher launcher = (Launcher) folder.getContext();
        this.mLauncher = launcher;
        this.mIsOpening = isOpening;
        this.mPreviewVerifier = new HxyFolderGridOrganizer(launcher.getDeviceProfile().inv);
        Resources res = this.mContent.getResources();
        this.mDuration = res.getInteger(isOpening
                ? R.integer.folder_open_duration
                : R.integer.folder_close_duration);
        this.mDelay = res.getInteger(R.integer.config_folderDelay);
        this.mFolderInterpolator = new DecelerateInterpolator();
    }

    private List<BubbleTextView> getPreviewIconsOnPage(int page) {
        return this.mPreviewVerifier.setFolderInfo(this.mFolder.mInfo).previewItemsForPage(page, this.mFolder.getIconsInReadingOrder());
    }

    public AnimatorSet getAnimator(Consumer<Animator> call) {
        int previewItemOffsetX;
        float initialAlpha;
        float finalAlpha;
        float from;
        long duration;
        BaseDragLayer.LayoutParams lp = (BaseDragLayer.LayoutParams) this.mFolder.getLayoutParams();
        ClippedFolderIconLayoutRule rule = this.mFolderIcon.getLayoutRule();
        boolean isOnFirstPage = this.mFolder.mContent.getCurrentPage() == 0;
        List<BubbleTextView> itemsInPreview = getPreviewIconsOnPage(isOnFirstPage ? 0 : this.mFolder.mContent.getCurrentPage());
        Rect folderIconPos = new Rect();
        float scaleRelativeToDragLayer = this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this.mFolderIcon, folderIconPos);
        Rect iconRect = this.mFolderIcon.getIconRect();
        folderIconPos.offset(iconRect.left, iconRect.top);
        folderIconPos.bottom = folderIconPos.top + iconRect.height();
        folderIconPos.right = folderIconPos.left + iconRect.width();
        float initialSize = ((float) (this.mPreviewBackground.getScaledRadius() * 2)) * scaleRelativeToDragLayer;
        float previewScale = rule.scaleForItem(itemsInPreview.size());
        float scalePreviewX = initialSize / ((float) lp.width);
        float scalePreviewY = initialSize / ((float) lp.height);
        float previewSize = rule.getIconSize() * previewScale;
        float initialScale = scalePreviewX;
        float scaleX = mIsOpening ? scalePreviewX : 1.0f;
        float scaleY = mIsOpening ? scalePreviewY : 1.0f;
        if (scaleX > 0.0f) {
            this.mFolder.setScaleX(scaleX);
            this.mFolder.setScaleY(scaleY);
        }
        this.mFolder.setPivotX((float) (-this.mPreviewBackground.getScaledRadius()));
        this.mFolder.setPivotY((float) (-this.mPreviewBackground.getScaledRadius()));
        int previewItemOffsetX2 = (int) (previewSize / 2.0f);
        if (Utilities.isRtl(this.mContext.getResources())) {
            previewItemOffsetX = (int) (((((float) lp.width) * initialScale) - initialSize) - ((float) previewItemOffsetX2));
        } else {
            previewItemOffsetX = previewItemOffsetX2;
        }
        int paddingOffsetX = (int) (((float) (this.mFolder.getPaddingLeft() + this.mContent.getPaddingLeft())) * initialScale);
        int paddingOffsetY = (int) (((float) (this.mFolder.getPaddingTop() + this.mContent.getPaddingTop())) * initialScale);
        int initialX = folderIconPos.left;
        int initialY = folderIconPos.top;
        float xDistance = (float) (initialX - lp.x);
        float yDistance = (float) (initialY - lp.y);
        int initialY2 = paddingOffsetX + previewItemOffsetX;
        Rect startRect = new Rect(Math.round(((float) initialY2) / initialScale), Math.round(((float) paddingOffsetY) / initialScale), Math.round((((float) initialY2) + initialSize) / initialScale), Math.round((((float) paddingOffsetY) + initialSize) / initialScale));
        Rect endRect = new Rect(0, 0, lp.width, lp.height);
        AnimatorSet a = new AnimatorSet();
        a.setInterpolator(this.mFolderInterpolator);
        float pxFromDp = (float) DimenUtils.pxToDp(mContext, 2.0f);
        PropertyResetListener colorResetListener = new PropertyResetListener(BubbleTextView.TEXT_ALPHA_PROPERTY, Float.valueOf(1.0f));
        // boolean isSpring = this.mLauncher.getStateManager().getCurrentStableState() == LauncherState.SPRING_LOADED_2;
        Animator t = getAnimator((View) this.mFolder, View.TRANSLATION_X, xDistance, 0.0f);
        call.accept(t);
        play(a, t);
        play(a, getAnimator((View) this.mFolder, View.TRANSLATION_Y, yDistance, 0.0f));
        play(a, getAnimator((View) this.mFolder, (Property) LauncherAnimUtils.SCALE_PROPERTY, initialScale, 1.0f));
        // play(a, getAnimator1(this.mFolderIcon, View.SCALE_X, 1.0f, 3.0f));
        // play(a, getAnimator1(this.mFolderIcon, View.SCALE_Y, 1.0f, 3.0f));
        // play(a, getAnimator1(this.mFolderIcon, View.ALPHA, 1.0f, 0.8f));
        // Animator animatorWallpaper = getAnimatorWallpaper(this.mLauncher.getHxyGroupAnimTool().getWallpaperBg(), View.ALPHA, 0.0f, 1.0f);
        // if (!this.mIsOpening || !isSpring) {
        // }
        // play(a, animatorWallpaper, this.mDuration);
        // if (isSpring && !this.mLauncher.isInState(LauncherState.OVERVIEW)) {
        //     play(a, getAnimator(this.mLauncher.getHxyGroupAnimTool().mUpView, View.ALPHA, 1.0f, 0.0f));
        // }
        if (!this.mLauncher.isInState(LauncherState.OVERVIEW)) {
            finalAlpha = 0.0f;
            initialAlpha = 1.0f;
            play(a, getAnimator((View) this.mLauncher.getDragLayer().findViewById(R.id.page_indicator), View.ALPHA, 1.0f, 0.0f));
        } else {
            finalAlpha = 0.0f;
            initialAlpha = 1.0f;
        }
        // play(a, getAnimator((View) this.mLauncher.getWorkspace(), View.ALPHA, initialAlpha, finalAlpha));
        // play(a, getAnimator(this.mLauncher.getAppsView().getSearchView(), View.ALPHA, initialAlpha, finalAlpha));
        Launcher launcher = this.mLauncher;
        View tempView = launcher.getHotseat();
        float workspace_target = 0.9f;
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        from = 1.0f;
        play(a, getAnimator(tempView, View.ALPHA, 1.0f, 0.0f));
        
        View pageIndicator = this.mLauncher.getDragLayer().findViewById(R.id.page_indicator);
        Workspace workspaceView = this.mLauncher.getWorkspace();
        int centerY = (int) (((-workspaceView.getPivotY()) / 2.0f) + (tempView.getPivotY() / 2.0f));
        tempView.setPivotY((float) centerY);
        pageIndicator.setPivotY((float) centerY);
        tempView.setPivotX(workspaceView.getPivotX());
        pageIndicator.setPivotX(workspaceView.getPivotX());
        play(a, getAnimator(tempView, View.SCALE_X, 1.0f, 0.9f));
        play(a, getAnimator(tempView, View.SCALE_Y, 1.0f, 0.9f));
        play(a, getAnimator((View) workspaceView, View.SCALE_X, from, workspace_target));
        play(a, getAnimator((View) workspaceView, View.SCALE_Y, from, workspace_target));
        play(a, getAnimator(pageIndicator, View.SCALE_X, 1.0f, 0.9f));
        play(a, getAnimator(pageIndicator, View.SCALE_Y, 1.0f, 0.9f));
        
        play(a, getAnimator((View) this.mFolder, View.ALPHA, 0.0f, 1.0f));
        play(a, this.mFolderIcon.getFolderName().createTextAlphaAnimator(!this.mIsOpening));
        this.mFolder.mFolderName.setAlpha(this.mIsOpening ? 0.0f : 1.0f);
        Animator animator2 = getAnimator((View) this.mFolder.mFolderName, View.ALPHA, 0.0f, 1.0f);
        AnimatorSet a2 = a;
        play(a, animator2, this.mIsOpening ? 32L : 0L,
                this.mIsOpening ? Math.max(0, this.mDuration - 32) : 32);
        int midDuration = this.mDuration / 2;
        play(a2, getAnimator((View) this.mFolder, View.TRANSLATION_Z, -this.mFolder.getElevation(), 0.0f), this.mIsOpening ? (long) midDuration : 0, midDuration);
        a2.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                HxyFolderAnimationManager.this.mFolder.setTranslationX(0.0f);
                HxyFolderAnimationManager.this.mFolder.setTranslationY(0.0f);
                HxyFolderAnimationManager.this.mFolder.setTranslationZ(0.0f);
                HxyFolderAnimationManager.this.mFolder.setScaleX(1.0f);
                HxyFolderAnimationManager.this.mFolder.setScaleY(1.0f);
                if (HxyFolderAnimationManager.this.mLauncher.isInState(LauncherState.OVERVIEW)) {
                    HxyFolderAnimationManager.this.mLauncher.getDragLayer().findViewById(R.id.page_indicator).setAlpha(0.0f);
                }
                HxyFolderAnimationManager.this.initHardlayer(false);
            }

            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                HxyFolderAnimationManager.this.initHardlayer(true);
            }
        });
        Iterator<Animator> it = a2.getChildAnimations().iterator();
        while (it.hasNext()) {
            it.next().setInterpolator(new DecelerateInterpolator(1.0f));
        }
        return a2;
    }

    public void startFolderEditAnim(View v) {
        AnimatorSet a = new AnimatorSet();
        play(a, getAnimator(v, View.ALPHA, 0.0f, 1.0f));
        a.setDuration(800);
        a.setInterpolator(this.mFolderInterpolator);
        a.start();
        v.setTag(a);
    }

    public void initHardlayer(boolean result) {
        if (result) {
            this.mLauncher.getHotseat().setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
            this.mLauncher.getWorkspace().setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
            this.mLauncher.getHotseat().setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
            this.mLauncher.getDragLayer().findViewById(R.id.page_indicator).setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
            return;
        }
        this.mLauncher.getHotseat().setLayerType(View.LAYER_TYPE_NONE, (Paint) null);
        if (this.mLauncher.isInState(LauncherState.NORMAL)) {
            this.mLauncher.getWorkspace().setLayerType(View.LAYER_TYPE_NONE, (Paint) null);
        }
        this.mLauncher.getHotseat().setLayerType(View.LAYER_TYPE_NONE, (Paint) null);
        this.mLauncher.getDragLayer().findViewById(R.id.page_indicator).setLayerType(View.LAYER_TYPE_NONE, (Paint) null);
    }

    private void addPreviewItemAnimators(AnimatorSet animatorSet, float folderScale, int previewItemOffsetX, int previewItemOffsetY) {
        List<BubbleTextView> list;
        int numItemsInPreview;
        List<BubbleTextView> itemsInPreview;
        boolean isOnFirstPage;
        AnimatorSet animatorSet2 = animatorSet;
        ClippedFolderIconLayoutRule rule = this.mFolderIcon.getLayoutRule();
        boolean z = true;
        boolean isOnFirstPage2 = this.mFolder.mContent.getCurrentPage() == 0;
        if (isOnFirstPage2) {
            list = getPreviewIconsOnPage(0);
        } else {
            list = getPreviewIconsOnPage(this.mFolder.mContent.getCurrentPage());
        }
        List<BubbleTextView> itemsInPreview2 = list;
        int numItemsInPreview2 = itemsInPreview2.size();
        int numItemsInFirstPagePreview = isOnFirstPage2 ? numItemsInPreview2 : ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;
        TimeInterpolator previewItemInterpolator = getPreviewItemInterpolator();
        ShortcutAndWidgetContainer cwc = this.mContent.getPageAt(0).getShortcutsAndWidgets();
        int i = 0;
        while (i < numItemsInPreview2) {
            BubbleTextView btv = itemsInPreview2.get(i);
            CellLayoutLayoutParams btvLp = (CellLayoutLayoutParams) btv.getLayoutParams();
            btvLp.isLockedToGrid = z;
            cwc.setupLp(btv);
            float iconScale = (rule.getIconSize() * rule.scaleForItem(numItemsInFirstPagePreview)) / ((float) itemsInPreview2.get(i).getIconSize());
            float initialScale = iconScale / folderScale;
            float scale = this.mIsOpening ? initialScale : 1.0f;
            btv.setScaleX(scale);
            btv.setScaleY(scale);
            rule.computePreviewItemDrawingParams(i, numItemsInFirstPagePreview, this.mTmpParams);
            int iconOffsetX = ((int) (((float) (btvLp.width - btv.getIconSize())) * iconScale)) / 2;
            float scale2 = scale;
            int i2 = i;
            int previewPosX = (int) (((this.mTmpParams.transX - ((float) iconOffsetX)) + ((float) previewItemOffsetX)) / folderScale);
            ClippedFolderIconLayoutRule rule2 = rule;
            int previewPosY = (int) ((this.mTmpParams.transY + ((float) previewItemOffsetY)) / folderScale);
            float xDistance = (float) (previewPosX - btvLp.x);
            int i3 = previewPosX;
            float yDistance = (float) (previewPosY - btvLp.y);
            int previewPosY2 = previewPosY;
            CellLayoutLayoutParams btvLp2 = btvLp;
            Animator translationX = getAnimator((View) btv, View.TRANSLATION_X, xDistance, 0.0f);
            translationX.setInterpolator(previewItemInterpolator);
            play(animatorSet2, translationX);
            float xDistance2 = xDistance;
            Animator translationY = getAnimator((View) btv, View.TRANSLATION_Y, yDistance, 0.0f);
            translationY.setInterpolator(previewItemInterpolator);
            play(animatorSet2, translationY);
            Animator scaleAnimator = getAnimator((View) btv, (Property) LauncherAnimUtils.SCALE_PROPERTY, initialScale, 1.0f);
            scaleAnimator.setInterpolator(previewItemInterpolator);
            play(animatorSet2, scaleAnimator);
            float initialScale2 = initialScale;
            if (this.mFolder.getItemCount() > ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW) {
                int delay = this.mDelay;
                if (!mIsOpening) {
                    delay *= 2;
                }
                if (mIsOpening) {
                    isOnFirstPage = isOnFirstPage2;
                    translationX.setStartDelay((long) delay);
                    translationY.setStartDelay((long) delay);
                    scaleAnimator.setStartDelay((long) delay);
                } else {
                    isOnFirstPage = isOnFirstPage2;
                }
                itemsInPreview = itemsInPreview2;
                numItemsInPreview = numItemsInPreview2;
                translationX.setDuration(translationX.getDuration() - ((long) delay));
                translationY.setDuration(translationY.getDuration() - ((long) delay));
                scaleAnimator.setDuration(scaleAnimator.getDuration() - ((long) delay));
            } else {
                isOnFirstPage = isOnFirstPage2;
                itemsInPreview = itemsInPreview2;
                numItemsInPreview = numItemsInPreview2;
            }
            final BubbleTextView bubbleTextView = btv;
            final float f2 = xDistance2;
            final float f3 = yDistance;
            final float yDistance2 = initialScale2;
            animatorSet2.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    if (HxyFolderAnimationManager.this.mIsOpening) {
                        bubbleTextView.setTranslationX(f2);
                        bubbleTextView.setTranslationY(f3);
                        bubbleTextView.setScaleX(yDistance2);
                        bubbleTextView.setScaleY(yDistance2);
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    bubbleTextView.setTranslationX(0.0f);
                    bubbleTextView.setTranslationY(0.0f);
                    bubbleTextView.setScaleX(1.0f);
                    bubbleTextView.setScaleY(1.0f);
                }
            });
            i = i2 + 1;
            rule = rule2;
            isOnFirstPage2 = isOnFirstPage;
            itemsInPreview2 = itemsInPreview;
            numItemsInPreview2 = numItemsInPreview;
            z = true;
        }
    }

    private void play(AnimatorSet as, Animator a) {
        play(as, a, a.getStartDelay(), this.mDuration);
    }

    private void play(AnimatorSet as, Animator a, int duration) {
        play(as, a, a.getStartDelay(), duration);
    }

    private void play(AnimatorSet as, Animator a, long startDelay, int duration) {
        a.setStartDelay(startDelay);
        a.setDuration((long) duration);
        as.play(a);
    }

    private TimeInterpolator getPreviewItemInterpolator() {
        if (this.mFolder.getItemCount() <= ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW) {
            return this.mFolderInterpolator;
        }
        if (this.mIsOpening) {
            return this.mFolderInterpolator;
        }
        return this.mFolderInterpolator;
    }

    private Animator getAnimator(DepthController view, Property property, float v1, float v2) {
        if (this.mIsOpening) {
            return ObjectAnimator.ofFloat(view, property, new float[]{v2});
        }
        return ObjectAnimator.ofFloat(view, property, new float[]{v1});
    }

    private Animator getAnimator(View view, Property property, float v1, float v2) {
        if (this.mIsOpening) {
            return ObjectAnimator.ofFloat(view, property, new float[]{v1, v2});
        }
        return ObjectAnimator.ofFloat(view, property, new float[]{v2, v1});
    }

    private Animator getAnimatorWallpaper(View view, Property property, float v1, float v2) {
        if (this.mIsOpening) {
            return ObjectAnimator.ofFloat(view, property, new float[]{view.getAlpha(), v2});
        }
        return ObjectAnimator.ofFloat(view, property, new float[]{view.getAlpha(), v1});
    }

    private Animator getAnimator1(View view, Property property, float v1, float v2) {
        if (this.mIsOpening) {
            return ObjectAnimator.ofFloat(view, property, new float[]{v1, v1});
        }
        return ObjectAnimator.ofFloat(view, property, new float[]{v2, v1});
    }

    private Animator getAnimator(GradientDrawable drawable, String property, int v1, int v2) {
        if (this.mIsOpening) {
            return ObjectAnimator.ofArgb(drawable, property, new int[]{v1, v2});
        }
        return ObjectAnimator.ofArgb(drawable, property, new int[]{v2, v1});
    }
}
