package com.android.launcher3.togglebar;

import static com.android.launcher3.LauncherState.EDIT_MODE;
import static com.android.launcher3.LauncherState.NORMAL;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.screenedit.GridGallery;
import com.android.launcher3.screenedit.ScrollEffectAdapter;
import com.android.launcher3.util.SystemBarHelper;

/**
 * Minimal OPPO {@code ToggleBarManager} contract for Transitions only:
 * <ul>
 *   <li>{@code mPendingState = "effect"} then {@code goToState(EDIT_MODE)}</li>
 *   <li>On state enable, inflate the strip into {@code R.id.toggle_bar_root}</li>
 *   <li>Strip lifetime is owned by this manager — not an {@code AbstractFloatingView}</li>
 * </ul>
 */
public final class TransitionEffectsManager {

    private static final String TAG = "TransitionEffects";
    public static final String EFFECT_STATE = "effect";

    private final Launcher mLauncher;
    @Nullable private String mPendingState;
    private boolean mActive;
    private boolean mApplied;
    @Nullable private View mContentView;
    @Nullable private ScrollEffectAdapter mAdapter;
    @Nullable private GridGallery mGallery;

    public TransitionEffectsManager(Launcher launcher) {
        mLauncher = launcher;
    }

    public boolean hasPendingState() {
        return !TextUtils.isEmpty(mPendingState);
    }

    public boolean isActive() {
        return mActive;
    }

    /** Pending entry or live effect strip — drives EditMode visible elements. */
    public boolean isEffectSession() {
        return mActive || hasPendingState();
    }

    /**
     * OPPO {@code gotoToggleBarOnLauncherNewIntent("effect")}: arm pending, hide hotseat,
     * enter edit/ToggleBar-equivalent state; UI is created from {@link #onEditModeEnabled()}.
     */
    public void gotoEffectOnNewIntent() {
        mPendingState = EFFECT_STATE;
        mApplied = false;
        if (mLauncher.getWorkspace() != null && mLauncher.getWorkspace().getHotseat() != null) {
            mLauncher.getWorkspace().getHotseat().setAlpha(0f);
        }
        Log.i(TAG, "gotoEffectOnNewIntent pending=effect state="
                + mLauncher.getStateManager().getState());
        if (mLauncher.getStateManager().isInStableState(EDIT_MODE)) {
            onEditModeEnabled();
            return;
        }
        mLauncher.getStateManager().goToState(EDIT_MODE);
        // If a late NORMAL completion already drained, re-assert after the next frame.
        mLauncher.getWorkspace().post(() -> {
            if (hasPendingState() && !mActive) {
                if (mLauncher.getStateManager().isInStableState(EDIT_MODE)) {
                    onEditModeEnabled();
                } else if (mLauncher.getStateManager().getState() != EDIT_MODE) {
                    Log.w(TAG, "pending effect lost EDIT_MODE; retrying goToState");
                    mLauncher.getStateManager().goToState(EDIT_MODE);
                }
            }
        });
    }

    /**
     * OPPO {@code onToggleBarStateEnable}: consume pending and inflate Effect UI into the
     * permanent root. Called from {@link Launcher#onStateSetEnd} when EDIT_MODE settles.
     */
    public void onEditModeEnabled() {
        if (!EFFECT_STATE.equals(mPendingState)) {
            return;
        }
        Log.i(TAG, "onEditModeEnabled → createEffectView");
        mPendingState = null;
        createEffectView();
    }

    private void createEffectView() {
        ViewGroup root = mLauncher.findViewById(R.id.toggle_bar_root);
        if (root == null) {
            Log.e(TAG, "toggle_bar_root missing from launcher layout");
            return;
        }
        destroyContentOnly();
        root.setVisibility(View.VISIBLE);
        root.bringToFront();
        mContentView = LayoutInflater.from(mLauncher)
                .inflate(R.layout.togglebar_effect_panel, root, false);
        root.addView(mContentView);

        mGallery = (GridGallery) mContentView;
        mAdapter = new ScrollEffectAdapter(mLauncher);
        mAdapter.beginPreview();
        mGallery.setGalleryAdapter(mAdapter);
        mAdapter.updateIconTextViewSelection(mAdapter.getSelectedPosition());
        mGallery.setCurrentItem(mAdapter.getSelectedPosition());

        mLauncher.getEditSelectionManager().showEffectsChrome(
                v -> finish(false /* apply */),
                v -> finish(true /* apply */));
        SystemBarHelper.hideStatusBar(mLauncher.getWindow(), false /* animate */);
        mActive = true;
        // Re-apply EDIT_MODE so hotseat stays hidden now that the session is active.
        if (mLauncher.isInState(EDIT_MODE)) {
            mLauncher.getStateManager().reapplyState(false);
        }
        Log.i(TAG, "createEffectView done rootVis=" + root.getVisibility()
                + " childCount=" + root.getChildCount());
    }

    /** Cancel or Apply, then leave to NORMAL. */
    public void finish(boolean apply) {
        if (!mActive && !hasPendingState()) {
            return;
        }
        mApplied = apply;
        if (mAdapter != null) {
            if (apply) {
                mAdapter.applyPreview();
            } else {
                mAdapter.cancelPreview();
            }
        }
        clearSessionUi();
        mLauncher.getEditSelectionManager().exit();
        SystemBarHelper.showStatusBar(mLauncher.getWindow());
        if (mLauncher.getWorkspace() != null && mLauncher.getWorkspace().getHotseat() != null) {
            mLauncher.getWorkspace().getHotseat().setAlpha(1f);
        }
        if (!mLauncher.isInState(NORMAL)) {
            mLauncher.getStateManager().goToState(NORMAL);
        }
    }

    /**
     * Called from {@link Launcher#onStateSetEnd} when entering NORMAL.
     * Must not drop a pending "effect" armed for EDIT_MODE (Settings HOME race).
     */
    public void onEnteredNormal() {
        if (hasPendingState()) {
            Log.w(TAG, "onEnteredNormal with pending effect — re-entering EDIT_MODE");
            mLauncher.getStateManager().goToState(EDIT_MODE);
            return;
        }
        if (!mActive) {
            return;
        }
        if (mAdapter != null && !mApplied) {
            mAdapter.cancelPreview();
        }
        clearSessionUi();
    }

    private void clearSessionUi() {
        destroyContentOnly();
        mPendingState = null;
        mActive = false;
    }

    private void destroyContentOnly() {
        ViewGroup root = mLauncher.findViewById(R.id.toggle_bar_root);
        if (root != null) {
            root.removeAllViews();
            root.setVisibility(View.GONE);
        }
        mContentView = null;
        mGallery = null;
        mAdapter = null;
    }
}
