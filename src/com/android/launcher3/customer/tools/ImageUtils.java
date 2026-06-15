package com.android.launcher3.customer.tools;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.Display;
import android.view.View;

import androidx.core.internal.view.SupportMenu;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.view.WindowManagerGlobal;
import android.hardware.display.DisplayManagerGlobal;
import com.android.launcher3.screenshot.ImageCaptureImpl;
import android.app.ActivityTaskManager;
import android.provider.Settings;

public class ImageUtils {
    private static final float DEFAULT_FULLSCREEN_SCALE = 1.0f;
    private static final String TAG = "ImageUtils";
    static final int TRANSPARENT = 30;
    private static final float mScaleValue = 0.75f;

    public static Bitmap createMaskImage(Bitmap source, Bitmap mask, Bitmap bt, Bitmap top) {
        if (mask == null) {
            return source;
        }
        Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(result);
        Paint paint = new Paint(1);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mCanvas.drawBitmap(source, (float) ((mask.getWidth() / 2) - (source.getWidth() / 2)), (float) ((mask.getHeight() / 2) - (source.getHeight() / 2)), (Paint) null);
        mCanvas.drawBitmap(mask, 0.0f, 0.0f, paint);
        paint.setXfermode((Xfermode) null);
        if (bt == null) {
            return result;
        }
        Bitmap r = doodle(result, bt);
        if (top != null) {
            return doodle(top, r);
        }
        return r;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap.Config config;
        BitmapDrawable b;
        if (drawable == null) {
            return null;
        }
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (drawable.getOpacity() != -1) {
            config = Bitmap.Config.ARGB_8888;
        } else {
            config = Bitmap.Config.ARGB_8888;
        }
        Bitmap bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, config);
        Canvas canvas = new Canvas(bitmap);
        canvas.save();
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        canvas.restore();
        if (!(drawable == null || !(drawable instanceof BitmapDrawable) || (b = (BitmapDrawable) drawable) == null || b.getBitmap() == bitmap)) {
            b.setCallback((Drawable.Callback) null);
            System.gc();
        }
        return bitmap;
    }


    public static int[] getBitmapCantPixel(Bitmap bit, int resize) {
        int width = bit.getWidth();
        int height = bit.getHeight();
        int offsetX = resize;
        int offsetY = resize;
        bit.copyPixelsToBuffer(ByteBuffer.allocate(bit.getByteCount()));
        int[] pixels = {bit.getPixel(offsetX, offsetY), bit.getPixel(width - offsetX, offsetY), bit.getPixel(offsetX, height - offsetY), bit.getPixel(width - offsetX, height - offsetY)};
        pixels[0] = Color.alpha(pixels[0]);
        pixels[1] = Color.alpha(pixels[1]);
        pixels[2] = Color.alpha(pixels[2]);
        pixels[3] = Color.alpha(pixels[3]);
        return pixels;
    }

    public static boolean neddResizeIcon(Bitmap bit, int resize) {
        int[] pixels = getBitmapCantPixel(bit, resize);
        if (pixels[0] > 30 || pixels[1] > 30 || pixels[2] > 30 || pixels[3] > 30) {
            return false;
        }
        return true;
    }


    public static Bitmap toRGB(Bitmap src) {
        if (src == null || src.getConfig() != Bitmap.Config.HARDWARE) {
            return src;
        }
        Log.d("songhui", "getBlurWallpaper config is HARDWARE, copy to ARGB_8888");
        long time1 = System.currentTimeMillis();
        Bitmap test = src.copy(Bitmap.Config.ARGB_8888, true);
        Log.d("songhui", "bitmap.copy time ------>" + (System.currentTimeMillis() - time1));
        return test;
    }

    public static Drawable zoomDrawable(Drawable drawable, int w, int h, Resources re) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap oldbmp = drawableToBitmap(drawable);
        Matrix matrix = new Matrix();
        matrix.postScale(((float) w) / ((float) width), ((float) h) / ((float) height));
        return new BitmapDrawable(re, Bitmap.createBitmap(oldbmp, 0, 0, width, height, matrix, true));
    }

    public static Bitmap resize(Bitmap bm, int w, int h) {
        if (bm == null) {
            return null;
        }
        int width = bm.getWidth();
        int height = bm.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(((float) w) / ((float) width), ((float) h) / ((float) height));
        try {
            Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
            Bitmap bg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            if (resizedBitmap != null) {
                return doodle(resizedBitmap, bg);
            }
            bm.recycle();
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Bitmap resizeHxy(Bitmap bm, int w, int h) {
        if (bm == null) {
            return null;
        }
        int width = bm.getWidth();
        int height = bm.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(((float) w) / ((float) width), ((float) h) / ((float) height));
        try {
            Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
            if (resizedBitmap != null) {
                return resizedBitmap;
            }
            bm.recycle();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Drawable cropDrawble(BubbleTextView view) {
        view.buildDrawingCache();
        Bitmap src = view.getDrawingCache();
        if (src == null) {
            return null;
        }
        Rect bound = new Rect();
        view.getIconBounds(bound);
        int iconsize = Launcher.getLauncher(view.getContext()).getDeviceProfile().iconSizePx;
        Bitmap newBM = Bitmap.createBitmap(src, bound.left, bound.top, iconsize, iconsize, (Matrix) null, false);
        view.destroyDrawingCache();
        return bitmapToDrawable(newBM);
    }

    public static Bitmap cropBitmap(BubbleTextView view) {
        view.buildDrawingCache();
        Bitmap src = view.getDrawingCache();
        if (src == null || src.getWidth() <= 0 || src.getHeight() <= 0) {
            view.destroyDrawingCache();
            return null;
        }
        Rect bound = new Rect();
        view.getIconBounds(bound);
        int iconsize = Launcher.getLauncher(view.getContext()).getDeviceProfile().iconSizePx;
        return Bitmap.createBitmap(src, bound.left, bound.top, iconsize, iconsize, (Matrix) null, false);
    }

    public static Bitmap scaleBitmap(Bitmap origin, int newWidth, int newHeight) {
        if (origin == null) {
            return null;
        }
        int height = origin.getHeight();
        int width = origin.getWidth();
        Matrix matrix = new Matrix();
        matrix.postScale(((float) newWidth) / ((float) width), ((float) newHeight) / ((float) height));
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (!origin.isRecycled()) {
            origin.recycle();
        }
        return newBM;
    }

    public static Bitmap resizeScreen(Bitmap bm, int w, int h) {
        Bitmap bitmap = bm;
        Bitmap BitmapOrg = bm;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(((float) w) / ((float) width), ((float) h) / ((float) height));
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);
        Bitmap bg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
        if (resizedBitmap != null) {
            return doodle(resizedBitmap, bg);
        }
        if (!(bitmap == null || resizedBitmap == bitmap)) {
            bm.recycle();
        }
        return resizedBitmap;
    }

    public static Bitmap doodle(Bitmap src, Bitmap bg) {
        Bitmap newb = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newb);
        canvas.drawBitmap(bg, 0.0f, 0.0f, (Paint) null);
        canvas.drawBitmap(src, (float) ((bg.getWidth() - src.getWidth()) / 2), (float) ((bg.getHeight() - src.getHeight()) / 2), (Paint) null);
        canvas.save();
        canvas.restore();
        return newb;
    }

    public static Drawable bitmapToDrawable(Bitmap bmp) {
        return new BitmapDrawable(bmp);
    }

    public static Drawable bitmapToDrawable1(Bitmap bmp, Resources r) {
        return new BitmapDrawable(r, bmp);
    }

    public static Bitmap drawableToBitmap1(Drawable drawable) {
        Bitmap.Config config;
        BitmapDrawable b;
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (drawable.getOpacity() != -1) {
            config = Bitmap.Config.ARGB_8888;
        } else {
            config = Bitmap.Config.RGB_565;
        }
        Bitmap bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, config);
        Canvas canvas = new Canvas(bitmap);
        canvas.save();
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        canvas.restore();
        if (!(drawable == null || !(drawable instanceof BitmapDrawable) || (b = (BitmapDrawable) drawable) == null || b.getBitmap() == bitmap)) {
            b.setCallback((Drawable.Callback) null);
            System.gc();
        }
        return bitmap;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(SupportMenu.CATEGORY_MASK);
        canvas.drawRoundRect(rectF, 30.0f, 30.0f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public static Bitmap doodletest(Bitmap src, Bitmap bg) {
        Bitmap newb = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newb);
        canvas.drawBitmap(bg, 0.0f, 0.0f, (Paint) null);
        canvas.drawBitmap(src, (float) ((bg.getWidth() - src.getWidth()) / 2), (float) ((bg.getHeight() - src.getHeight()) / 2), (Paint) null);
        canvas.save();
        canvas.restore();
        return newb;
    }

    public static Bitmap viewShot(View v) {
        if (v == null) {
            return null;
        }
        v.setScaleY(0.3f);
        v.setScaleX(0.3f);
        v.invalidate();
        if (v.getMeasuredWidth() <= 0 || v.getMeasuredHeight() <= 0) {
            v.setScaleY(1.0f);
            v.setScaleX(1.0f);
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_4444);
        Canvas c = new Canvas(bitmap);
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        try {
            v.draw(c);
        } catch (Exception e) {
            e.printStackTrace();
        }
        v.setScaleY(1.0f);
        v.setScaleX(1.0f);
        v.invalidate();
        return bitmap;
    }

    public static void savePNG(Bitmap bitmap, String path) {
        File file = new File(path);
        if (bitmap != null) {
            try {
                FileOutputStream out = new FileOutputStream(file);
                if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    out.flush();
                    out.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public static Bitmap getScaleIcon(Bitmap src) {
        if (src != null && 0 == 0) {
            return src;
        }
        return null;
    }

    public static Bitmap resizeAndOffset(Bitmap bm, int w, int h, int Xoff, int Yoff, int iconSize) {
        Bitmap bitmap = bm;
        int width = bm.getWidth();
        int height = bm.getHeight();
        Matrix matrix = getMatrix();
        matrix.postScale(((float) w) / ((float) width), ((float) h) / ((float) height));
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        int i = iconSize;
        Bitmap bg = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
        if (resizedBitmap != null) {
            return doodleOff(resizedBitmap, bg, Xoff, Yoff);
        }
        int i2 = Xoff;
        int i3 = Yoff;
        if (!(bitmap == null || resizedBitmap == bitmap)) {
            bm.recycle();
        }
        return resizedBitmap;
    }

    public static Bitmap doodleOff(Bitmap src, Bitmap bg, int x, int y) {
        Bitmap newb = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newb);
        canvas.drawBitmap(bg, 0.0f, 0.0f, (Paint) null);
        canvas.drawBitmap(src, (float) (((bg.getWidth() - src.getWidth()) / 2) + x), (float) (((bg.getHeight() - src.getHeight()) / 2) + y), (Paint) null);
        canvas.save();
        canvas.restore();
        if (!(src == null || src == newb)) {
            src.recycle();
        }
        if (bg != null) {
            bg.recycle();
        }
        return newb;
    }

    private static Matrix getMatrix() {
        Matrix matrix = new Matrix();
        Camera camera = new Camera();
        camera.save();
        camera.getMatrix(matrix);
        camera.restore();
        return matrix;
    }

   public static Drawable takeScreenShotOfView(Context context, float scale) {
       Long start = Long.valueOf(System.currentTimeMillis());
       Bitmap blu = screenShot(context);
       Long valueOf = Long.valueOf(System.currentTimeMillis() - start.longValue());
       Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
       Point displaySize = new Point();
       display.getRealSize(displaySize);
       Bitmap blu2 = resizeHxy(blu, (int) (((float) displaySize.x) * scale), (int) (((float) displaySize.y) * scale));
       Long time = Long.valueOf(System.currentTimeMillis() - start.longValue());
       Drawable result = bitmapToDrawable(blurBitmap(context, blu2));
       Long time2 = Long.valueOf(System.currentTimeMillis() - start.longValue());
       return result;
   }

   public static Drawable takeScreenShotOfView(Context context) {
        Long start = Long.valueOf(System.currentTimeMillis());
        Bitmap blu = screenShot(context);
        Long valueOf = Long.valueOf(System.currentTimeMillis() - start.longValue());
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        Point displaySize = new Point();
        display.getRealSize(displaySize);
        Bitmap blu2 = resizeHxy(blu, (int) (((float) displaySize.x) * 0.9f), (int) (((float) displaySize.y) * 0.9f));
        Long time = Long.valueOf(System.currentTimeMillis() - start.longValue());
        Drawable result = bitmapToDrawable(blurBitmap(context, blu2));
        Long time2 = Long.valueOf(System.currentTimeMillis() - start.longValue());
        return result;
    }

    public static Bitmap screenShot(Context context) {
        long currentTimeMillis = System.currentTimeMillis();
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        Point displaySize = new Point();
        display.getRealSize(displaySize);
        int displayWidth = displaySize.x;
        int displayHeight = displaySize.y;
        int rotation = display.getRotation();
        Rect crop = new Rect(0, 0, displayWidth, displayHeight);
        if (Settings.System.getInt(context.getContentResolver(), "hxy_singlehand_state", 1) == 1) {
            int leftOrRight = Settings.System.getInt(context.getContentResolver(), "hxy_singlehand_leftorright", 0);
            if (leftOrRight == 1) {
                crop = new Rect((int) (((float) displayWidth) * 0.25f), (int) (((float) displayHeight) * 0.25f), displayWidth, displayHeight);
            } else if (leftOrRight == 2) {
                crop = new Rect(0, (int) (((float) displayHeight) * 0.25f), (int) (((float) displayWidth) * 0.75f), displayHeight);
            }
        }
        Bitmap screenShot = captureScreenshot(crop);
        if (screenShot == null) {
            return null;
        }
        screenShot.setHasAlpha(false);
        screenShot.prepareToDraw();
        long currentTimeMillis2 = System.currentTimeMillis();
        if (screenShot == null || screenShot.getConfig() != Bitmap.Config.HARDWARE) {
            return screenShot;
        }
        return screenShot.copy(Bitmap.Config.ARGB_4444, true);
    }
    
    public static Bitmap captureScreenshot(Rect crop) {
        return new ImageCaptureImpl(WindowManagerGlobal.getWindowManagerService(), ActivityTaskManager.getService()).captureDisplay(0, crop);
    }

    public static Bitmap takeScreenShotOfView(View v) {
        v.setDrawingCacheEnabled(true);
        Bitmap b = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false);
        return b;
    }

    public static Bitmap blurBitmap(Context context, Bitmap bitmap, int radius) {
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_4444);
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
        blurScript.setRadius((float) radius);
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        allOut.copyTo(outBitmap);
        rs.destroy();
        return outBitmap;
    }

    public static Bitmap blurBitmap(Context context, Bitmap bitmap) {
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_4444);
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
        blurScript.setRadius((float) 20);
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        allOut.copyTo(outBitmap);
        rs.destroy();
        return outBitmap;
    }

    public static Bitmap copy(Bitmap bitmap) {
        if (bitmap == null || bitmap.getConfig() != Bitmap.Config.HARDWARE) {
            return bitmap;
        }
        Log.d("songhui", "getBlurWallpaper config is HARDWARE, copy to ARGB_8888");
        long time1 = System.currentTimeMillis();
        Bitmap bitmap2 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Log.d("songhui", "bitmap.copy time ------>" + (System.currentTimeMillis() - time1));
        return bitmap2;
    }

    public static Bitmap doodleWeather(Bitmap decade, Bitmap unit, Bitmap icon, Bitmap degress, Bitmap re, int count, int iconsize) {
        Paint paint;
        Rect reRect;
        Rect degressRect;
        Canvas canvas;
        Rect decadeRect;
        Rect unitRect;
        Paint paint2;
        Rect rect;
        Bitmap decade2 = decade;
        Bitmap unit2 = unit;
        Bitmap icon2 = icon;
        Bitmap degress2 = degress;
        Bitmap re2 = re;
        int i = count;
        int i2 = iconsize;
        if (icon2 == null) {
            return null;
        }
        if (icon.getHeight() != i2) {
            icon2 = resize(icon2, (int) ((((float) icon.getWidth()) / ((float) icon.getHeight())) * ((float) i2)), i2);
        }
        Bitmap newb = Bitmap.createBitmap(icon2.getWidth(), icon2.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(newb);
        if (icon2 != null) {
            canvas2.drawBitmap(icon2, 0.0f, 0.0f, (Paint) null);
        }
        if (decade2 == null || unit2 == null || degress2 == null) {
            Canvas canvas3 = canvas2;
            return newb;
        }
        int width = iconsize;
        if (decade.getHeight() != i2) {
            decade2 = resize(decade2, (int) ((((float) decade.getWidth()) / ((float) decade.getHeight())) * ((float) i2)), i2);
        }
        if (unit.getHeight() != i2) {
            unit2 = resize(unit2, (int) ((((float) unit.getWidth()) / ((float) unit.getHeight())) * ((float) i2)), i2);
        }
        if (degress.getHeight() != i2) {
            degress2 = resize(degress2, (int) ((((float) degress.getWidth()) / ((float) degress.getHeight())) * ((float) i2)), i2);
        }
        int childWidth = unit2.getWidth();
        if (i > 1) {
            childWidth = decade2.getWidth();
        }
        int left1 = (childWidth * i) + ((i - 1) * 0);
        int reLeft = ((int) ((((float) width) / 2.0f) - (((float) left1) / 2.0f))) + ((int) (((float) (childWidth + 0)) * -0.8f));
        Bitmap newb2 = newb;
        int decadeLeft = ((int) ((((float) width) / 2.0f) - (((float) left1) / 2.0f))) + ((childWidth + 0) * 0);
        Bitmap bitmap = icon2;
        int width2 = (int) (((float) icon2.getWidth()) / 2.6f);
        Canvas canvas4 = canvas2;
        int unitLeft = ((int) ((((float) width) / 2.0f) - (((float) left1) / 2.0f))) + 0 + ((childWidth + 0) * 1);
        int i3 = width;
        int degressLeft = ((int) ((((float) width) / 2.0f) - (((float) left1) / 2.0f))) + 0 + ((childWidth + 0) * 2);
        int i4 = left1;
        Paint paint3 = new Paint();
        paint3.setAntiAlias(true);
        paint3.setDither(true);
        paint3.setFilterBitmap(true);
        if (re2 != null) {
            if (re.getHeight() != i2) {
                paint = paint3;
                re2 = resize(re2, (int) ((((float) re.getWidth()) / ((float) re.getHeight())) * ((float) i2)), i2);
            } else {
                paint = paint3;
            }
            reRect = new Rect(reLeft, 0, reLeft + childWidth, re2.getHeight() + 0);
            re2 = re2;
        } else {
            paint = paint3;
            reRect = null;
        }
        int i5 = reLeft;
        Rect decadeRect2 = new Rect(decadeLeft, 0, decadeLeft + childWidth, decade2.getHeight() + 0);
        Rect unitRect2 = new Rect(decadeLeft, 0, decadeLeft + childWidth, unit2.getHeight() + 0);
        Rect degressRect2 = new Rect(unitLeft, 0, degress2.getWidth() + unitLeft, degress2.getHeight() + 0);
        if (i > 1) {
            Rect rect2 = degressRect2;
            Rect decadeRect3 = new Rect(decadeLeft, 0, decadeLeft + childWidth, decade2.getHeight() + 0);
            Rect unitRect3 = new Rect(unitLeft, 0, unitLeft + childWidth, unit2.getHeight() + 0);
            canvas = canvas4;
            canvas.drawBitmap(decade2, (Rect) null, decadeRect3, (Paint) null);
            Rect decadeRect4 = decadeRect3;
            degressRect = new Rect(degressLeft, 0, degressLeft + degress2.getWidth(), degress2.getHeight() + 0);
            Rect rect3 = degressRect;
            unitRect = unitRect3;
            decadeRect = decadeRect4;
        } else {
            Rect degressRect3 = degressRect2;
            canvas = canvas4;
            decadeRect = decadeRect2;
            unitRect = unitRect2;
            degressRect = degressRect3;
        }
        if (re2 == null) {
            int i6 = unitLeft;
            paint2 = paint;
        } else if (re2 != decade2) {
            int i7 = decadeLeft;
            int i8 = unitLeft;
            paint2 = paint;
            canvas.drawBitmap(re2, (Rect) null, reRect, paint2);
        } else {
            int i9 = unitLeft;
            paint2 = paint;
        }
        if (i > 1) {
            rect = null;
            canvas.drawBitmap(decade2, (Rect) null, decadeRect, paint2);
        } else {
            rect = null;
        }
        canvas.drawBitmap(unit2, rect, unitRect, paint2);
        canvas.drawBitmap(degress2, rect, degressRect, paint2);
        canvas.save();
        canvas.restore();
        if (decade2 == null) {
            return newb2;
        }
        decade2.recycle();
        unit2.recycle();
        degress2.recycle();
        System.gc();
        return newb2;
    }

    public static Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(((float) newWidth) / ((float) width), ((float) newHeight) / ((float) height));
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
    }
}
