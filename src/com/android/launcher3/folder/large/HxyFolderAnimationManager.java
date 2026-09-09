package com.android.launcher3.folder.large;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Property;
import android.view.View;
import android.view.animation.PathInterpolator;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
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
import com.android.launcher3.views.BaseDragLayer;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ColorOS {@code OplusFolderAnimationManager} open/close path:
 * per-icon COUI springs (bounce/response) + workspace/hotseat 1↔0.92 companion fade.
 */
public class HxyFolderAnimationManager {
    /** Exact ColorOS OplusFolderAnimationManager constants. */
    private static final float OPEN_TRANS_BOUNCE = 0.15f;
    private static final float OPEN_TRANS_RESPONSE = 0.45f;
    private static final float CLOSE_TRANS_BOUNCE_MAX = 0.16f;
    private static final float CLOSE_TRANS_BOUNCE_MIN = 0.1f;
    private static final float CLOSE_TRANS_RESPONSE = 0.4f;
    private static final float ICON_ALPHA_BOUNCE = 0.0f;
    private static final float ICON_ALPHA_RESPONSE = 0.3f;
    private static final float HEADER_SPRING_BOUNCE = 0.0f;
    private static final float HEADER_SPRING_OPEN_RESPONSE = 0.4f;
    private static final float HEADER_SPRING_CLOSE_RESPONSE = 0.15f;
    private static final float OPEN_ANIM_SCALE_RATIO = 0.2f;
    private static final float ONE_THIRD = 0.33f;
    private static final float TWO_THIRD = 0.66f;
    private static final float WORKSPACE_FOLDER_SCALE = 0.92f;

    private static final TimeInterpolator WORKSPACE_ALPHA_OPEN =
            new PathInterpolator(0.3f, 0f, 0.1f, 1f);
    private static final TimeInterpolator WORKSPACE_SCALE_INTERPOLATOR =
            new PathInterpolator(0.3f, 0f, 0.1f, 1f);

    private final FolderPagedView mContent;
    private final Context mContext;
    private final int mDuration;
    private final int mWorkspaceContentDuration;
    private final Folder mFolder;
    private final FolderIcon mFolderIcon;
    private final boolean mIsOpening;
    private final Launcher mLauncher;
    private final PreviewBackground mPreviewBackground;
    private final HxyFolderGridOrganizer mPreviewVerifier;
    private final PreviewItemDrawingParams mTmpParams = new PreviewItemDrawingParams(0f, 0f, 0f);
    private final ArrayList<COUISpringAnimation> mRunningSprings = new ArrayList<>();

    public HxyFolderAnimationManager(Folder folder, boolean isOpening) {
        mFolder = folder;
        mContent = folder.mContent;
        mFolderIcon = folder.getFolderIcon();
        mPreviewBackground = mFolderIcon.mBackground;
        mContext = folder.getContext();
        mLauncher = (Launcher) folder.getContext();
        mIsOpening = isOpening;
        mPreviewVerifier = new HxyFolderGridOrganizer(mLauncher.getDeviceProfile().inv);
        Resources res = mContent.getResources();
        // Full ColorOS path uses folder_open_duration / folder_close_duration (850 / 800).
        mDuration = res.getInteger(isOpening
                ? R.integer.folder_open_duration
                : R.integer.folder_close_duration);
        mWorkspaceContentDuration = res.getInteger(isOpening
                ? R.integer.folder_workspace_content_open_duration
                : R.integer.folder_workspace_content_close_duration);
    }

    private List<BubbleTextView> getPreviewIconsOnPage(int page) {
        return mPreviewVerifier.setFolderInfo(mFolder.mInfo)
                .previewItemsForPage(page, mFolder.getIconsInReadingOrder());
    }

    public AnimatorSet getAnimator(Consumer<Animator> call) {
        AnimatorSet a = new AnimatorSet();

        // ColorOS full path keeps the folder container at scale 1; icons spring individually.
        mFolder.setScaleX(1f);
        mFolder.setScaleY(1f);
        mFolder.setTranslationX(0f);
        mFolder.setTranslationY(0f);

        // Placeholder so Folder open/close listeners attach and duration matches ColorOS.
        ValueAnimator gate = ValueAnimator.ofFloat(0f, 1f);
        gate.setDuration(mDuration);
        call.accept(gate);
        a.play(gate);

        playWorkspaceCompanion(a);

        // Folder chrome alpha (container becomes visible immediately; header springs separately).
        play(a, getAnimator(mFolder, View.ALPHA, 0f, 1f), 0, Math.min(mDuration, 200));

        View header = mFolder.mFolderName;
        if (header != null) {
            float headerResponse = mIsOpening
                    ? HEADER_SPRING_OPEN_RESPONSE : HEADER_SPRING_CLOSE_RESPONSE;
            if (mIsOpening) {
                header.setAlpha(0f);
            }
            playSpringOnGate(gate, header, COUIDynamicAnimation.ALPHA,
                    mIsOpening ? 0f : 1f, mIsOpening ? 1f : 0f,
                    HEADER_SPRING_BOUNCE, headerResponse, COUIDynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
        }

        play(a, mFolderIcon.getFolderName().createTextAlphaAnimator(!mIsOpening), 0, mDuration);

        addChildrenSpringAnimators(gate);

        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                initHardlayer(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                cancelSprings();
                resetChildrenTransforms();
                mFolder.setTranslationX(0f);
                mFolder.setTranslationY(0f);
                mFolder.setTranslationZ(0f);
                mFolder.setScaleX(1f);
                mFolder.setScaleY(1f);
                mFolder.setAlpha(1f);
                if (mLauncher.isInState(LauncherState.OVERVIEW)) {
                    View pi = mLauncher.getDragLayer().findViewById(R.id.page_indicator);
                    if (pi != null) {
                        pi.setAlpha(0f);
                    }
                }
                initHardlayer(false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelSprings();
                resetChildrenTransforms();
            }
        });
        return a;
    }

    private void playWorkspaceCompanion(AnimatorSet a) {
        View hotseat = mLauncher.getHotseat();
        Workspace<?> workspace = mLauncher.getWorkspace();
        View pageIndicator = mLauncher.getDragLayer().findViewById(R.id.page_indicator);

        // AnimationConstant.getMinWpScaleForFolderAnim → 0.92
        float workspaceTarget = WORKSPACE_FOLDER_SCALE;
        int contentMs = mWorkspaceContentDuration;

        int centerY = (int) (((-workspace.getPivotY()) / 2f) + (hotseat.getPivotY() / 2f));
        hotseat.setPivotY(centerY);
        hotseat.setPivotX(workspace.getPivotX());
        if (pageIndicator != null) {
            pageIndicator.setPivotY(centerY);
            pageIndicator.setPivotX(workspace.getPivotX());
        }

        Animator wsAlpha = getAnimator(workspace, View.ALPHA, 1f, 0f);
        wsAlpha.setInterpolator(mIsOpening ? WORKSPACE_ALPHA_OPEN : WORKSPACE_SCALE_INTERPOLATOR);
        play(a, wsAlpha, 0, contentMs);

        play(a, getAnimator(hotseat, View.ALPHA, 1f, 0f), 0, contentMs);
        play(a, getAnimator(hotseat, View.SCALE_X, 1f, workspaceTarget), 0, contentMs);
        play(a, getAnimator(hotseat, View.SCALE_Y, 1f, workspaceTarget), 0, contentMs);
        play(a, getAnimator(workspace, View.SCALE_X, 1f, workspaceTarget), 0, contentMs);
        play(a, getAnimator(workspace, View.SCALE_Y, 1f, workspaceTarget), 0, contentMs);
        if (pageIndicator != null) {
            play(a, getAnimator(pageIndicator, View.ALPHA, 1f, 0f), 0, contentMs);
            play(a, getAnimator(pageIndicator, View.SCALE_X, 1f, workspaceTarget), 0, contentMs);
            play(a, getAnimator(pageIndicator, View.SCALE_Y, 1f, workspaceTarget), 0, contentMs);
        }
        for (Animator child : a.getChildAnimations()) {
            if (child != wsAlpha && child.getDuration() == contentMs) {
                child.setInterpolator(WORKSPACE_SCALE_INTERPOLATOR);
            }
        }
    }

    /**
     * ColorOS {@code OplusFolderAnimationManager#getChildrenAnimatorSet} — spring each icon
     * from the closed-folder preview position to its grid cell (and reverse on close).
     */
    private void addChildrenSpringAnimators(ValueAnimator gate) {
        CellLayout cellLayout = mContent.getCurrentCellLayout();
        if (cellLayout == null) {
            return;
        }
        ShortcutAndWidgetContainer container = cellLayout.getShortcutsAndWidgets();
        if (container == null) {
            return;
        }
        cellLayout.setClipChildren(false);
        cellLayout.setClipToPadding(false);
        container.setClipChildren(false);
        container.setClipToPadding(false);

        DeviceProfile dp = mLauncher.getDeviceProfile();
        int folderIconSizePx = dp.folderIconSizePx > 0 ? dp.folderIconSizePx : dp.iconSizePx;
        int iconSizePx = dp.iconSizePx;
        int folderCellWidthPx = dp.folderCellWidthPx;
        int folderCellHeightPx = dp.folderCellHeightPx;

        float[] folderIconLoc = new float[2];
        float scaleRel = mLauncher.getDragLayer()
                .getDescendantCoordRelativeToSelf(mFolderIcon, folderIconLoc);
        float folderIconCenterX = ((folderIconSizePx / 2f) + mPreviewBackground.getOffsetX())
                * scaleRel + folderIconLoc[0];
        float folderIconCenterY = ((folderIconSizePx / 2f) + mPreviewBackground.getOffsetY())
                * scaleRel + folderIconLoc[1];

        float[] contentLoc = new float[2];
        Utilities.getDescendantCoordRelativeToAncestor(mContent, mFolder, contentLoc, false);
        BaseDragLayer.LayoutParams folderLp =
                (BaseDragLayer.LayoutParams) mFolder.getLayoutParams();
        float contentCenterX = contentLoc[0] + folderLp.x + mContent.getWidth() / 2f;
        float contentCenterY = contentLoc[1] + folderLp.y + mContent.getHeight() / 2f;
        float[] folderDistance = new float[]{
                folderIconCenterX - contentCenterX,
                folderIconCenterY - contentCenterY
        };

        int needX = folderIconCenterX <= dp.availableWidthPx * ONE_THIRD ? 1
                : (folderIconCenterX <= dp.availableWidthPx * TWO_THIRD ? 2 : 3);
        int needY = folderIconCenterY <= dp.availableHeightPx * ONE_THIRD ? 1
                : (folderIconCenterY <= dp.availableHeightPx * TWO_THIRD ? 2 : 3);

        ClippedFolderIconLayoutRule rule = mFolderIcon.getLayoutRule();
        boolean isOnFirstPage = mContent.getCurrentPage() == 0;
        List<BubbleTextView> previewItems = getPreviewIconsOnPage(
                isOnFirstPage ? 0 : mContent.getCurrentPage());
        int previewCount = previewItems.size();
        int firstPagePreviewCount = isOnFirstPage
                ? previewCount : ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;

        int childCount = container.getChildCount();
        int lastCellY = childCount == 0 ? 0
                : ((CellLayoutLayoutParams) container.getChildAt(childCount - 1)
                .getLayoutParams()).getCellY();
        int lastCellX = childCount == 0 ? 0
                : (childCount > cellLayout.getCountX()
                ? cellLayout.getCountX() - 1
                : ((CellLayoutLayoutParams) container.getChildAt(childCount - 1)
                .getLayoutParams()).getCellX());

        int maxLevelClose = getMaxLevel(4 - needX, 4 - needY, lastCellY, lastCellX);

        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof BubbleTextView)) {
                continue;
            }
            BubbleTextView btv = (BubbleTextView) child;
            CellLayoutLayoutParams btvLp = (CellLayoutLayoutParams) btv.getLayoutParams();
            btvLp.isLockedToGrid = true;
            container.setupLp(btv);

            int previewIndex = previewItems.indexOf(btv);
            boolean inPreview = previewIndex >= 0
                    && previewIndex < ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;
            float previewScale;
            float previewTransX;
            float previewTransY;
            if (inPreview) {
                rule.computePreviewItemDrawingParams(previewIndex, firstPagePreviewCount, mTmpParams);
                previewScale = mTmpParams.scale;
                previewTransX = mTmpParams.transX;
                previewTransY = mTmpParams.transY;
            } else {
                // ColorOS requireScaleChild → start tiny when not part of closed preview.
                previewScale = rule.scaleForItem(firstPagePreviewCount) * OPEN_ANIM_SCALE_RATIO;
                previewTransX = folderIconSizePx / 2f;
                previewTransY = folderIconSizePx / 2f;
            }

            float f30 = inPreview ? 1f : OPEN_ANIM_SCALE_RATIO;
            float width = ((((mContent.getWidth() / 2f) - (folderCellWidthPx / 2f))
                    - btvLp.x)
                    - mContent.getPaddingLeft())
                    - cellLayout.getPaddingLeft()
                    - ((((folderIconSizePx - (iconSizePx * previewScale)) / 2f) - previewTransX)
                    * scaleRel);
            float height = ((((mContent.getHeight() / 2f) - (folderCellHeightPx / 2f))
                    - btvLp.y)
                    - mContent.getPaddingTop())
                    - cellLayout.getPaddingTop();
            float f35 = (folderIconSizePx / 2f) - ((previewScale * folderCellHeightPx) / 2f);
            float yOffset = height - ((((btv.getPaddingTop() * previewScale) + f35) - previewTransY)
                    * scaleRel);

            float startTx;
            float startTy;
            float startScale;
            float endTx;
            float endTy;
            float endScale;
            float bounce;
            float response;

            int level = getLevel(btvLp.getCellX(), btvLp.getCellY(), needX, needY,
                    lastCellY, lastCellX);
            if (mIsOpening) {
                startTx = width + folderDistance[0];
                startTy = yOffset + folderDistance[1];
                startScale = previewScale * scaleRel * f30;
                endTx = 0f;
                endTy = 0f;
                endScale = 1f;
                bounce = OPEN_TRANS_BOUNCE;
                response = OPEN_TRANS_RESPONSE; // level * 0 response delay unused (ICON_RESPONSE_DELAY=0)
            } else {
                startTx = btv.getTranslationX();
                startTy = btv.getTranslationY();
                startScale = btv.getScaleX();
                endTx = width + folderDistance[0];
                endTy = yOffset + folderDistance[1];
                endScale = previewScale * scaleRel * f30;
                float t = ((level + 1f) / (maxLevelClose + 1f));
                bounce = Utilities.mapRange(t, CLOSE_TRANS_BOUNCE_MAX, CLOSE_TRANS_BOUNCE_MIN);
                response = CLOSE_TRANS_RESPONSE;
            }

            updatePivot(btv);

            final float sTx = startTx;
            final float sTy = startTy;
            final float sSc = startScale;
            gate.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (mIsOpening) {
                        btv.setTranslationX(sTx);
                        btv.setTranslationY(sTy);
                        btv.setScaleX(sSc);
                        btv.setScaleY(sSc);
                        btv.setAlpha(0f);
                    }
                }
            });

            playSpringOnGate(gate, btv, COUIDynamicAnimation.TRANSLATION_X,
                    startTx, endTx, bounce, response, 0.5f);
            playSpringOnGate(gate, btv, COUIDynamicAnimation.TRANSLATION_Y,
                    startTy, endTy, bounce, response, 0.5f);
            playSpringOnGate(gate, btv, COUIDynamicAnimation.SCALE_X,
                    startScale, endScale, bounce, response,
                    COUIDynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
            playSpringOnGate(gate, btv, COUIDynamicAnimation.SCALE_Y,
                    startScale, endScale, bounce, response,
                    COUIDynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
            playSpringOnGate(gate, btv, COUIDynamicAnimation.ALPHA,
                    mIsOpening ? 0f : 1f, mIsOpening ? 1f : 0f,
                    ICON_ALPHA_BOUNCE, ICON_ALPHA_RESPONSE,
                    COUIDynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
        }
    }

    private void updatePivot(BubbleTextView btv) {
        btv.setPivotX(btv.getMeasuredWidth() / 2f);
        btv.setPivotY(btv.getPaddingTop() + btv.getIconSize() / 2f);
    }

    private void playSpringOnGate(ValueAnimator gate, View target,
            COUIDynamicAnimation.ViewProperty property,
            float start, float end, float bounce, float response, float minVisible) {
        COUISpringForce force = new COUISpringForce(end)
                .setBounce(bounce)
                .setResponse(response);
        COUISpringAnimation spring = new COUISpringAnimation(target, property);
        spring.setSpring(force);
        spring.setStartValue(start);
        spring.setMinimumVisibleChange(minVisible);
        mRunningSprings.add(spring);
        gate.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                spring.setStartValue(start);
                spring.animateToFinalPosition(end);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (spring.isRunning()) {
                    spring.cancel();
                }
            }
        });
    }

    private void cancelSprings() {
        for (COUISpringAnimation spring : mRunningSprings) {
            if (spring.isRunning()) {
                spring.cancel();
            }
        }
        mRunningSprings.clear();
    }

    private void resetChildrenTransforms() {
        CellLayout cellLayout = mContent.getCurrentCellLayout();
        if (cellLayout == null) {
            return;
        }
        ShortcutAndWidgetContainer container = cellLayout.getShortcutsAndWidgets();
        if (container == null) {
            return;
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            child.setTranslationX(0f);
            child.setTranslationY(0f);
            child.setScaleX(1f);
            child.setScaleY(1f);
            child.setAlpha(1f);
        }
        if (mFolder.mFolderName != null) {
            mFolder.mFolderName.setAlpha(1f);
        }
    }

    /** ColorOS OplusFolderAnimationManager.getLevel */
    private static int getLevel(int cellX, int cellY, int needX, int needY,
            int lastCellY, int lastCellX) {
        if (needX == 1) {
            if (needY == 1) {
                return (lastCellX + lastCellY) - (cellX + cellY);
            }
            if (needY == 2) {
                return lastCellX - cellX;
            }
            if (needY == 3) {
                return (lastCellX - cellX) + cellY;
            }
        } else if (needX == 2) {
            if (needY == 1) {
                return lastCellY - cellY;
            }
            if (needY == 3) {
                return cellY;
            }
            return 0;
        } else if (needX == 3) {
            if (needY == 1) {
                return cellX + (lastCellY - cellY);
            }
            if (needY == 2) {
                return cellX;
            }
            if (needY == 3) {
                return cellX + cellY;
            }
        }
        return 0;
    }

    /** ColorOS OplusFolderAnimationManager.getMaxLevel */
    private static int getMaxLevel(int needX, int needY, int lastCellY, int lastCellX) {
        if (needX == 1 || needX == 3) {
            if (needY == 1 || needY == 3) {
                return lastCellX + lastCellY;
            }
            if (needY == 2) {
                return lastCellX;
            }
        } else if (needX == 2) {
            if (needY == 1 || needY == 3) {
                return lastCellY;
            }
        }
        return 0;
    }

    public void startFolderEditAnim(View v) {
        AnimatorSet a = new AnimatorSet();
        play(a, getAnimator(v, View.ALPHA, 0f, 1f), 0, 800);
        a.setInterpolator(WORKSPACE_SCALE_INTERPOLATOR);
        a.start();
        v.setTag(a);
    }

    public void initHardlayer(boolean result) {
        View pageIndicator = mLauncher.getDragLayer().findViewById(R.id.page_indicator);
        if (result) {
            mLauncher.getHotseat().setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
            mLauncher.getWorkspace().setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
            if (pageIndicator != null) {
                pageIndicator.setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
            }
            return;
        }
        mLauncher.getHotseat().setLayerType(View.LAYER_TYPE_NONE, (Paint) null);
        if (mLauncher.isInState(LauncherState.NORMAL)) {
            mLauncher.getWorkspace().setLayerType(View.LAYER_TYPE_NONE, (Paint) null);
        }
        if (pageIndicator != null) {
            pageIndicator.setLayerType(View.LAYER_TYPE_NONE, (Paint) null);
        }
    }

    private void play(AnimatorSet as, Animator a, long startDelay, int duration) {
        if (a == null) {
            return;
        }
        a.setStartDelay(startDelay);
        a.setDuration(duration);
        as.play(a);
    }

    private Animator getAnimator(View view, Property property, float v1, float v2) {
        if (mIsOpening) {
            return ObjectAnimator.ofFloat(view, property, v1, v2);
        }
        return ObjectAnimator.ofFloat(view, property, v2, v1);
    }
}
