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
import androidx.dynamicanimation.animation.FloatValueHolder;

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
    /** ColorOS OplusFolderAnimationManager.TEXT_DELAY_PERCENT — title stays 0 until spring ≥ 0.5. */
    private static final float TEXT_DELAY_PERCENT = 0.5f;
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
    /** Close-end icon poses; applied when the gate ends so icons don't snap back to grid. */
    private final ArrayList<IconEndState> mIconEndStates = new ArrayList<>();

    private static final class IconEndState {
        final BubbleTextView view;
        final float tx;
        final float ty;
        final float scale;
        final float textAlpha;

        IconEndState(BubbleTextView view, float tx, float ty, float scale, float textAlpha) {
            this.view = view;
            this.tx = tx;
            this.ty = ty;
            this.scale = scale;
            this.textAlpha = textAlpha;
        }
    }

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
        if (!isOpening && mFolderIcon != null) {
            // Small folders: suppress PreviewItemManager immediately.
            // Large folders: defer until after landing math (list must stay laid out).
            if (!(mFolderIcon instanceof HxyLargeFolderIcon
                    && HxyLargeFolderProxy.isLargeFolder(mFolderIcon))) {
                mFolderIcon.onFolderAnimStartClose(mContent.getCurrentPage());
            }
        }
    }

    private List<BubbleTextView> getPreviewIconsOnPage(int page) {
        return mPreviewVerifier.setFolderInfo(mFolder.mInfo)
                .previewItemsForPage(page, mFolder.getIconsInReadingOrder());
    }

    public AnimatorSet getAnimator(Consumer<Animator> call) {
        AnimatorSet a = new AnimatorSet();
        mIconEndStates.clear();
        mRunningSprings.clear();

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

        // ColorOS full path keeps the folder container opaque; only header/icons spring.
        mFolder.setAlpha(1f);
        if (mIsOpening && mFolderIcon != null) {
            // Hide closed preview before children take their open-start poses — otherwise
            // the plate minis and springing BTVs double-draw (especially large folders).
            mFolderIcon.setIconVisible(false);
            mFolderIcon.setForceHideDot(true);
        }

        // ColorOS animates folder_header (not only the EditText) with open alpha remapped.
        View header = mFolder.mHeader != null ? mFolder.mHeader : mFolder.mFolderName;
        if (header != null) {
            float headerResponse = mIsOpening
                    ? HEADER_SPRING_OPEN_RESPONSE : HEADER_SPRING_CLOSE_RESPONSE;
            if (mIsOpening) {
                header.setAlpha(0f);
            }
            playHeaderAlphaSpringOnGate(gate, header,
                    mIsOpening ? 0f : 1f, mIsOpening ? 1f : 0f,
                    HEADER_SPRING_BOUNCE, headerResponse);
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
                // Snap springs to their final values before cancelling so close doesn't
                // leave icons mid-flight, then reset only after open (close keeps preview
                // pose until Folder.closeComplete removes this view).
                finishSpringsAtFinalPosition();
                if (mIsOpening) {
                    resetChildrenTransforms();
                }
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
                finishSpringsAtFinalPosition();
                if (mIsOpening) {
                    resetChildrenTransforms();
                }
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

        // Close: Oppo snaps workspace to final scale before icon springs so the
        // FolderIcon plate stays aligned with DragLayer landing (esp. large folders).
        // Only fade α 0→1; scale is already 1.
        if (!mIsOpening) {
            workspace.setScaleX(1f);
            workspace.setScaleY(1f);
            hotseat.setScaleX(1f);
            hotseat.setScaleY(1f);
            if (pageIndicator != null) {
                pageIndicator.setScaleX(1f);
                pageIndicator.setScaleY(1f);
            }
            Animator wsAlpha = getAnimator(workspace, View.ALPHA, 1f, 0f);
            wsAlpha.setInterpolator(WORKSPACE_SCALE_INTERPOLATOR);
            play(a, wsAlpha, 0, contentMs);
            play(a, getAnimator(hotseat, View.ALPHA, 1f, 0f), 0, contentMs);
            if (pageIndicator != null) {
                play(a, getAnimator(pageIndicator, View.ALPHA, 1f, 0f), 0, contentMs);
            }
            return;
        }

        // Open: 1→0.92 / α 1→0.
        Animator wsAlpha = getAnimator(workspace, View.ALPHA, 1f, 0f);
        wsAlpha.setInterpolator(WORKSPACE_ALPHA_OPEN);
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
        // Plate size must match PreviewItemManager's rule.init(previewSize).
        int plateSize = mPreviewBackground.getPreviewSize() > 0
                ? mPreviewBackground.getPreviewSize()
                : (dp.folderIconSizePx > 0 ? dp.folderIconSizePx : dp.iconSizePx);
        int plateW = mPreviewBackground.getPreviewWidth() > 0
                ? mPreviewBackground.getPreviewWidth() : plateSize;
        int plateH = mPreviewBackground.getPreviewHeight() > 0
                ? mPreviewBackground.getPreviewHeight() : plateSize;

        // Open runs before the next layout pass; force measure/layout so content width
        // and child bounds are valid for start-pose math (large folders were worst).
        if (mIsOpening) {
            ensureFolderLaidOutForAnim();
        }

        // Close: workspace is still at 0.92 when we build animators, but it restores to 1.0
        // before icon springs finish. Measure FolderIcon at the FINAL scale so landing
        // matches the settled preview (Oppo snaps workspace to state scale before measure).
        Workspace<?> workspace = mLauncher.getWorkspace();
        View hotseat = mLauncher.getHotseat();
        float savedWsSx = workspace.getScaleX();
        float savedWsSy = workspace.getScaleY();
        float savedHsSx = hotseat.getScaleX();
        float savedHsSy = hotseat.getScaleY();
        if (!mIsOpening) {
            workspace.setScaleX(1f);
            workspace.setScaleY(1f);
            hotseat.setScaleX(1f);
            hotseat.setScaleY(1f);
        }

        float[] folderIconLoc = new float[2];
        float scaleRel = mLauncher.getDragLayer()
                .getDescendantCoordRelativeToSelf(mFolderIcon, folderIconLoc);
        float folderIconCenterX = ((plateW / 2f) + mPreviewBackground.getOffsetX())
                * scaleRel + folderIconLoc[0];
        float folderIconCenterY = ((plateH / 2f) + mPreviewBackground.getOffsetY())
                * scaleRel + folderIconLoc[1];

        // ColorOS getFolderDistance: preview-plate center vs folder content center.
        float[] contentLoc = new float[2];
        Utilities.getDescendantCoordRelativeToAncestor(mContent, mFolder, contentLoc, false);
        com.android.launcher3.views.BaseDragLayer.LayoutParams folderLp =
                (com.android.launcher3.views.BaseDragLayer.LayoutParams) mFolder.getLayoutParams();
        float contentCenterX = contentLoc[0] + folderLp.x + mContent.getWidth() / 2f;
        float contentCenterY = contentLoc[1] + folderLp.y + mContent.getHeight() / 2f;
        float[] folderDistance = new float[]{
                folderIconCenterX - contentCenterX,
                folderIconCenterY - contentCenterY
        };

        // Keep workspace at final scale 1.0 through landing math (incl. large-folder
        // list getDescendantCoord). Restored after the children loop below.

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
        // Oppo requireScaleChild: indices beyond the closed-folder preview param count.
        int previewParamCount = Math.min(previewCount,
                ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW);
        if (HxyLargeFolderProxy.isLargeFolder(mFolderIcon)
                && mFolderIcon instanceof HxyLargeFolderIcon) {
            int largeCount = ((HxyLargeFolderIcon) mFolderIcon).getLargePreviewParamCount();
            if (largeCount > 0) {
                previewParamCount = largeCount;
            }
        }

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
        int iconSizePx = dp.iconSizePx;
        int folderCellWidthPx = dp.folderCellWidthPx;
        int folderCellHeightPx = dp.folderCellHeightPx;

        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof BubbleTextView)) {
                continue;
            }
            BubbleTextView btv = (BubbleTextView) child;
            CellLayoutLayoutParams btvLp = (CellLayoutLayoutParams) btv.getLayoutParams();
            btvLp.isLockedToGrid = true;
            container.setupLp(btv);
            if (btv.getWidth() <= 0 || btv.getHeight() <= 0) {
                container.measureChild(btv);
            }

            int previewIndex = previewItems.indexOf(btv);
            boolean inPreview = previewIndex >= 0
                    && previewIndex < ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;
            // Oppo requireScaleChild: indices past closed-preview param count.
            boolean overflow = i >= previewParamCount;
            // Preview → matching 3×3 slot. Overflow open: stack under last preview
            // icon (Oppo stacked params). Overflow close: plate-center (≥ MAX).
            int paramIndex;
            if (inPreview) {
                paramIndex = previewIndex;
            } else if (mIsOpening) {
                paramIndex = ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW - 1;
            } else {
                paramIndex = Math.max(i, ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW);
            }
            rule.computePreviewItemDrawingParams(paramIndex, firstPagePreviewCount, mTmpParams);

            float previewTransX = mTmpParams.transX;
            float previewTransY = mTmpParams.transY;
            // params.scale is ColorOS plate fraction (~0.18).
            float animScale = mTmpParams.scale;
            float previewIconSize = rule.getIconSize() * animScale;
            // Oppo requireScaleChild → OPEN_ANIM_SCALE_RATIO (0.2).
            float f30 = overflow ? OPEN_ANIM_SCALE_RATIO : 1f;
            float hotseatScale = 1f;
            // Lower index draws above so overflow stacked under slot 8 stays covered.
            btv.setZ(-i);

            float startTx;
            float startTy;
            float startScale;
            float endTx;
            float endTy;
            float endScale;
            float bounce;
            float response;

            int level = mIsOpening
                    ? getLevel(btvLp.getCellX(), btvLp.getCellY(), needX, needY,
                    lastCellY, lastCellX)
                    : getLevel(btvLp.getCellX(), btvLp.getCellY(), 4 - needX, 4 - needY,
                    lastCellY, lastCellX);

            float[] previewToBtv = resolvePreviewLanding(btv, previewIndex, overflow,
                    previewParamCount, previewTransX, previewTransY,
                    previewIconSize * f30 * hotseatScale, folderIconLoc, scaleRel);

            if (mIsOpening) {
                // Oppo: spring from preview (or stacked last-slot ×0.2 for overflow)
                // to open grid. DragLayer landing matches drawn plate slots.
                startTx = previewToBtv[0];
                startTy = previewToBtv[1];
                startScale = previewToBtv[2];
                endTx = 0f;
                endTy = 0f;
                endScale = 1f;
                bounce = OPEN_TRANS_BOUNCE;
                response = OPEN_TRANS_RESPONSE;
                btv.setTranslationX(startTx);
                btv.setTranslationY(startTy);
                btv.setScaleX(startScale);
                btv.setScaleY(startScale);
                btv.setAlpha(1f);
                btv.setTextAlpha(0f);
            } else {
                // Close: DragLayer mapping so end pose matches drawn preview slots.
                startTx = btv.getTranslationX();
                startTy = btv.getTranslationY();
                startScale = btv.getScaleX();
                endTx = previewToBtv[0];
                endTy = previewToBtv[1];
                endScale = previewToBtv[2];
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
                        btv.setAlpha(1f);
                        btv.setTextAlpha(0f);
                    }
                }
            });

            playSpringOnGate(gate, btv, COUIDynamicAnimation.TRANSLATION_X,
                    startTx, endTx, bounce, response, 0.5f);
            playSpringOnGate(gate, btv, COUIDynamicAnimation.TRANSLATION_Y,
                    startTy, endTy, bounce, response, 0.5f);
            playScaleSpringOnGate(gate, btv, startScale, endScale, bounce, response);
            playTextAlphaSpringOnGate(gate, btv,
                    mIsOpening ? 0f : 1f, mIsOpening ? 1f : 0f,
                    ICON_ALPHA_BOUNCE, ICON_ALPHA_RESPONSE);

            if (!mIsOpening) {
                mIconEndStates.add(new IconEndState(btv, endTx, endTy, endScale, 0f));
            }
        }

        if (!mIsOpening) {
            // Keep workspace at 1.0 (already snapped in playWorkspaceCompanion).
            // Restoring 0.92 here reintroduced plate vs landing mismatch for large folders.
            if (mFolderIcon instanceof HxyLargeFolderIcon
                    && HxyLargeFolderProxy.isLargeFolder(mFolderIcon)) {
                mFolderIcon.onFolderAnimStartClose(mContent.getCurrentPage());
            }
        }
    }

    private void updatePivot(BubbleTextView btv) {
        // ColorOS OplusFolderAnimationManager: pivot at view center.
        btv.setPivotX(btv.getWidth() / 2f);
        btv.setPivotY(btv.getHeight() / 2f);
    }

    /** Measure/layout Folder using DragLayer LayoutParams so open start poses are valid. */
    private void ensureFolderLaidOutForAnim() {
        com.android.launcher3.views.BaseDragLayer.LayoutParams lp =
                (com.android.launcher3.views.BaseDragLayer.LayoutParams) mFolder.getLayoutParams();
        if (lp == null) {
            return;
        }
        int w = lp.width > 0 ? lp.width : mFolder.getMeasuredWidth();
        int h = lp.height > 0 ? lp.height : mFolder.getMeasuredHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        int wSpec = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY);
        mFolder.measure(wSpec, hSpec);
        int left = lp.x;
        int top = lp.y;
        mFolder.layout(left, top, left + mFolder.getMeasuredWidth(),
                top + mFolder.getMeasuredHeight());
        // Ensure current page children have real bounds for DragLayer mapping.
        CellLayout cellLayout = mContent.getCurrentCellLayout();
        if (cellLayout != null) {
            ShortcutAndWidgetContainer container = cellLayout.getShortcutsAndWidgets();
            if (container != null) {
                for (int i = 0; i < container.getChildCount(); i++) {
                    View child = container.getChildAt(i);
                    if (child.getWidth() <= 0 || child.getHeight() <= 0) {
                        container.measureChild(child);
                        // Position from layout params if layout() hasn't assigned bounds yet.
                        if (child.getLayoutParams() instanceof CellLayoutLayoutParams) {
                            CellLayoutLayoutParams clp =
                                    (CellLayoutLayoutParams) child.getLayoutParams();
                            if (clp.width > 0 && clp.height > 0) {
                                child.layout(clp.x, clp.y, clp.x + clp.width, clp.y + clp.height);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolve open-start / close-end pose. Large folders land on
     * {@link HxyLargeFolderListView} cells; small folders use ClippedFolder rule offsets.
     */
    private float[] resolvePreviewLanding(BubbleTextView btv, int previewIndex,
            boolean overflow, int previewParamCount,
            float previewTransX, float previewTransY, float previewIconSize,
            float[] folderIconLoc, float folderIconScaleRel) {
        if (HxyLargeFolderProxy.isLargeFolder(mFolderIcon)
                && mFolderIcon instanceof HxyLargeFolderIcon) {
            HxyLargeFolderIcon largeIcon = (HxyLargeFolderIcon) mFolderIcon;
            int largeCount = largeIcon.getLargePreviewParamCount();
            if (largeCount > 0) {
                int slot;
                if (previewIndex >= 0 && previewIndex < largeCount) {
                    slot = previewIndex;
                } else if (overflow || previewIndex < 0) {
                    slot = largeCount - 1;
                } else {
                    slot = Math.min(previewIndex, largeCount - 1);
                }
                Rect slotBounds = new Rect();
                if (largeIcon.getPreviewItemBoundsInDragLayer(slot, slotBounds)) {
                    float size = Math.min(slotBounds.width(), slotBounds.height());
                    float left = slotBounds.left;
                    float top = slotBounds.top;
                    // Oppo requireScaleChild: shrink overflow toward the last cell center.
                    if (overflow) {
                        float shrunk = size * OPEN_ANIM_SCALE_RATIO;
                        left += (size - shrunk) / 2f;
                        top += (size - shrunk) / 2f;
                        size = shrunk;
                    }
                    return computePreviewLandingTranslation(
                            btv, left, top, size);
                }
            }
        }
        return computePreviewLandingTranslation(
                btv, previewTransX, previewTransY, previewIconSize,
                folderIconLoc, folderIconScaleRel);
    }

    /**
     * Translation/scale that places {@code btv}'s icon on the FolderIcon preview slot.
     * Uses DragLayer mapping so landing matches {@link PreviewItemManager#drawPreviewItem}
     * regardless of plate vs {@code iconSizePx} differences.
     *
     * @return float[]{tx, ty, scale}
     */
    private float[] computePreviewLandingTranslation(BubbleTextView btv,
            float previewTransX, float previewTransY, float previewIconSize,
            float[] folderIconLoc, float folderIconScaleRel) {
        float previewLeft = folderIconLoc[0]
                + (mPreviewBackground.getBasePreviewOffsetX() + previewTransX)
                * folderIconScaleRel;
        float previewTop = folderIconLoc[1]
                + (mPreviewBackground.getBasePreviewOffsetY() + previewTransY)
                * folderIconScaleRel;
        float previewSizeOnScreen = previewIconSize * folderIconScaleRel;
        return computePreviewLandingTranslation(btv, previewLeft, previewTop, previewSizeOnScreen);
    }

    /** Landing from absolute DragLayer preview-icon bounds (large-folder list cells). */
    private float[] computePreviewLandingTranslation(BubbleTextView btv,
            float previewLeft, float previewTop, float previewSizeOnScreen) {
        float saveTx = btv.getTranslationX();
        float saveTy = btv.getTranslationY();
        float saveSx = btv.getScaleX();
        float saveSy = btv.getScaleY();
        btv.setTranslationX(0f);
        btv.setTranslationY(0f);
        btv.setScaleX(1f);
        btv.setScaleY(1f);

        float[] origin = new float[2];
        float btvScaleRel = mLauncher.getDragLayer()
                .getDescendantCoordRelativeToSelf(btv, origin);

        Rect iconBounds = new Rect();
        btv.getIconBounds(iconBounds);
        float cx = btv.getWidth() / 2f;
        float cy = btv.getHeight() / 2f;
        float iconSizeOnScreen = Math.max(1, btv.getIconSize()) * btvScaleRel;
        float scale = previewSizeOnScreen / Math.max(1f, iconSizeOnScreen);
        // Never land larger than the open icon; clamp pathological measure failures.
        if (!(scale > 0f) || Float.isNaN(scale)) {
            scale = previewSizeOnScreen / Math.max(1, btv.getIconSize());
        }
        scale = Math.min(scale, 1f);

        // View maps local point p → parent: (p - pivot) * scale + pivot + translation
        float iconLeftAtScale = cx + (iconBounds.left - cx) * scale;
        float iconTopAtScale = cy + (iconBounds.top - cy) * scale;
        float tx = previewLeft - origin[0] - iconLeftAtScale * btvScaleRel;
        float ty = previewTop - origin[1] - iconTopAtScale * btvScaleRel;
        // translation is in the BTV parent's space; convert from DragLayer deltas.
        if (btvScaleRel != 0f) {
            tx /= btvScaleRel;
            ty /= btvScaleRel;
        }

        btv.setTranslationX(saveTx);
        btv.setTranslationY(saveTy);
        btv.setScaleX(saveSx);
        btv.setScaleY(saveSy);
        return new float[]{tx, ty, scale};
    }

    private void playScaleSpringOnGate(ValueAnimator gate, BubbleTextView target,
            float start, float end, float bounce, float response) {
        COUISpringForce force = new COUISpringForce(end)
                .setBounce(bounce)
                .setResponse(response);
        COUISpringAnimation spring = new COUISpringAnimation(new FloatValueHolder(start));
        spring.setSpring(force);
        spring.setStartValue(start);
        spring.setMinimumVisibleChange(COUIDynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
        spring.addUpdateListener((animation, value, velocity) -> {
            target.setScaleX(value);
            target.setScaleY(value);
        });
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

    /** ColorOS: spring drives BubbleTextView.setTextAlpha, not View.ALPHA. */
    private void playTextAlphaSpringOnGate(ValueAnimator gate, BubbleTextView target,
            float start, float end, float bounce, float response) {
        COUISpringForce force = new COUISpringForce(end)
                .setBounce(bounce)
                .setResponse(response);
        COUISpringAnimation spring = new COUISpringAnimation(new FloatValueHolder(start));
        spring.setSpring(force);
        spring.setStartValue(start);
        spring.setMinimumVisibleChange(COUIDynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
        spring.addUpdateListener((animation, value, velocity) -> target.setTextAlpha(value));
        spring.addEndListener((animation, canceled, value, velocity) -> {
            if (!canceled) {
                target.setTextAlpha(end);
            }
        });
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

    /**
     * ColorOS {@code OplusFolderAnimationManager.getFolderHeaderAnimator}: open alpha is remapped
     * so the title stays invisible until the spring progress passes {@link #TEXT_DELAY_PERCENT}.
     */
    private void playHeaderAlphaSpringOnGate(ValueAnimator gate, View header,
            float start, float end, float bounce, float response) {
        COUISpringForce force = new COUISpringForce(end)
                .setBounce(bounce)
                .setResponse(response);
        COUISpringAnimation spring = new COUISpringAnimation(new FloatValueHolder(start));
        spring.setSpring(force);
        spring.setStartValue(start);
        spring.setMinimumVisibleChange(COUIDynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
        spring.addUpdateListener((animation, value, velocity) -> {
            float alpha = value;
            if (mIsOpening) {
                alpha = value < TEXT_DELAY_PERCENT
                        ? 0f
                        : (value - TEXT_DELAY_PERCENT) / (1f - TEXT_DELAY_PERCENT);
            }
            header.setAlpha(alpha);
        });
        spring.addEndListener((animation, canceled, value, velocity) -> {
            if (!canceled) {
                header.setAlpha(mIsOpening ? 1f : 0f);
            }
        });
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

    private void finishSpringsAtFinalPosition() {
        for (IconEndState state : mIconEndStates) {
            state.view.setTranslationX(state.tx);
            state.view.setTranslationY(state.ty);
            state.view.setScaleX(state.scale);
            state.view.setScaleY(state.scale);
            state.view.setAlpha(1f);
            state.view.setTextAlpha(state.textAlpha);
        }
        mIconEndStates.clear();
        for (COUISpringAnimation spring : mRunningSprings) {
            if (spring.isRunning()) {
                if (spring.canSkipToEnd()) {
                    spring.skipToEnd();
                }
                spring.cancel();
            }
        }
        mRunningSprings.clear();
    }

    private void cancelSprings() {
        mIconEndStates.clear();
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
            if (child instanceof BubbleTextView) {
                ((BubbleTextView) child).setTextAlpha(1f);
            }
        }
        if (mFolder.mHeader != null) {
            mFolder.mHeader.setAlpha(1f);
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
