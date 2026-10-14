/*
 * Copyright (C) 2026 The Dream Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.folder;

import static android.text.TextUtils.isEmpty;
import static com.android.launcher3.LauncherState.EDIT_MODE;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.LauncherState.SPRING_LOADED;
import static com.android.launcher3.compat.AccessibilityManagerCompat.sendCustomAccessibilityEvent;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.anim.KeyboardInsetAnimationCallback;
import com.android.launcher3.compat.AccessibilityManagerCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.folder.large.LargeFolderAnimationManager;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.DimenUtils;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.views.FloatingIconView;
import com.android.launcher3.views.OptionsDialogView;
import java.util.List;
import java.util.function.Consumer;

/**
 * ColorOS-style open folder: full-bleed layout on the dimmed wallpaper, Hxy open/close
 * animations, outside-tap and empty long-press handling, inline rename chrome and
 * Overview suppression.
 */
public class DreamFolder extends Folder {

    public static final int ITEM_TYPE_ADD_FOLDER = 1223;

    protected final Launcher mLauncher;
    private int mHeaderHeight;
    public View mHeader;
    float mFolderIconPivotX;
    float mFolderIconPivotY;
    public View mContentLl;
    public ImageView mClearText;

    /** ColorOS OplusFolder: outside-tap dismiss waits for UP without move. */
    private final PointF mActionDownPoint = new PointF();
    private boolean mIsTouchMoveEvent;
    /** Oppo: long-press empty area outside folder content → EDIT_MODE. */
    private boolean mEmptyLongPressEnabled;
    private boolean mEmptyLongPressOccurred;
    private GestureDetector mEmptyLongPressDetector;

    public DreamFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = Launcher.getLauncher(context);
        // Full-bleed folder must receive empty-area taps to dismiss (ColorOS).
        setClickable(true);
        // Oppo OplusFolder GestureDetector: long-press dimmed area outside content → ToggleBar.
        mEmptyLongPressDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public void onLongPress(MotionEvent e) {
                        if (mEmptyLongPressEnabled
                                && handleEmptyLongPressToEditMode()) {
                            mEmptyLongPressOccurred = true;
                            if (mFolderIcon != null) {
                                mFolderIcon.cancelLongPress();
                            }
                        }
                    }
                });
    }

    /**
     * Oppo {@code ToggleBarUtils.handleToToggleBarForFolder}: empty-space long-press inside an
     * open folder enters ToggleBar / {@link LauncherState#EDIT_MODE} without closing the folder.
     * <p>
     * Oppo only {@code goToState(TOGGLE_BAR)}; chrome is shown by ToggleBar enable.
     * Dream mirrors that: state transition only — {@link Launcher#onStateSetEnd} installs
     * OptionsDialog / EditSelection when entering EDIT_MODE (workspace path may already
     * have called {@link Launcher#showDefaultOptions}).
     */
    public boolean handleEmptyLongPressToEditMode() {
        if (mLauncher == null || !mIsOpen || mIsAnimatingClosed) {
            return false;
        }
        // Oppo allowToToggleBar + handleToToggleBarForFolder guards.
        if (!mLauncher.isInState(NORMAL) || mLauncher.isWorkspaceLoading()
                || !mLauncher.hasBeenResumed()) {
            return false;
        }
        if (mLauncher.isInMultiWindowMode()) {
            return false;
        }
        if (mInfo != null && mInfo.isInHotseat()) {
            return false;
        }
        Vibrator vibrator = (Vibrator) mLauncher.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(20);
        }
        mEmptyLongPressOccurred = true;
        mLauncher.getStateManager().goToState(EDIT_MODE);
        return true;
    }

    /** True after empty-area long-press entered edit mode (suppresses empty-cell click dismiss). */
    boolean hasEmptyLongPressOccurred() {
        return mEmptyLongPressOccurred;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isFullBleedOpenFolder()) {
            // ColorOS oplus_user_folder: FolderPagedView paddingTop = pageview_padding_top;
            // L/R content margins are 0; cell L/R/bottom come from folder_page.xml.
            int pagePadTop = getResources().getDimensionPixelSize(
                    R.dimen.folder_pageview_padding_top);
            mContent.setPadding(0, pagePadTop, 0, 0);
        }

        mHeader = findViewById(R.id.folder_header);
        if (isFullBleedOpenFolder()) {
            // ColorOS OplusFolder: indicator floats above content bottom (padding zone).
            ViewGroup.LayoutParams footerLp = mFooter.getLayoutParams();
            if (footerLp instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) footerLp;
                flp.bottomMargin = getResources().getDimensionPixelSize(
                        R.dimen.coloros_folder_page_indicator_bottom_space);
                mFooter.setLayoutParams(flp);
            }
            // ColorOS launcher_page_indicator_select/unselect_color (#FFF / #4DFFFFFF).
            mPageIndicator.setFolderPaginationStyle(ContextCompat.getColor(
                    getContext(), R.color.coloros_folder_page_indicator_selected));
            ViewGroup.LayoutParams indicatorLp = mPageIndicator.getLayoutParams();
            if (indicatorLp != null) {
                indicatorLp.height = getResources().getDimensionPixelSize(
                        R.dimen.coloros_folder_page_indicator_height);
                mPageIndicator.setLayoutParams(indicatorLp);
            }
        }
        if (isFullBleedOpenFolder()) {
            // ColorOS: title sits below status bar inside folder_title_height.
            mFolderName.setPadding(mFolderName.getPaddingLeft(), 0,
                    mFolderName.getPaddingRight(), 0);
            mFolderName.setCursorVisible(false);
            if (Utilities.ATLEAST_R) {
                // ColorOS Folder has no IME lift for open folder; AOSP's
                // KeyboardInsetAnimationCallback also forces translationY = -ime.
                mKeyboardInsetAnimationCallback = null;
                setWindowInsetsAnimationCallback(null);
            }
        } else {
            mFolderName.setPadding(mFolderName.getPaddingLeft(),
                    (mFooterHeight - mFolderName.getLineHeight()) / 2,
                    mFolderName.getPaddingRight(),
                    (mFooterHeight - mFolderName.getLineHeight()) / 2);
        }

        mContentLl = findViewById(R.id.folder_content_ll);
        mClearText = findViewById(R.id.clear_text);
        if (mClearText != null) {
            // ColorOS OplusFolder: clear deletes the Editable then setText("").
            mClearText.setOnClickListener(v -> {
                if (mFolderName == null) {
                    return;
                }
                Editable text = mFolderName.getText();
                if (text != null) {
                    text.delete(0, text.length());
                }
                mFolderName.setText("");
            });
        }
        mFooter.measure(0, 0);
        mHeader.measure(0, 0);
        mContentLl.measure(0, 0);
        mFooterHeight = this.mFooter.getMeasuredHeight();
        if (isFullBleedOpenFolder()) {
            // ColorOS open folder: header is a fixed chrome height (device shows 62dp),
            // not wrap_content from measure(0,0) which collapses to the EditText line.
            mHeaderHeight = getResources().getDimensionPixelSize(
                    R.dimen.coloros_folder_title_height);
            // ColorOS folder_name_text_size = 24sp (do not use scaled folderLabelTextSizePx).
            mFolderName.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.folder_label_text_size));
        } else {
            mHeaderHeight = mHeader.getMeasuredHeight();
        }
    }

    @Override
    public void startEditingFolderName() {
        post(() -> {
            // ColorOS OplusFolder.startEditingFolderName:
            // clear visible, selectAll, cursor on, rename capsule on header.
            if (isFullBleedOpenFolder()) {
                mFolderName.setHint("");
                mFolderName.setCursorVisible(true);
                mFolderName.selectAll();
                if (mClearText != null) {
                    mClearText.setVisibility(VISIBLE);
                }
                if (mHeader != null) {
                    mHeader.setBackgroundResource(R.drawable.folder_rename_edittext_background);
                }
            } else {
                showLabelSuggestions();
                mFolderName.setHint("");
            }
            mIsEditingName = true;
        });
    }

    @Override
    public boolean onBackKey() {
        if (!isFullBleedOpenFolder()) {
            return super.onBackKey();
        }
        // ColorOS OplusFolder.onBackKey: empty edit restores previous title;
        // only persist when the text actually changed.
        CharSequence previous = mInfo.title;
        Editable text = mFolderName.getText();
        if (TextUtils.isEmpty(text)) {
            mFolderName.setText(previous);
        } else if (!TextUtils.equals(previous, text)) {
            String newTitle = text.toString();
            mInfo.setTitle(newTitle, mLauncherDelegate.getModelWriter());
            mFolderIcon.onTitleChanged(newTitle);
            mFolderName.setText(newTitle);
            sendCustomAccessibilityEvent(
                    this, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    getContext().getString(R.string.folder_renamed, newTitle));
        }

        if (mClearText != null) {
            mClearText.setVisibility(INVISIBLE);
        }
        mFolderName.setCursorVisible(false);
        if (mHeader != null) {
            mHeader.setBackground(null);
        }

        mFolderName.clearFocus();
        Selection.setSelection(mFolderName.getText(), 0, 0);
        mIsEditingName = false;
        return true;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        if (!isFullBleedOpenFolder()) {
            return super.onApplyWindowInsets(windowInsets);
        }
        // ColorOS full-bleed folder keeps title chrome pinned; IME covers the lower icons
        // instead of translating the whole folder (which pushed the title off-screen).
        if (Utilities.ATLEAST_R) {
            setTranslationY(0);
        }
        return windowInsets;
    }

    /**
     * Opens the user folder described by the specified tag. The opening of the folder
     * is animated relative to the specified View. If the View is null, no animation
     * is played.
     */
    @Override
    protected void animateOpen(List<WorkspaceItemInfo> items, int pageNo, boolean addFlag) {
        FloatingIconView.finishExitFloatingIconView((Launcher) mActivityContext);
        Folder openFolder = getOpen(mActivityContext);
        if (openFolder != null && openFolder != this) {
            // Close any open folder before opening a folder.
            openFolder.close(true);
        }

        mContent.bindItems(items, addFlag);
        centerAboutIcon();
        mItemsInvalidated = true;
        updateTextViewFocus();

        mIsOpen = true;

        BaseDragLayer dragLayer = mActivityContext.getDragLayer();
        // Just verify that the folder hasn't already been added to the DragLayer.
        // There was a one-off crash where the folder had a parent already.
        if (getParent() == null) {
            dragLayer.addView(this);
            mDragController.addDropTarget(this);
        } else {
            if (FeatureFlags.IS_STUDIO_BUILD) {
                Log.e(TAG, "Opening folder (" + this + ") which already has a parent:"
                        + getParent());
            }
        }

        mContent.completePendingPageChanges();
        mContent.setCurrentPage(pageNo);

        // This is set to true in close(), but isn't reset to false until onDropCompleted(). This
        // leads to an inconsistent state if you drag out of the folder and drag back in without
        // dropping. One resulting issue is that replaceFolderWithFinalItem() can be called twice.
        mDeleteFolderOnDropCompleted = false;

        cancelRunningAnimations();
        AnimatorSet anim = new LargeFolderAnimationManager(this, true /* isOpening */).getAnimator(new Consumer<Animator>() {
            @Override
            public void accept(Animator animator) {
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mFolderIcon.setIconVisible(false);
                        mFolderIcon.drawLeaveBehindIfExists();
                        mFolderIcon.setForceHideDot(true);
                        mLauncher.setLauncherBlurBg(true);
                        // Hide bottom ToggleBar without exiting EDIT_MODE. Using
                        // closeOpenViews → OptionsDialog.onClosed would goToState(NORMAL)
                        // mid-open and leave Workspace alpha at 0 after companion fade.
                        if (mLauncher != null && mLauncher.isInState(EDIT_MODE)) {
                            OptionsDialogView.hidePreservingEditMode(mLauncher);
                            View hotseat = mLauncher.getHotseat();
                            if (hotseat != null) {
                                hotseat.setAlpha(0f);
                                hotseat.setVisibility(INVISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setState(STATE_OPEN);
                        announceAccessibilityChanges();
                        AccessibilityManagerCompat.sendFolderOpenedEventToTest(getContext());

                        mContent.setFocusOnFirstChild();
                        if (mLauncher != null && mLauncher.isInState(EDIT_MODE)) {
                            // Re-layout bottom-aligned after edit-chrome removal.
                            centerAboutIcon();
                            requestLayout();
                        }
                    }
                });
            }
        });

        // Footer animation
        if (mContent.getPageCount() > 1 && !mInfo.hasOption(FolderInfo.FLAG_MULTI_PAGE_ANIMATION)) {
            int footerWidth = mContent.getDesiredWidth()
                    - mFooter.getPaddingLeft() - mFooter.getPaddingRight();

            float textWidth = mFolderName.getPaint().measureText(mFolderName.getText().toString());
            float translation = (footerWidth - textWidth) / 2;
            mFolderName.setTranslationX(mContent.mIsRtl ? -translation : translation);
            mPageIndicator.prepareEntryAnimation();

            // Do not update the flag if we are in drag mode. The flag will be updated, when we
            // actually drop the icon.
            final boolean updateAnimationFlag = !mDragInProgress;
            anim.addListener(new AnimatorListenerAdapter() {

                @SuppressLint("InlinedApi")
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFolderName.animate().setDuration(FOLDER_NAME_ANIMATION_DURATION)
                            .translationX(0)
                            .setInterpolator(AnimationUtils.loadInterpolator(
                                    getContext(), android.R.interpolator.fast_out_slow_in));
                    mPageIndicator.playEntryAnimation();

                    if (updateAnimationFlag) {
                        mInfo.setOption(FolderInfo.FLAG_MULTI_PAGE_ANIMATION, true,
                                mLauncherDelegate.getModelWriter());
                    }
                }
            });
        } else {
            mFolderName.setTranslationX(0);
        }

        mPageIndicator.stopAllAnimations();

        // b/282158620 because setCurrentPlayTime() below will start animator, we need to register
        // {@link AnimatorListener} before it so that {@link AnimatorListener#onAnimationStart} can
        // be called to register mCurrentAnimator, which will be used to cancel animator
        addAnimationStartListeners(anim);
        // Hxy open applies per-icon start poses before start(); seeking would advance springs
        // before those poses are applied and makes large folders look wrong.
        anim.start();

        // Make sure the folder picks up the last drag move even if the finger doesn't move.
        if (mDragController.isDragging()) {
            mDragController.forceTouchMove();
        }
        mContent.verifyVisibleHighResIcons(mContent.getNextPage());
    }

    @Override
    protected void handleClose(boolean animate) {
        mIsOpen = false;
        if (mLauncher != null) {
            mLauncher.setRetainOpenFolderThroughOverview(false);
        }

        if (!animate && mCurrentAnimator != null && mCurrentAnimator.isRunning()) {
            mCurrentAnimator.cancel();
        }

        if (isEditingName()) {
            mFolderName.dispatchBackKey();
        }

        if (mFolderIcon != null) {
            mFolderIcon.clearLeaveBehindIfExists();
        }

        if (animate) {
            animateClosed();
        } else {
            // Non-animated close must still undo open-companion transforms. Hxy open
            // sets Workspace/Hotseat View.SCALE_* / ALPHA directly; skipping that leave
            // the desktop stuck faded/scaled (looks like a black/broken home).
            restoreWorkspaceAfterFolderDismiss();
            closeComplete(false);
            post(this::announceAccessibilityChanges);
            mLauncher.setLauncherBlurBg(false);
        }

        // Notify the accessibility manager that this folder "window" has disappeared and no
        // longer occludes the workspace items
        mActivityContext.getDragLayer().sendAccessibilityEvent(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    /**
     * Snap workspace chrome back after a non-animated folder dismiss.
     * Matches the end state of {@link LargeFolderAnimationManager} close companions.
     * When editing ({@link LauncherState#SPRING_LOADED}), reapply state scale/ty —
     * do not force normal-desktop scale 1.0 (that breaks the toggle-bar layout).
     * Always restore companion {@code View.ALPHA}: open-folder fades Workspace itself to 0,
     * and {@code reapplyState} only touches per-page container alphas — not Workspace alpha.
     */
    private void restoreWorkspaceAfterFolderDismiss() {
        if (mLauncher == null) {
            return;
        }
        restoreWorkspaceCompanionAlpha();
        if (mLauncher.isInState(SPRING_LOADED)
                || mLauncher.isInState(EDIT_MODE)) {
            mLauncher.getStateManager().reapplyState(false);
            // reapplyState does not reset Workspace View.ALPHA after folder open fade.
            restoreWorkspaceCompanionAlpha();
            resetPivot();
            return;
        }
        Workspace<?> workspace = mLauncher.getWorkspace();
        View hotseat = mLauncher.getHotseat();
        View pageIndicator = workspace != null ? workspace.getPageIndicator() : null;
        if (workspace != null) {
            workspace.setScaleX(1f);
            workspace.setScaleY(1f);
            workspace.setTranslationX(0f);
            workspace.setTranslationY(0f);
        }
        if (hotseat != null) {
            hotseat.setScaleX(1f);
            hotseat.setScaleY(1f);
            hotseat.setTranslationX(0f);
            hotseat.setTranslationY(0f);
        }
        if (pageIndicator != null) {
            pageIndicator.setScaleX(1f);
            pageIndicator.setScaleY(1f);
        }
        resetPivot();
    }

    /**
     * Undo {@link LargeFolderAnimationManager} open companion fade (Workspace/Hotseat α → 0).
     * Also restores page {@link ShortcutAndWidgetContainer} alphas that Overview→NORMAL
     * may have forced to 0 while a folder stayed open.
     */
    public void restoreWorkspaceCompanionAlpha() {
        if (mLauncher == null) {
            return;
        }
        Workspace<?> workspace = mLauncher.getWorkspace();
        View hotseat = mLauncher.getHotseat();
        View pageIndicator = workspace != null ? workspace.getPageIndicator() : null;
        if (workspace != null) {
            workspace.setAlpha(1f);
            final int pageCount = workspace.getChildCount();
            for (int i = 0; i < pageCount; i++) {
                View page = workspace.getChildAt(i);
                if (page instanceof CellLayout) {
                    View container = ((CellLayout) page).getShortcutsAndWidgets();
                    if (container != null) {
                        container.setAlpha(1f);
                    }
                }
            }
        }
        if (hotseat != null) {
            // Keep hidden if edit-mode long-press already hid the dock.
            if (hotseat.getVisibility() == VISIBLE) {
                hotseat.setAlpha(1f);
            }
        }
        if (pageIndicator != null && !mLauncher.getStateManager().getState().overviewUi) {
            pageIndicator.setAlpha(1f);
        }
    }

    @Override
    protected void animateClosed() {
        if (mIsAnimatingClosed) {
            return;
        }

        mContent.completePendingPageChanges();
        mContent.snapToPageImmediately(mContent.getDestinationPage());

        cancelRunningAnimations();
        AnimatorSet a = new LargeFolderAnimationManager(this, false).getAnimator(new Consumer<Animator>() {
            @Override
            public void accept(Animator animator) {
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (Utilities.ATLEAST_R) {
                            setWindowInsetsAnimationCallback(null);
                        }
                        mIsAnimatingClosed = true;
                        // ColorOS clears dim with content restore, not before icon springs start.
                        mLauncher.setLauncherBlurBg(false);
                        // Oppo Folder.animateClosed: clear in-folder drop outlines.
                        clearFolderDropOutlines();
                        if (mFolderIcon != null) {
                            // Suppress preview items before making the plate visible so the
                            // first close frame cannot double-draw preview + Folder children.
                            mFolderIcon.onFolderAnimStartClose(
                                    DreamFolder.this.mContent.getCurrentPage());
                            mFolderIcon.setIconVisible(true);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (Utilities.ATLEAST_R && mKeyboardInsetAnimationCallback != null) {
                            setWindowInsetsAnimationCallback(mKeyboardInsetAnimationCallback);
                        }
                        closeComplete(true);
                        announceAccessibilityChanges();
                        mIsAnimatingClosed = false;
                        // Stay hidden in edit-selection so notification badges do not clash
                        // with checkmarks (Oppo SelectStateIconRenderer.hideDot).
                        boolean keepHidden = false;
                        if (mActivityContext instanceof Launcher launcher) {
                            keepHidden = launcher.getEditSelectionManager().isActive();
                        }
                        mFolderIcon.setForceHideDot(keepHidden);
                        resetPivot();
                        // Ensure SPRING_LOADED scale/ty survive close (Oppo endTransition).
                        if (mLauncher.isInState(SPRING_LOADED)
                                || mLauncher.isInState(EDIT_MODE)) {
                            mLauncher.getStateManager().reapplyState(false);
                        }
                        // Oppo: closing folder while still in TOGGLE_BAR restores bottom ToggleBar.
                        if (mLauncher.isInState(EDIT_MODE)
                                && mLauncher.getEditSelectionManager().isActive()
                                && !mLauncher.getTransitionEffectsManager().isEffectSession()
                                && AbstractFloatingView.getOpenView(mLauncher,
                                        AbstractFloatingView.TYPE_OPTIONS_POPUP_DIALOG) == null) {
                            OptionsDialogView.show(mLauncher, null);
                        }
                    }
                });
            }
        });

        Object o = this.mFolderName.getTag();
        if (o instanceof AnimatorSet) {
            ((AnimatorSet) o).pause();
            this.mFolderName.setAlpha(0.0f);
        }
        addAnimationStartListeners(a);
        a.start();
    }

    public void resetPivot() {
        int pageWidth = this.mLauncher.getWorkspace().getMeasuredWidth();
        int pageHeight = this.mLauncher.getWorkspace().getMeasuredHeight();
        if (!this.mLauncher.getDeviceProfile().isMultiWindowMode) {
            this.mLauncher.getWorkspace().setPivotY(((float) pageHeight) / 2.0f);
            this.mLauncher.getWorkspace().setPivotX(((float) pageWidth) / 2.0f);
        }
        View tempView = mLauncher.getHotseat();
        tempView.setPivotX((float) (tempView.getMeasuredWidth() / 2));
        tempView.setPivotY((float) (tempView.getMeasuredHeight() / 2));
    }

    @Override
    public void closeComplete(boolean wasAnimated) {
        // TODO: Clear all active animations.
        BaseDragLayer parent = (BaseDragLayer) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
        mDragController.removeDropTarget(this);
        clearFocus();
        if (mFolderIcon != null) {
            mFolderIcon.setVisibility(View.VISIBLE);
            mFolderIcon.setIconVisible(true);
            mFolderIcon.mFolderName.setTextVisibility(true);
            // Always clear preview suppress (large-list / small PreviewItemManager).
            // Gating this on wasAnimated left icons blank after Recents empty-tap close(false).
            mFolderIcon.onFolderClose(mContent.getCurrentPage());
            if (wasAnimated) {
                mFolderIcon.animateBgShadowAndStroke();
                if (mFolderIcon.hasDot()) {
                    mFolderIcon.animateDotScale(0f, 1f);
                }
                mFolderIcon.requestFocus();
            }
            // Refresh Oppo selection count badge after folder dismiss.
            if (mActivityContext instanceof Launcher launcher
                    && launcher.getEditSelectionManager().isActive()) {
                mFolderIcon.invalidate();
            }
        }

        if (mRearrangeOnClose) {
            rearrangeChildren();
            mRearrangeOnClose = false;
        }
        if (getItemCount() <= 1) {
            if (!mDragInProgress && !mSuppressFolderDeletion) {
                replaceFolderWithFinalItem();
            } else if (mDragInProgress) {
                mDeleteFolderOnDropCompleted = true;
            }
        } else if (!mDragInProgress) {
            mContent.unbindItems();
        }
        mSuppressFolderDeletion = false;
        clearDragInfo();
        setState(STATE_CLOSED);
        mContent.setCurrentPage(0);
    }

    @Override
    public void onDragEnter(DragObject d) {
        super.onDragEnter(d);
        // Oppo: folder CellLayout must be in-drag so drop chips can paint.
        CellLayout page = mContent != null ? mContent.getCurrentCellLayout() : null;
        if (page != null) {
            page.onDragEnter();
        }
    }

    @Override
    public void onDragOver(DragObject d) {
        boolean scrollPaused = mScrollPauseAlarm.alarmPending();
        super.onDragOver(d);
        if (!scrollPaused) {
            // Oppo: show drop outline while reordering inside the open folder.
            visualizeFolderDropLocation(d);
        }
    }

    /** Oppo OplusFolder.onDragOver: soft drop pad under the empty / target cell. */
    private void visualizeFolderDropLocation(DragObject d) {
        if (mContent == null || !mContent.rankOnCurrentPage(mEmptyCellRank)) {
            return;
        }
        CellLayout page = mContent.getCurrentCellLayout();
        if (page == null) {
            return;
        }
        int countX = page.getCountX();
        if (countX <= 0) {
            return;
        }
        int maxPerPage = mContent.itemsPerPage();
        int pageRank = mTargetRank % maxPerPage;
        if (pageRank < 0) {
            pageRank += maxPerPage;
        }
        page.visualizeDropLocation(pageRank % countX, pageRank / countX, 1, 1, d);
    }

    private void clearFolderDropOutlines() {
        if (mContent == null) {
            return;
        }
        CellLayout page = mContent.getCurrentCellLayout();
        if (page != null) {
            page.clearDragOutlines();
            page.onDragExit();
        }
    }

    @Override
    public void onDragExit(DragObject d) {
        super.onDragExit(d);
        clearFolderDropOutlines();
    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean success) {
        if (success && getItemCount() <= 1) {
            mDeleteFolderOnDropCompleted = true;
        }
        super.onDropCompleted(target, d, success);
    }

    @Override
    protected void centerAboutIcon() {
        BaseDragLayer.LayoutParams lp = (BaseDragLayer.LayoutParams) getLayoutParams();
        BaseDragLayer parent = mActivityContext.getDragLayer();
        DragLayer dragLayer = mLauncher.getDragLayer();

        // ColorOS OplusFolder.centerAboutIcon: bottom-align measured content above nav inset.
        // Do not clamp to coloros_folder_top_offset — that pinned sparse folders mid-screen.
        if (isFullBleedOpenFolder()) {
            int width = getMeasuredWidth() > 0 ? getMeasuredWidth() : getFolderWidth();
            int height = getMeasuredHeight() > 0 ? getMeasuredHeight() : getFolderHeight();
            if (width <= 0) {
                width = mLauncher.getDeviceProfile().availableWidthPx;
            }
            if (height <= 0) {
                height = getFolderHeight();
            }
            int bottomInset = mLauncher.getDeviceProfile().getInsets().bottom;
            int topInset = mLauncher.getDeviceProfile().getInsets().top;
            int x = (dragLayer.getWidth() - width) / 2;
            // Oppo: y = (dragLayerH - bottomInset) - measuredHeight
            int y = Math.max(topInset, (dragLayer.getHeight() - bottomInset) - height);
            Rect iconRectInDragLayer = sTempRect;
            dragLayer.getDescendantRectRelativeToSelf(mFolderIcon, iconRectInDragLayer);
            Rect iconContentRect = mFolderIcon.getIconRect();
            iconRectInDragLayer.offset(iconContentRect.left, iconContentRect.top);
            iconRectInDragLayer.bottom = iconRectInDragLayer.top + iconContentRect.height();
            iconRectInDragLayer.right = iconRectInDragLayer.left + iconContentRect.width();
            setPivotX(iconRectInDragLayer.centerX() - x);
            setPivotY(iconRectInDragLayer.centerY() - y);
            mFolderIconPivotX = mFolderIcon.getMeasuredWidth() / 2f;
            mFolderIconPivotY = mFolderIcon.getMeasuredHeight() / 2f;
            lp.width = width;
            lp.height = height;
            lp.x = x;
            lp.y = y;
            mFolderIcon.setPivotX(mFolderIconPivotX);
            mFolderIcon.setPivotY(mFolderIconPivotY);
            return;
        }

        int width = getFolderWidth();
        int height = getFolderHeight();

        parent.getDescendantRectRelativeToSelf(mFolderIcon, sTempRect);
        int centerX = sTempRect.centerX();
        int centerY = sTempRect.centerY();
        int centeredLeft = centerX - width / 2;
        int centeredTop = centerY - height / 2;

        sTempRect.set(mActivityContext.getFolderBoundingBox());
        int left = Utilities.boundToRange(centeredLeft, sTempRect.left, sTempRect.right - width);
        int top = Utilities.boundToRange(centeredTop, sTempRect.top, sTempRect.bottom - height);
        int[] inOutPosition = new int[]{left, top};
        mActivityContext.updateOpenFolderPosition(inOutPosition, sTempRect, width, height);
        left = inOutPosition[0];
        top = inOutPosition[1];

        Rect iconRectInDragLayer = sTempRect;

        dragLayer.getDescendantRectRelativeToSelf(mFolderIcon, iconRectInDragLayer);
        Rect iconContentRect = mFolderIcon.getIconRect();
        iconRectInDragLayer.offset(iconContentRect.left, iconContentRect.top);
        iconRectInDragLayer.bottom = iconRectInDragLayer.top + iconContentRect.height();
        iconRectInDragLayer.right = iconRectInDragLayer.left + iconContentRect.width();

        int targetX = mLauncher.getWorkspace().getPageAreaRelativeToDragLayer().centerX() - (width / 2);
        int targetY = mLauncher.getWorkspace().getPageAreaRelativeToDragLayer().centerY() - (height / 2);

        Rect availableArea;
        LauncherState currentState = (LauncherState) mLauncher.getStateManager().getState();
        if (currentState == LauncherState.OVERVIEW) {
            View overviewPanel = mLauncher.getOverviewPanel();
            availableArea = new Rect();
            dragLayer.getDescendantRectRelativeToSelf(overviewPanel, availableArea);
        } else {
            availableArea = mLauncher.getWorkspace().getPageAreaRelativeToDragLayer();
        }

        targetX = (DimenUtils.getScreenWidth(mLauncher) - width) / 2;
        targetY = Math.max(availableArea.top, Math.min(targetY, availableArea.bottom - height));

        if (mLauncher.getDeviceProfile().isPhone) {
            int maxAvailableWidth = mLauncher.getDeviceProfile().availableWidthPx - width;
            int workspacePaddingWithFolder = mLauncher.getWorkspace().getPaddingLeft() + getPaddingLeft();
            if (maxAvailableWidth < workspacePaddingWithFolder * 4) {
                targetX = (mLauncher.getDeviceProfile().availableWidthPx - width) / 2;
            }
        }

        int folderPivotX = width / 2 + (centeredLeft - left);
        int folderPivotY = height / 2 + (centeredTop - top);
        int iconCenterX = iconRectInDragLayer.centerX();
        int iconCenterY = iconRectInDragLayer.centerY();
        setPivotX(iconCenterX);
        setPivotY(iconCenterY);

        float folderWidthScale = (float) targetX / width;
        mFolderIconPivotX = mFolderIcon.getMeasuredWidth() * folderWidthScale;

        float folderHeightScale = (float) targetY / height;
        mFolderIconPivotY = mFolderIcon.getMeasuredHeight() * folderHeightScale;

        lp.width = width;
        lp.height = height;
        lp.x = targetX;
        lp.y = targetY;

        // mBackground.setBounds(0, 0, width, height);
        mFolderIcon.setPivotX(mFolderIconPivotX);
        mFolderIcon.setPivotY(mFolderIconPivotY);
    }

    @Override
    protected int getContentAreaHeight() {
        DeviceProfile grid = mActivityContext.getDeviceProfile();
        int maxContentAreaHeight;
        if (isFullBleedOpenFolder()) {
            // ColorOS OplusFolder.getContentAreaHeight
            Rect insets = grid.getInsets();
            maxContentAreaHeight = grid.availableHeightPx - insets.top - insets.bottom
                    - mFooterHeight;
        } else {
            maxContentAreaHeight = grid.availableHeightPx - grid.getTotalWorkspacePadding().y
                    - mFooterHeight;
        }
        int height = Math.min(maxContentAreaHeight,
                mContent.getDesiredHeight());
        return Math.max(height, MIN_CONTENT_DIMEN);
    }

    @Override
    protected int getFolderHeight(int contentAreaHeight) {
        if (isFullBleedOpenFolder()) {
            // ColorOS: indicator overlays content; height excludes separate footer stack.
            int wrapperTop = getResources().getDimensionPixelSize(
                    R.dimen.folder_content_wrapper_margin_top);
            return getPaddingTop() + getPaddingBottom() + contentAreaHeight
                    + mHeaderHeight + wrapperTop;
        }
        return getPaddingTop() + getPaddingBottom() + contentAreaHeight + mFooterHeight
                + mHeaderHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int contentWidth = getContentAreaWidth();
        int contentHeight = getContentAreaHeight();

        int contentAreaWidthSpec = MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY);
        int contentAreaHeightSpec = MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY);

        mContent.setFixedSize(contentWidth, contentHeight);
        mContent.measure(contentAreaWidthSpec, contentAreaHeightSpec);

        if (mContent.getChildCount() > 0) {
            int cellIconGap = (this.mContent.getPageAt(0).getCellWidth() - this.mActivityContext.getDeviceProfile().iconSizePx) / 2;
            mFooter.setPadding(this.mContent.getPaddingLeft() + cellIconGap, this.mFooter.getPaddingTop(), this.mContent.getPaddingRight() + cellIconGap, this.mFooter.getPaddingBottom());
        }

        if (isFullBleedOpenFolder()) {
            // ColorOS OplusFolder.onMeasure:
            // folderHeight = folder_content_wrapper_margin_top + headerH + contentH (+ paddings)
            // Page indicator overlays content (FrameLayout), so do not add footer to height.
            int wrapperTop = getResources().getDimensionPixelSize(
                    R.dimen.folder_content_wrapper_margin_top);
            int folderWidth = getPaddingLeft() + getPaddingRight() + contentWidth;
            int folderHeight = wrapperTop + mHeaderHeight + getPaddingTop() + getPaddingBottom()
                    + contentHeight;
            int headerMargin = getResources().getDimensionPixelSize(
                    R.dimen.coloros_folder_header_margin_horizontal);
            int headerWidthSpec = MeasureSpec.makeMeasureSpec(
                    Math.max(0, folderWidth - 2 * headerMargin), MeasureSpec.EXACTLY);
            mHeader.measure(headerWidthSpec,
                    View.MeasureSpec.makeMeasureSpec(this.mHeaderHeight, MeasureSpec.EXACTLY));
            mFooter.measure(contentAreaWidthSpec,
                    View.MeasureSpec.makeMeasureSpec(this.mFooterHeight, MeasureSpec.EXACTLY));
            mContentLl.measure(contentAreaWidthSpec,
                    View.MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY));
            setMeasuredDimension(folderWidth, folderHeight);
            return;
        }

        mFooter.measure(contentAreaWidthSpec, View.MeasureSpec.makeMeasureSpec(this.mFooterHeight, MeasureSpec.EXACTLY));
        mHeader.measure(contentAreaWidthSpec, View.MeasureSpec.makeMeasureSpec(this.mHeaderHeight, MeasureSpec.EXACTLY));
        mContentLl.measure(contentAreaWidthSpec, View.MeasureSpec.makeMeasureSpec(this.mFooterHeight + contentHeight, MeasureSpec.EXACTLY));
        setMeasuredDimension(getPaddingLeft() + getPaddingRight() + contentWidth, getFolderHeight(contentHeight));
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        // ColorOS: while Overview is up the folder stays open but must not steal
        // empty-space taps — RecentsView needs them for startHome().
        if (isHiddenForOverview()) {
            return false;
        }
        BaseDragLayer dl = (BaseDragLayer) getParent();
        if (dl == null) {
            return false;
        }

        final int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mActionDownPoint.set(ev.getX(), ev.getY());
            mIsTouchMoveEvent = false;
            mEmptyLongPressOccurred = false;

            if (isEditingName()) {
                // ColorOS OplusFolder: clear button is inside rename chrome — do not end edit.
                boolean overName = dl.isEventOverView(mFolderName, ev);
                boolean overClear = mClearText != null
                        && mClearText.getVisibility() == VISIBLE
                        && dl.isEventOverView(mClearText, ev);
                if (!overName && !overClear) {
                    mFolderName.dispatchBackKey();
                    return true;
                }
                mEmptyLongPressEnabled = false;
                return false;
            }

            // Oppo: enable folder-chrome empty long-press only outside name + content.
            boolean overName = mFolderName != null && dl.isEventOverView(mFolderName, ev);
            boolean overContent = mContent != null && dl.isEventOverView(mContent, ev);
            mEmptyLongPressEnabled = !overName && !overContent;
            if (mEmptyLongPressDetector != null) {
                mEmptyLongPressDetector.onTouchEvent(ev);
            }

            // Steal the gesture when empty long-press is armed. Otherwise the drawing-hidden
            // FolderIcon under the dim still receives the same long-press and opens showForFolder.
            if (mEmptyLongPressEnabled) {
                if (mFolderIcon != null) {
                    mFolderIcon.cancelLongPress();
                }
                return true;
            }

            if (!isFullBleedOpenFolder()
                    && !dl.isEventOverView(this, ev)
                    && mLauncherDelegate.interceptOutsideTouch(ev, dl, this)) {
                return true;
            }
            // ColorOS: outside / empty dismiss is decided on UP (not DOWN).
            return false;
        }

        if (mEmptyLongPressDetector != null) {
            mEmptyLongPressDetector.onTouchEvent(ev);
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (!mIsTouchMoveEvent) {
                int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if (Math.abs(ev.getX() - mActionDownPoint.x) >= slop
                        || Math.abs(ev.getY() - mActionDownPoint.y) >= slop) {
                    mIsTouchMoveEvent = true;
                }
            }
            return false;
        }

        if (action == MotionEvent.ACTION_UP && isFullBleedOpenFolder() && !isEditingName()) {
            // ColorOS OplusFolder: tap dimmed wallpaper outside folder chrome closes on UP.
            if (!dl.isEventOverView(this, ev) && !mIsTouchMoveEvent && !mEmptyLongPressOccurred
                    && mIsOpen) {
                close(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        if (isHiddenForOverview()) {
            return false;
        }
        BaseDragLayer dl = (BaseDragLayer) getParent();
        if (dl == null) {
            return false;
        }
        // Active controller after empty-area steal — keep feeding long-press + dismiss.
        if (mEmptyLongPressDetector != null) {
            mEmptyLongPressDetector.onTouchEvent(ev);
        }
        final int action = ev.getAction();
        if (action == MotionEvent.ACTION_MOVE && !mIsTouchMoveEvent) {
            int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (Math.abs(ev.getX() - mActionDownPoint.x) >= slop
                    || Math.abs(ev.getY() - mActionDownPoint.y) >= slop) {
                mIsTouchMoveEvent = true;
            }
        }
        if (action == MotionEvent.ACTION_UP && isFullBleedOpenFolder()
                && !isEditingName() && mIsOpen
                && !mIsTouchMoveEvent && !mEmptyLongPressOccurred) {
            if (!dl.isEventOverView(this, ev)) {
                close(true);
            } else {
                // Empty chrome inside folder bounds (name/content already excluded on steal).
                completeDragExit();
            }
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isHiddenForOverview()) {
            return false;
        }
        // Keep move tracking when the gesture is handled by the folder view itself.
        if (event.getAction() == MotionEvent.ACTION_MOVE && !mIsTouchMoveEvent) {
            int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (Math.abs(event.getX() - mActionDownPoint.x) >= slop
                    || Math.abs(event.getY() - mActionDownPoint.y) >= slop) {
                mIsTouchMoveEvent = true;
            }
        }
        // ColorOS OplusFolder.onTouchEvent: empty chrome UP → close via completeDragExit.
        if (isFullBleedOpenFolder()
                && !isEditingName()
                && event.getAction() == MotionEvent.ACTION_UP
                && mIsOpen
                && !mIsTouchMoveEvent
                && !mEmptyLongPressOccurred) {
            completeDragExit();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean canInterceptEventsInSystemGestureRegion() {
        return !isHiddenForOverview();
    }

    /**
     * True while Overview owns the screen: folder remains open (ColorOS) but is not interactive.
     */
    private boolean isHiddenForOverview() {
        return mLauncher != null && mLauncher.getStateManager().getState().overviewUi;
    }

    /** Show/hide for Overview without closing — alpha alone still receives touches. */
    public void setSuppressedForOverview(boolean suppressed) {
        if (suppressed) {
            setAlpha(0f);
            setVisibility(INVISIBLE);
            if (mLauncher != null) {
                mLauncher.setRetainOpenFolderThroughOverview(true);
            }
        } else if (mIsOpen) {
            setVisibility(VISIBLE);
            setAlpha(1f);
            // Overview→NORMAL resets workspace chrome; re-apply open-folder companions + dim.
            applyOpenWorkspaceCompanions();
            if (mLauncher != null) {
                mLauncher.setLauncherBlurBg(true);
            }
        }
    }

    /**
     * Snap workspace to ColorOS open-folder companion end state (faded / slightly scaled).
     * Overview state transitions otherwise restore full desktop under an open folder.
     */
    public void applyOpenWorkspaceCompanions() {
        if (mLauncher == null) {
            return;
        }
        final float scale = 0.92f;
        Workspace<?> workspace = mLauncher.getWorkspace();
        View hotseat = mLauncher.getHotseat();
        View pageIndicator = workspace != null ? workspace.getPageIndicator() : null;
        if (workspace != null) {
            workspace.setAlpha(0f);
            workspace.setScaleX(scale);
            workspace.setScaleY(scale);
        }
        if (hotseat != null) {
            hotseat.setAlpha(0f);
            hotseat.setScaleX(scale);
            hotseat.setScaleY(scale);
        }
        if (pageIndicator != null) {
            pageIndicator.setAlpha(0f);
            pageIndicator.setScaleX(scale);
            pageIndicator.setScaleY(scale);
        }
    }

    @Override
    protected boolean hasBackgroundCard() {
        // No frosted plate — content floats on the darkened wallpaper (ColorOS).
        return !isFullBleedOpenFolder();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mClipPath == null && hasBackgroundCard()) {
            mBackground.setColor(ContextCompat.getColor(getContext(),
                    R.color.hxy_folder_bg_round_color));
        }
        super.dispatchDraw(canvas);
    }

    /**
     * ColorOS-like open folder: fill the drag layer, no frosted card, icons float on dark dim.
     * Hxy full-folder style enables this.
     */
    public boolean isFullBleedOpenFolder() {
        return getResources().getBoolean(R.bool.config_show_full_folder_style);
    }
}
