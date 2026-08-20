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
import android.util.Property;
import android.view.View;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherState;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.folder.ClippedFolderIconLayoutRule;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.FolderPagedView;
import com.android.launcher3.folder.PreviewBackground;
import com.android.launcher3.folder.PreviewItemDrawingParams;
import com.android.launcher3.folder.large.HxyFolderGridOrganizer;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.R;
import com.coui.appcompat.animation.COUIMoveEaseInterpolator;
import com.coui.appcompat.animation.COUISpringInterpolator;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HxyFolderAnimationManager {
    private static final int FOLDER_NAME_ALPHA_DURATION = 180;
    private static final int FOLDER_SURFACE_ALPHA_DURATION = 160;
    private static final int WORKSPACE_ALPHA_DURATION = 160;
    private static final float COLOROS_WORKSPACE_SCALE = 0.92f;
    private static final float NON_PREVIEW_ITEM_SCALE = 0.2f;
    private FolderPagedView mContent;
    private Context mContext;
    private final int mDelay;
    private final int mDuration;
    private Folder mFolder;
    public FolderIcon mFolderIcon;
    private final TimeInterpolator mFolderInterpolator;
    private final TimeInterpolator mWorkspaceInterpolator;
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
                ? R.integer.config_fullFolderOpenDuration
                : R.integer.config_fullFolderCloseDuration);
        this.mDelay = res.getInteger(R.integer.config_folderDelay);
        // OplusFolderAnimationManager uses COUI springs for the full-folder path. Keep the
        // decoded response/bounce pairs here so every property follows the same physical curve.
        this.mFolderInterpolator = isOpening
                ? new COUISpringInterpolator(0.45d, 0.15d)
                : new COUISpringInterpolator(0.40d, 0.13d);
        this.mWorkspaceInterpolator = isOpening
                ? new COUIMoveEaseInterpolator()
                : this.mFolderInterpolator;
    }

    private List<BubbleTextView> getPreviewIconsOnPage(int page) {
        return this.mPreviewVerifier.setFolderInfo(this.mFolder.mInfo).previewItemsForPage(page, this.mFolder.getIconsInReadingOrder());
    }

    public AnimatorSet getAnimator(Consumer<Animator> call) {
        int previewItemOffsetX;
        float initialAlpha;
        float finalAlpha;
        float from;
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
        float previewSize = rule.getIconSize() * previewScale;
        float initialScale = scalePreviewX;
        this.mFolder.setScaleX(1.0f);
        this.mFolder.setScaleY(1.0f);
        this.mFolder.setPivotX(0.0f);
        this.mFolder.setPivotY(0.0f);
        int previewItemOffsetX2 = (int) (previewSize / 2.0f);
        if (Utilities.isRtl(this.mContext.getResources())) {
            previewItemOffsetX = (int) (((((float) lp.width) * initialScale) - initialSize) - ((float) previewItemOffsetX2));
        } else {
            previewItemOffsetX = previewItemOffsetX2;
        }
        int paddingOffsetX = (int) (((float) (this.mFolder.getPaddingLeft() + this.mContent.getPaddingLeft())) * initialScale);
        int paddingOffsetY = (int) (((float) (this.mFolder.getPaddingTop() + this.mContent.getPaddingTop())) * initialScale);
        Rect contentPos = new Rect();
        this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this.mContent, contentPos);
        float xDistance = folderIconPos.exactCenterX() - contentPos.exactCenterX();
        float yDistance = folderIconPos.exactCenterY() - contentPos.exactCenterY();
        AnimatorSet a = new AnimatorSet();
        Animator t = getAnimator((View) this.mFolder, View.TRANSLATION_X, xDistance, 0.0f);
        call.accept(t);
        play(a, t);
        play(a, getAnimator((View) this.mFolder, View.TRANSLATION_Y, yDistance, 0.0f));
        if (!this.mLauncher.isInState(LauncherState.OVERVIEW)) {
            finalAlpha = 0.0f;
            initialAlpha = 1.0f;
            play(a, getAnimator((View) this.mLauncher.getDragLayer().findViewById(R.id.page_indicator), View.ALPHA, 1.0f, 0.0f), WORKSPACE_ALPHA_DURATION);
        } else {
            finalAlpha = 0.0f;
            initialAlpha = 1.0f;
        }
        Animator workspaceAlphaAnimator = getAnimator(
                (View) this.mLauncher.getWorkspace(), View.ALPHA, initialAlpha, finalAlpha);
        play(a, workspaceAlphaAnimator, WORKSPACE_ALPHA_DURATION);
        Launcher launcher = this.mLauncher;
        View tempView = launcher.getHotseat();
        float workspace_target = COLOROS_WORKSPACE_SCALE;
        from = 1.0f;
        play(a, getAnimator(tempView, View.ALPHA, 1.0f, 0.0f), WORKSPACE_ALPHA_DURATION);
        
        View pageIndicator = this.mLauncher.getDragLayer().findViewById(R.id.page_indicator);
        Workspace workspaceView = this.mLauncher.getWorkspace();
        int centerY = (int) (((-workspaceView.getPivotY()) / 2.0f) + (tempView.getPivotY() / 2.0f));
        tempView.setPivotY((float) centerY);
        pageIndicator.setPivotY((float) centerY);
        tempView.setPivotX(workspaceView.getPivotX());
        pageIndicator.setPivotX(workspaceView.getPivotX());
        play(a, getAnimator(tempView, View.SCALE_X, 1.0f, COLOROS_WORKSPACE_SCALE));
        play(a, getAnimator(tempView, View.SCALE_Y, 1.0f, COLOROS_WORKSPACE_SCALE));
        play(a, getAnimator((View) workspaceView, View.SCALE_X, from, workspace_target));
        play(a, getAnimator((View) workspaceView, View.SCALE_Y, from, workspace_target));
        play(a, getAnimator(pageIndicator, View.SCALE_X, 1.0f, COLOROS_WORKSPACE_SCALE));
        play(a, getAnimator(pageIndicator, View.SCALE_Y, 1.0f, COLOROS_WORKSPACE_SCALE));

        // Labels are not present in the closed preview. Fade them independently so tiny text
        // does not travel outward with the preview icons during the morph.
        List<Animator> itemTextAlphaAnimators = new ArrayList<>();
        for (BubbleTextView icon : this.mFolder.getItemsOnPage(
                this.mFolder.mContent.getCurrentPage())) {
            if (this.mIsOpening) {
                icon.setTextVisibility(false);
            }
            Animator itemTextAlpha = icon.createTextAlphaAnimator(this.mIsOpening);
            play(a, itemTextAlpha);
            itemTextAlphaAnimators.add(itemTextAlpha);
            itemTextAlpha.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    icon.setTextVisibility(true);
                }
            });
        }
        
        this.mFolder.setAlpha(1.0f);
        Animator closedFolderNameAlpha =
                this.mFolderIcon.getFolderName().createTextAlphaAnimator(!this.mIsOpening);
        play(a, closedFolderNameAlpha, 0, FOLDER_NAME_ALPHA_DURATION);
        View folderHeader = this.mFolder.findViewById(R.id.folder_header);
        folderHeader.setAlpha(this.mIsOpening ? 0.0f : 1.0f);
        Animator headerAlphaAnimator = getAnimator(folderHeader, View.ALPHA, 0.0f, 1.0f);
        AnimatorSet a2 = a;
        play(a, headerAlphaAnimator);

        View contentBackground = this.mFolder.findViewById(R.id.folder_content_background);
        if (contentBackground != null) {
            // OplusFolder keeps this structural view transparent.
            contentBackground.animate().cancel();
            contentBackground.setAlpha(0.0f);
        }
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
                HxyFolderAnimationManager.this.mFolder.setAlpha(1.0f);
                folderHeader.setAlpha(HxyFolderAnimationManager.this.mIsOpening ? 1.0f : 0.0f);
                View contentBackground = HxyFolderAnimationManager.this.mFolder.findViewById(
                        R.id.folder_content_background);
                if (contentBackground != null) {
                    contentBackground.animate().cancel();
                    contentBackground.setAlpha(0.0f);
                }
                float launcherContentAlpha = HxyFolderAnimationManager.this.mIsOpening
                        ? 0.0f : 1.0f;
                float launcherContentScale = HxyFolderAnimationManager.this.mIsOpening
                        ? COLOROS_WORKSPACE_SCALE : 1.0f;
                workspaceView.setAlpha(launcherContentAlpha);
                workspaceView.setScaleX(launcherContentScale);
                workspaceView.setScaleY(launcherContentScale);
                tempView.setAlpha(launcherContentAlpha);
                tempView.setScaleX(launcherContentScale);
                tempView.setScaleY(launcherContentScale);
                pageIndicator.setAlpha(HxyFolderAnimationManager.this.mLauncher.isInState(
                        LauncherState.OVERVIEW) ? 0.0f : launcherContentAlpha);
                pageIndicator.setScaleX(launcherContentScale);
                pageIndicator.setScaleY(launcherContentScale);
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
            it.next().setInterpolator(this.mFolderInterpolator);
        }
        workspaceAlphaAnimator.setInterpolator(this.mWorkspaceInterpolator);
        closedFolderNameAlpha.setInterpolator(this.mWorkspaceInterpolator);
        headerAlphaAnimator.setInterpolator(new COUISpringInterpolator(
                0.0d, this.mIsOpening ? 0.40d : 0.15d));
        for (Animator itemTextAlpha : itemTextAlphaAnimators) {
            itemTextAlpha.setInterpolator(new COUISpringInterpolator(0.0d, 0.30d));
        }
        int radiusDiff = this.mPreviewBackground.getScaledRadius()
                - this.mPreviewBackground.getRadius();
        addPreviewItemAnimators(a2, 1.0f,
                (int) (previewItemOffsetX / scaleRelativeToDragLayer) + radiusDiff,
                radiusDiff, xDistance, yDistance);
        addNonPreviewItemAnimators(a2, itemsInPreview);
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

    private void addPreviewItemAnimators(AnimatorSet animatorSet, float folderScale,
            int previewItemOffsetX, int previewItemOffsetY,
            float folderTranslationX, float folderTranslationY) {
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
            if (addColorOsFlexiblePreviewItemAnimator(animatorSet2, btv,
                    previewItemInterpolator, folderTranslationX, folderTranslationY)) {
                i++;
                continue;
            }
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
            float xDistance = (float) (previewPosX - btvLp.x) - folderTranslationX;
            int i3 = previewPosX;
            float yDistance = (float) (previewPosY - btvLp.y) - folderTranslationY;
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
    private boolean addColorOsFlexiblePreviewItemAnimator(AnimatorSet animatorSet,
            BubbleTextView item, TimeInterpolator interpolator,
            float folderTranslationX, float folderTranslationY) {
        if (!(mFolderIcon instanceof HxyLargeFolderIcon)
                || !(item.getTag() instanceof WorkspaceItemInfo)) {
            return false;
        }
        Rect previewBounds = new Rect();
        if (!((HxyLargeFolderIcon) mFolderIcon).getColorOsPreviewItemBounds(
                (WorkspaceItemInfo) item.getTag(), previewBounds)) {
            return false;
        }
        Rect folderIconBounds = new Rect();
        Rect openItemBounds = new Rect();
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(
                mFolderIcon, folderIconBounds);
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(item, openItemBounds);

        float previewCenterX = folderIconBounds.left + previewBounds.exactCenterX();
        float previewCenterY = folderIconBounds.top + previewBounds.exactCenterY();
        float openIconCenterX = openItemBounds.left + item.getWidth() * 0.5f;
        float openIconCenterY = openItemBounds.top + item.getIconSize() * 0.5f;
        float translationXValue = previewCenterX - openIconCenterX - folderTranslationX;
        float translationYValue = previewCenterY - openIconCenterY - folderTranslationY;
        float initialScale = previewBounds.width() / (float) Math.max(1, item.getIconSize());

        item.setPivotX(item.getWidth() * 0.5f);
        item.setPivotY(item.getIconSize() * 0.5f);
        Animator translationX = getAnimator(item, View.TRANSLATION_X,
                translationXValue, 0.0f);
        Animator translationY = getAnimator(item, View.TRANSLATION_Y,
                translationYValue, 0.0f);
        Animator scale = getAnimator(item, LauncherAnimUtils.SCALE_PROPERTY,
                initialScale, 1.0f);
        translationX.setInterpolator(interpolator);
        translationY.setInterpolator(interpolator);
        scale.setInterpolator(interpolator);
        play(animatorSet, translationX);
        play(animatorSet, translationY);
        play(animatorSet, scale);

        if (mFolder.getItemCount() > ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW) {
            int delay = mIsOpening ? mDelay : mDelay * 2;
            if (mIsOpening) {
                translationX.setStartDelay(delay);
                translationY.setStartDelay(delay);
                scale.setStartDelay(delay);
            }
            translationX.setDuration(Math.max(0, translationX.getDuration() - delay));
            translationY.setDuration(Math.max(0, translationY.getDuration() - delay));
            scale.setDuration(Math.max(0, scale.getDuration() - delay));
        }
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (mIsOpening) {
                    item.setTranslationX(translationXValue);
                    item.setTranslationY(translationYValue);
                    item.setScaleX(initialScale);
                    item.setScaleY(initialScale);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                item.setTranslationX(0.0f);
                item.setTranslationY(0.0f);
                item.setScaleX(1.0f);
                item.setScaleY(1.0f);
                item.setPivotX(item.getWidth() * 0.5f);
                item.setPivotY(item.getHeight() * 0.5f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                onAnimationEnd(animation);
            }
        });
        return true;
    }



    /**
     * OPPO keeps the first nine children spatially connected to the closed preview. Children
     * outside that preview do not translate from the folder icon; they spring from 0.2 scale
     * while their labels use the separate child-alpha spring.
     */
    private void addNonPreviewItemAnimators(AnimatorSet animatorSet,
            List<BubbleTextView> previewItems) {
        for (BubbleTextView item : this.mFolder.getItemsOnPage(
                this.mFolder.mContent.getCurrentPage())) {
            if (previewItems.contains(item)) {
                continue;
            }
            float startScale = this.mIsOpening ? NON_PREVIEW_ITEM_SCALE : 1.0f;
            item.setScaleX(startScale);
            item.setScaleY(startScale);
            Animator scaleAnimator = getAnimator(item, LauncherAnimUtils.SCALE_PROPERTY,
                    NON_PREVIEW_ITEM_SCALE, 1.0f);
            scaleAnimator.setInterpolator(this.mFolderInterpolator);
            play(animatorSet, scaleAnimator);
            scaleAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    item.setScaleX(1.0f);
                    item.setScaleY(1.0f);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    onAnimationEnd(animation);
                }
            });
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

    private Animator getAnimator(View view, Property property, float v1, float v2) {
        if (this.mIsOpening) {
            return ObjectAnimator.ofFloat(view, property, new float[]{v1, v2});
        }
        return ObjectAnimator.ofFloat(view, property, new float[]{v2, v1});
    }

}
