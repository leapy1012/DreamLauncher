package com.android.launcher.guide.side;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;

import com.android.launcher3.R;
import com.android.quickstep.TouchInteractionService.TISBinder;
import com.android.quickstep.util.TISBindHelper;
import com.android.quickstep.util.MotionPauseDetector;

import java.util.Collections;

/**
 * ColorOS-style interactive side gesture guide.
 *
 * <p>The visual assets and dimensions match OplusLauncher's
 * {@code SideSlipGesturesGuideActivity}; the gesture handling is kept local so the guide does not
 * depend on private Oplus navigation services.</p>
 */
public class SideSlipGesturesGuideActivity extends Activity {

    public static final String EXTRA_GUIDE_START_TYPE = "guide_start_type";
    private GestureGuideView mGuideView;
    private TISBindHelper mTisBindHelper;
    private TISBinder mTisBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        int startType = getIntent().getIntExtra(EXTRA_GUIDE_START_TYPE, 1);
        boolean singleGesture = getIntent().hasExtra(EXTRA_GUIDE_START_TYPE);
        mGuideView = new GestureGuideView(startType, singleGesture);
        setContentView(mGuideView);
        mGuideView.post(() -> {
            applyInsetsController();
            mGuideView.updateSystemGestureExclusion();
        });
        mTisBindHelper = new TISBindHelper(this, this::onTisConnected);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTouchInteractionService(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateTouchInteractionService(false);
    }

    @Override
    protected void onDestroy() {
        updateTouchInteractionService(false);
        if (mGuideView != null) {
            mGuideView.destroy();
        }
        if (mTisBindHelper != null) {
            mTisBindHelper.onDestroy();
        }
        super.onDestroy();
    }

    private void onTisConnected(TISBinder binder) {
        mTisBinder = binder;
        updateTouchInteractionService(isResumed());
    }

    private void updateTouchInteractionService(boolean enabled) {
        if (mTisBinder != null) {
            mTisBinder.setGestureBlockedTaskId(enabled ? getTaskId() : -1);
        }
    }

    @Override
    public void onBackPressed() {
        if (mGuideView != null && mGuideView.onSystemBack()) {
            return;
        }
        super.onBackPressed();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarDividerColor(Color.TRANSPARENT);
        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }
    }

    private void applyInsetsController() {
        if (android.os.Build.VERSION.SDK_INT < 30
                || !getWindow().getDecorView().isAttachedToWindow()) {
            return;
        }
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private final class GestureGuideView extends View {

        private static final int LEFT_BACK = 1;
        private static final int RIGHT_BACK = 2;
        private static final int HOME = 3;
        private static final int RECENTS = 4;
        private static final int SWITCH_APP = 5;

        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint mTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mMessagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix mBackgroundMatrix = new Matrix();
        private final Handler mHandler = new Handler();
        private final Bitmap mWorkspace;
        private final Bitmap mBackSuccess;
        private final Bitmap mActionBackground;
        private final Bitmap mHomeSuccess;
        private final MotionPauseDetector mMotionPauseDetector;
        private final PathInterpolator mGuideMotionInterpolator =
                new PathInterpolator(.3f, 0f, .1f, 1f);
        private final boolean mSingleGesture;
        private float mDensity;

        private int mState;
        private float mDownX;
        private float mDownY;
        private long mDownTime;
        private float mOffsetX;
        private float mOffsetY;
        private float mContentScale = 1f;
        private float mContentHeightScale = 1f;
        private boolean mTracking;
        private boolean mShowingSuccess;
        private boolean mHadBeenPeeked;
        private boolean mUseHomeBackground;
        private VelocityTracker mVelocityTracker;
        private ValueAnimator mRunningAnimator;

        GestureGuideView(int startType, boolean singleGesture) {
            super(SideSlipGesturesGuideActivity.this);
            mDensity = getResources().getDisplayMetrics().density;
            mState = Math.max(LEFT_BACK, Math.min(SWITCH_APP, startType));
            mSingleGesture = singleGesture;
            mWorkspace = BitmapFactory.decodeResource(
                    getResources(), R.drawable.side_slip_gestures_action_img);
            mBackSuccess = BitmapFactory.decodeResource(
                    getResources(), R.drawable.side_gestures_back_success);
            mActionBackground = BitmapFactory.decodeResource(
                    getResources(), R.drawable.side_gestures_action_bg);
            mHomeSuccess = BitmapFactory.decodeResource(
                    getResources(), R.drawable.side_gestures_home_success);
            mMotionPauseDetector = new MotionPauseDetector(
                    SideSlipGesturesGuideActivity.this);

            mTitlePaint.setColor(Color.BLACK);
            mTitlePaint.setTextSize(dp(20));
            mTitlePaint.setTextAlign(Paint.Align.CENTER);
            mTitlePaint.setTypeface(android.graphics.Typeface.create(
                    "sans-serif-medium", android.graphics.Typeface.NORMAL));

            mMessagePaint.setColor(0x8A000000);
            mMessagePaint.setTextSize(dp(14));
            mMessagePaint.setTextAlign(Paint.Align.CENTER);
            mMessagePaint.setTypeface(android.graphics.Typeface.create(
                    "sans-serif", android.graphics.Typeface.NORMAL));
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setFocusable(true);
            setContentDescription(getString(R.string.oplus_learn_gestures));
        }

        void updateSystemGestureExclusion() {
            if (android.os.Build.VERSION.SDK_INT < 29 || !isAttachedToWindow()) {
                return;
            }
            Rect exclusion;
            if (mState == LEFT_BACK) {
                // OPPO leaves the taught edge active, then consumes the resulting onBackPressed().
                exclusion = new Rect(getWidth() / 2, 0, getWidth(), getHeight());
            } else if (mState == RIGHT_BACK) {
                exclusion = new Rect(0, 0, getWidth() / 2, getHeight());
            } else {
                // TIS blocks Home/Overview while the view classifies the bottom touch stream.
                exclusion = new Rect(0, 0, getWidth(), getHeight());
            }
            SideSlipGesturesGuideActivity.this.getWindow().setSystemGestureExclusionRects(
                    Collections.singletonList(exclusion));
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            // Oplus renders the guide in a fixed 360dp-wide context. Use the same virtual density
            // so the 312dp prompt box has identical proportions on devices with custom DPI.
            mDensity = width / 360f;
            mTitlePaint.setTextSize(dp(20));
            mMessagePaint.setTextSize(dp(14));
            updateBackgroundMatrix(mWorkspace, width, height);
            updateSystemGestureExclusion();
        }

        boolean onSystemBack() {
            if (mShowingSuccess || (mState != LEFT_BACK && mState != RIGHT_BACK)) {
                return true;
            }
            if (android.os.Build.VERSION.SDK_INT >= 29 && isAttachedToWindow()) {
                SideSlipGesturesGuideActivity.this.getWindow().setSystemGestureExclusionRects(
                        Collections.singletonList(
                                new Rect(0, 0, getWidth(), getHeight())));
            }
            animateBackCompletion();
            return true;
        }

        private void updateBackgroundMatrix(Bitmap bitmap, int width, int height) {
            if (bitmap == null || width == 0 || height == 0) {
                return;
            }
            float scale = Math.max((float) width / bitmap.getWidth(),
                    (float) height / bitmap.getHeight());
            float dx = (width - bitmap.getWidth() * scale) / 2f;
            float dy = (height - bitmap.getHeight() * scale) / 2f;
            mBackgroundMatrix.setScale(scale, scale);
            mBackgroundMatrix.postTranslate(dx, dy);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawWorkspace(canvas);
            drawGuideIndicator(canvas);
            drawPrompt(canvas);
        }

        private void drawGuideIndicator(Canvas canvas) {
            if (mTracking || mShowingSuccess) {
                return;
            }
            if (mState == LEFT_BACK || mState == RIGHT_BACK) {
                drawBackGuideIndicator(canvas);
                postInvalidateDelayed(16);
                return;
            }
            if (mState == SWITCH_APP) {
                drawSwitchAppGuideIndicator(canvas);
                postInvalidateDelayed(16);
                return;
            }
            float phase = (SystemClock.uptimeMillis() % 1800L) / 1800f;
            float eased = phase < .75f ? phase / .75f : 1f;
            float startX = getWidth() / 2f;
            float startY = getHeight() - dp(54);
            float endX = startX;
            float endY = startY - (mState == RECENTS ? dp(72) : dp(116));
            if (mState == SWITCH_APP) {
                endX += dp(92);
                endY = startY - dp(30);
            }
            float x = startX + (endX - startX) * eased;
            float y = startY + (endY - startY) * eased;

            mPaint.setShader(new LinearGradient(
                    x, y, x, y + dp(64),
                    0x667EA7E5, 0x007EA7E5, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRoundRect(
                    x - dp(20), y, x + dp(20), y + dp(72),
                    dp(20), dp(20), mPaint);
            mPaint.setShader(null);
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, dp(20), mPaint);
            postInvalidateDelayed(16);
        }

        /** Reproduces the horizontal path in OPPO's side_gestures_switch_app animation. */
        private void drawSwitchAppGuideIndicator(Canvas canvas) {
            float frame = (SystemClock.uptimeMillis() % 2267L) * 60f / 1000f;
            float position;
            if (frame < 3f || frame >= 90f) {
                position = 0;
            } else if (frame < 31f) {
                position = mGuideMotionInterpolator.getInterpolation((frame - 3f) / 28f);
            } else if (frame <= 61f) {
                position = 1f;
            } else {
                position = 1f - mGuideMotionInterpolator.getInterpolation((frame - 61f) / 29f);
            }

            // The source animation is 360x180, rendered inside a 124x62dp viewport.
            float viewportLeft = (getWidth() - dp(124)) / 2f;
            float compositionScale = dp(124) / 360f;
            float centerX = viewportLeft
                    + (169.75f + (299.75f - 169.75f) * position) * compositionScale;
            float centerY = getHeight() - dp(31);
            float radius = dp(20.6667f);

            mPaint.setShader(new LinearGradient(
                    centerX, centerY, centerX, getHeight(),
                    0x407EA7E5, 0x007EA7E5,
                    android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRoundRect(
                    centerX - radius, centerY,
                    centerX + radius, getHeight(),
                    radius, radius, mPaint);
            mPaint.setShader(null);
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(centerX, centerY, radius, mPaint);
        }

        /**
         * Draws the same 124x62dp, 136-frame loop used by OPPO's
         * side_gestures_left/right animations.
         */
        private void drawBackGuideIndicator(Canvas canvas) {
            float frame = (SystemClock.uptimeMillis() % 2267L) * 60f / 1000f;
            if (frame >= 105f) {
                return;
            }

            float alpha;
            if (frame < 15f) {
                alpha = frame / 15f;
            } else if (frame <= 90f) {
                alpha = 1f;
            } else {
                alpha = 1f - (frame - 90f) / 15f;
            }

            float motion;
            if (frame <= 15f) {
                motion = 0;
            } else if (frame < 55f) {
                motion = mGuideMotionInterpolator.getInterpolation((frame - 15f) / 40f);
            } else {
                motion = 1f;
            }

            float indicatorScale = frame <= 90f
                    ? 1f : 1f + .28f * Math.min(1f, (frame - 90f) / 20f);
            float radius = dp(20.6667f) * indicatorScale;
            float centerY = getHeight() / 2f;
            float travel = dp(92.6556f);
            boolean fromLeft = mState == LEFT_BACK;
            float centerX = fromLeft
                    ? travel * motion
                    : getWidth() - travel * motion;
            float edgeX = fromLeft ? 0 : getWidth();

            int trailAlpha = Math.round(0x66 * alpha);
            int transparentBlue = 0x007EA7E5;
            int visibleBlue = (trailAlpha << 24) | 0x007EA7E5;
            if (Math.abs(centerX - edgeX) >= 1f) {
                mPaint.setShader(new LinearGradient(
                        edgeX, centerY, centerX, centerY,
                        transparentBlue, visibleBlue,
                        android.graphics.Shader.TileMode.CLAMP));
                float trailLeft = Math.min(edgeX, centerX);
                float trailRight = Math.max(edgeX, centerX);
                canvas.drawRoundRect(
                        trailLeft, centerY - dp(20.6667f),
                        trailRight, centerY + dp(20.6667f),
                        dp(20.6667f), dp(20.6667f), mPaint);
                mPaint.setShader(null);
            }
            mPaint.setColor((Math.round(255 * alpha) << 24) | 0x00FFFFFF);
            canvas.drawCircle(centerX, centerY, radius, mPaint);
        }

        private void drawWorkspace(Canvas canvas) {
            Bitmap image = mShowingSuccess && (mState == LEFT_BACK || mState == RIGHT_BACK)
                    ? mBackSuccess : mWorkspace;
            if (image == null) {
                canvas.drawColor(0xFFE9CEC2);
                return;
            }

            if (mState <= RIGHT_BACK) {
                if (image != mWorkspace) {
                    updateBackgroundMatrix(image, getWidth(), getHeight());
                }
                int save = canvas.save();
                canvas.translate(mOffsetX, mOffsetY);
                canvas.drawBitmap(image, mBackgroundMatrix, mPaint);
                canvas.restoreToCount(save);
                if (image != mWorkspace) {
                    updateBackgroundMatrix(mWorkspace, getWidth(), getHeight());
                }
                return;
            }

            drawFullScreenBitmap(canvas,
                    mUseHomeBackground && mHomeSuccess != null
                            ? mHomeSuccess : mActionBackground);

            float cardWidth = getWidth() * mContentScale;
            float cardHeight = getHeight() * mContentHeightScale;
            float left = (getWidth() - cardWidth) / 2f + mOffsetX;
            float top = (getHeight() - cardHeight) / 2f + mOffsetY;
            RectF current = new RectF(left, top, left + cardWidth, top + cardHeight);
            float spacing = 50f;

            if (mState == RECENTS || mState == SWITCH_APP) {
                RectF previous = new RectF(
                        current.left - cardWidth - spacing, current.top,
                        current.left - spacing, current.bottom);
                drawRoundedBitmap(canvas, mBackSuccess, previous, dp(19));
            }
            if (mState == SWITCH_APP) {
                RectF next = new RectF(
                        current.right + spacing, current.top,
                        current.right + cardWidth + spacing, current.bottom);
                drawRoundedBitmap(canvas, mBackSuccess, next, dp(19));
            }
            drawRoundedBitmap(canvas, mWorkspace, current,
                    Math.min(dp(19), Math.max(0, (1f - mContentScale) * dp(46))));
        }

        private void drawFullScreenBitmap(Canvas canvas, Bitmap bitmap) {
            if (bitmap == null) {
                canvas.drawColor(Color.BLACK);
                return;
            }
            updateBackgroundMatrix(bitmap, getWidth(), getHeight());
            canvas.drawBitmap(bitmap, mBackgroundMatrix, mPaint);
        }

        private void drawRoundedBitmap(Canvas canvas, Bitmap bitmap, RectF destination,
                float radius) {
            if (bitmap == null || destination.width() <= 0 || destination.height() <= 0) {
                return;
            }
            float scale = Math.max(
                    destination.width() / bitmap.getWidth(),
                    destination.height() / bitmap.getHeight());
            float sourceWidth = destination.width() / scale;
            float sourceHeight = destination.height() / scale;
            float sourceLeft = (bitmap.getWidth() - sourceWidth) / 2f;
            float sourceTop = (bitmap.getHeight() - sourceHeight) / 2f;
            Rect source = new Rect(
                    Math.round(sourceLeft), Math.round(sourceTop),
                    Math.round(sourceLeft + sourceWidth),
                    Math.round(sourceTop + sourceHeight));

            int save = canvas.save();
            Path clip = new Path();
            clip.addRoundRect(destination, radius, radius, Path.Direction.CW);
            canvas.clipPath(clip);
            canvas.drawBitmap(bitmap, source, destination, mPaint);
            canvas.restoreToCount(save);
        }

        private void drawPrompt(Canvas canvas) {
            float cardWidth = Math.min(dp(312), getWidth() - dp(32));
            float left = (getWidth() - cardWidth) / 2f;
            float top = dp(48);
            float height = dp(147);
            RectF card = new RectF(left, top, left + cardWidth, top + height);

            mPaint.setColor(Color.WHITE);
            mPaint.setShadowLayer(dp(6), 0, dp(3), 0x26000000);
            canvas.drawRoundRect(card, dp(12), dp(12), mPaint);
            mPaint.clearShadowLayer();

            float closeCenterX = left + dp(35);
            float closeCenterY = top + dp(36);
            mPaint.setColor(0xD9000000);
            mPaint.setStrokeWidth(dp(1.6f));
            mPaint.setStrokeCap(Paint.Cap.SQUARE);
            float radius = dp(6);
            canvas.drawLine(closeCenterX - radius, closeCenterY - radius,
                    closeCenterX + radius, closeCenterY + radius, mPaint);
            canvas.drawLine(closeCenterX + radius, closeCenterY - radius,
                    closeCenterX - radius, closeCenterY + radius, mPaint);

            if (mShowingSuccess) {
                drawSuccess(canvas, card.centerX(), top);
                return;
            }
            drawCenteredText(canvas, titleForState(), card.centerX(), top + dp(86), mTitlePaint,
                    cardWidth - dp(64), dp(23));
            canvas.drawText(messageForState(), card.centerX(), top + dp(115), mMessagePaint);
        }

        private void drawSuccess(Canvas canvas, float centerX, float top) {
            float cy = top + dp(91);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(dp(2));
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setColor(0xFF2EC58D);
            canvas.drawCircle(centerX - dp(51), cy - dp(6), dp(10), mPaint);
            Path tick = new Path();
            tick.moveTo(centerX - dp(56), cy - dp(6));
            tick.lineTo(centerX - dp(52), cy - dp(2));
            tick.lineTo(centerX - dp(46), cy - dp(10));
            canvas.drawPath(tick, mPaint);
            mPaint.setStyle(Paint.Style.FILL);
            mTitlePaint.setTextSize(dp(18));
            canvas.drawText(getString(R.string.side_gestures_success),
                    centerX + dp(14), cy, mTitlePaint);
            mTitlePaint.setTextSize(dp(20));
        }

        private void drawCenteredText(Canvas canvas, String text, float centerX, float baseline,
                Paint paint, float maxWidth, float lineHeight) {
            if (paint.measureText(text) <= maxWidth) {
                canvas.drawText(text, centerX, baseline, paint);
                return;
            }
            int split = -1;
            for (int i = text.indexOf(' '); i >= 0; i = text.indexOf(' ', i + 1)) {
                if (paint.measureText(text.substring(0, i)) <= maxWidth) {
                    split = i;
                } else {
                    break;
                }
            }
            if (split <= 0) {
                canvas.drawText(text, centerX, baseline, paint);
                return;
            }
            canvas.drawText(text.substring(0, split), centerX, baseline - lineHeight / 2f, paint);
            canvas.drawText(text.substring(split + 1), centerX,
                    baseline + lineHeight / 2f, paint);
        }

        private String titleForState() {
            switch (mState) {
                case RIGHT_BACK:
                    return getString(R.string.side_gestures_right_title);
                case HOME:
                    return getString(R.string.side_gestures_home_title);
                case RECENTS:
                    return getString(R.string.side_gestures_recents_title);
                case SWITCH_APP:
                    return getString(R.string.side_gestures_switch_title);
                case LEFT_BACK:
                default:
                    return getString(R.string.side_gestures_left_title);
            }
        }

        private String messageForState() {
            String prefix;
            if (mSingleGesture) {
                prefix = "";
            } else if (mState <= RIGHT_BACK) {
                prefix = "1/4 ";
            } else {
                prefix = (mState - 1) + "/4 ";
            }
            switch (mState) {
                case HOME:
                    return prefix + getString(R.string.side_gestures_home_msg);
                case RECENTS:
                    return prefix + getString(R.string.side_gestures_recents_msg);
                case SWITCH_APP:
                    return prefix + getString(R.string.side_gestures_switch_app_msg);
                default:
                    return prefix + getString(R.string.side_gestures_back_msg);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (mShowingSuccess) {
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (isCloseHit(event.getX(), event.getY())) {
                        finish();
                        return true;
                    }
                    if (mRunningAnimator != null) {
                        mRunningAnimator.cancel();
                    }
                    mDownX = event.getX();
                    mDownY = event.getY();
                    mDownTime = event.getEventTime();
                    mTracking = isValidStart(mDownX, mDownY);
                    mHadBeenPeeked = false;
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                    }
                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(event);
                    mMotionPauseDetector.clear();
                    mMotionPauseDetector.setOnMotionPauseListener(
                            () -> mHadBeenPeeked = true);
                    mMotionPauseDetector.setDisallowPause(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (mTracking) {
                        mVelocityTracker.addMovement(event);
                        float dx = event.getX() - mDownX;
                        float dy = event.getY() - mDownY;
                        boolean allowPause = mState == RECENTS
                                && -dy >= dp(36) && Math.abs(dy) >= Math.abs(dx);
                        mMotionPauseDetector.setDisallowPause(!allowPause);
                        mMotionPauseDetector.addPosition(event);
                        updateGesturePreview(event.getX(), event.getY());
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (mVelocityTracker != null) {
                        mVelocityTracker.addMovement(event);
                    }
                    if (mTracking && isSuccessful(event)) {
                        if (mState >= HOME) {
                            animateBottomCompletion();
                        } else {
                            completeGesture();
                        }
                    } else {
                        resetPreview(true);
                    }
                    mTracking = false;
                    clearMotionTracking();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    mTracking = false;
                    resetPreview(true);
                    clearMotionTracking();
                    return true;
                default:
                    return true;
            }
        }

        private boolean isCloseHit(float x, float y) {
            float cardWidth = Math.min(dp(312), getWidth() - dp(32));
            float left = (getWidth() - cardWidth) / 2f;
            return x >= left && x <= left + dp(70) && y >= dp(48) && y <= dp(105);
        }

        private boolean isValidStart(float x, float y) {
            float edge = dp(32);
            float bottom = getHeight() - getBottomGestureHeight();
            switch (mState) {
                case LEFT_BACK:
                    return x <= edge;
                case RIGHT_BACK:
                    return x >= getWidth() - edge;
                default:
                    return y >= bottom;
            }
        }

        private void updateGesturePreview(float x, float y) {
            float dx = x - mDownX;
            float dy = y - mDownY;
            if (mState == LEFT_BACK) {
                mOffsetX = Math.max(0, Math.min(dx, getWidth() * .32f));
            } else if (mState == RIGHT_BACK) {
                mOffsetX = Math.min(0, Math.max(dx, -getWidth() * .32f));
            } else {
                mContentScale = Math.max(.5f, Math.min(1f, y / getHeight()));
                mContentHeightScale = mContentScale;
                mOffsetX = dx * mContentScale;
                mOffsetY = 0;
            }
            invalidate();
        }

        private boolean isSuccessful(MotionEvent event) {
            float dx = event.getX() - mDownX;
            float dy = event.getY() - mDownY;
            float xVelocity = 0;
            float yVelocity = 0;
            if (mVelocityTracker != null) {
                mVelocityTracker.computeCurrentVelocity(
                        1000, ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity());
                xVelocity = mVelocityTracker.getXVelocity();
                yVelocity = mVelocityTracker.getYVelocity();
            }
            boolean isUpwardFling = yVelocity < -dp(150);
            boolean endsInBottomRegion =
                    event.getY() >= getHeight() - 2f * getBottomGestureHeight();
            switch (mState) {
                case LEFT_BACK:
                    return dx > dp(72) && Math.abs(dx) > Math.abs(dy);
                case RIGHT_BACK:
                    return dx < -dp(72) && Math.abs(dx) > Math.abs(dy);
                case HOME:
                    return dy < -dp(36) && !mHadBeenPeeked && !endsInBottomRegion
                            && (isUpwardFling || dy < -dp(105));
                case RECENTS:
                    return dy < -dp(36) && mHadBeenPeeked && !endsInBottomRegion;
                case SWITCH_APP:
                    return dy < -dp(20) && Math.abs(dx) > dp(72)
                            && (endsInBottomRegion
                                    || Math.abs(xVelocity) > Math.abs(yVelocity));
                default:
                    return false;
            }
        }

        private float getBottomGestureHeight() {
            int id = getResources().getIdentifier(
                    "navigation_bar_gesture_height", "dimen", "android");
            float systemHeight = id == 0 ? 0 : getResources().getDimensionPixelSize(id);
            return Math.max(dp(36), systemHeight);
        }

        private void clearMotionTracking() {
            mMotionPauseDetector.clear();
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
        }

        private void animateBottomCompletion() {
            final float startScale = mContentScale;
            final float startHeightScale = mContentHeightScale;
            final float startX = mOffsetX;
            final float startY = mOffsetY;
            final float targetScale;
            final float targetHeightScale;
            final float targetX;
            final float targetY;

            if (mState == HOME) {
                mUseHomeBackground = true;
                targetScale = dp(50) / getWidth();
                targetHeightScale = dp(50) / getHeight();
                targetX = dp(300) - getWidth() / 2f;
                targetY = dp(568) - getHeight() / 2f;
            } else if (mState == RECENTS) {
                targetScale = 0.5833333f;
                targetHeightScale = targetScale;
                targetX = 0;
                targetY = 0;
            } else {
                float direction = startX == 0
                        ? Math.signum(mDownX - getWidth() / 2f) : Math.signum(startX);
                if (direction == 0) {
                    direction = 1;
                }
                targetScale = 1f;
                targetHeightScale = 1f;
                targetX = direction * (getWidth() + 50f);
                targetY = 0;
            }

            mRunningAnimator = ValueAnimator.ofFloat(0f, 1f);
            mRunningAnimator.setDuration(330);
            mRunningAnimator.setInterpolator(new PathInterpolator(.3f, 0f, .1f, 1f));
            mRunningAnimator.addUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();
                mContentScale = startScale + (targetScale - startScale) * progress;
                mContentHeightScale = startHeightScale
                        + (targetHeightScale - startHeightScale) * progress;
                mOffsetX = startX + (targetX - startX) * progress;
                mOffsetY = startY + (targetY - startY) * progress;
                invalidate();
            });
            mRunningAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                private boolean mCancelled;

                @Override
                public void onAnimationCancel(android.animation.Animator animation) {
                    mCancelled = true;
                }

                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    mRunningAnimator = null;
                    if (!mCancelled) {
                        completeGesture();
                    }
                }
            });
            mRunningAnimator.start();
        }

        private void completeGesture() {
            mShowingSuccess = true;
            invalidate();
            mHandler.postDelayed(() -> {
                if (mSingleGesture) {
                    if (mState == LEFT_BACK) {
                        mShowingSuccess = false;
                        mState = RIGHT_BACK;
                        resetVisualState();
                        updateSystemGestureExclusion();
                        invalidate();
                    } else {
                        finish();
                    }
                    return;
                }
                if (mState == SWITCH_APP) {
                    finish();
                    return;
                }
                mShowingSuccess = false;
                mState++;
                resetVisualState();
                updateSystemGestureExclusion();
                invalidate();
            }, 1200);
        }

        private void animateBackCompletion() {
            final float startX = mOffsetX;
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(400);
            animator.setInterpolator(new PathInterpolator(.3f, 0f, .1f, 1f));
            animator.addUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();
                mOffsetX = startX + (getWidth() - startX) * progress;
                invalidate();
            });
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    mOffsetX = 0;
                    completeGesture();
                }
            });
            animator.start();
        }

        private void resetPreview(boolean animate) {
            if (!animate) {
                resetVisualState();
                return;
            }
            final float startX = mOffsetX;
            final float startY = mOffsetY;
            final float startScale = mContentScale;
            final float startHeightScale = mContentHeightScale;
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(260);
            animator.setInterpolator(new PathInterpolator(.3f, 0f, .1f, 1f));
            animator.addUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();
                mOffsetX = startX * (1f - progress);
                mOffsetY = startY * (1f - progress);
                mContentScale = startScale + (1f - startScale) * progress;
                mContentHeightScale = startHeightScale + (1f - startHeightScale) * progress;
                invalidate();
            });
            animator.start();
        }

        private void resetVisualState() {
            mOffsetX = 0;
            mOffsetY = 0;
            mContentScale = 1f;
            mContentHeightScale = 1f;
            mUseHomeBackground = false;
        }

        void destroy() {
            mHandler.removeCallbacksAndMessages(null);
            clearMotionTracking();
            if (mRunningAnimator != null) {
                mRunningAnimator.cancel();
                mRunningAnimator = null;
            }
        }

        private float dp(float value) {
            return value * mDensity;
        }
    }
}
