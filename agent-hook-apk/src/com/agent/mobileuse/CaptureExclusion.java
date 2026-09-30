package com.agent.mobileuse;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Best-effort screenshot exclusion for the local DSH overlay surfaces.
 *
 * Android's hidden PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY maps to
 * SurfaceControl.SKIP_SCREENSHOT in WindowManagerService. That is the behavior
 * we want here: omit only this overlay layer while leaving the app underneath
 * visible in screencap / screen recording.
 *
 * The SurfaceControl transaction is a second, post-attach fallback for OEM
 * builds that recreate or rewrite the window surface after LayoutParams are
 * submitted.
 */
final class CaptureExclusion {
    private static final String TAG = "CaptureExclusion";
    private static final int PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY = 0x00100000;

    private CaptureExclusion() {}

    static void markWindow(Window window) {
        if (window == null) return;
        try {
            WindowManager.LayoutParams lp = window.getAttributes();
            markLayoutParams(lp);
            window.setAttributes(lp);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to mark window LayoutParams: " + t.getMessage());
        }

        try {
            markView(window.getDecorView());
        } catch (Throwable ignored) {}
    }

    static void markLayoutParams(WindowManager.LayoutParams lp) {
        if (lp == null) return;
        try {
            Field f;
            try {
                f = WindowManager.LayoutParams.class.getField("privateFlags");
            } catch (NoSuchFieldException e) {
                f = WindowManager.LayoutParams.class.getDeclaredField("privateFlags");
                f.setAccessible(true);
            }
            int flags = f.getInt(lp);
            f.setInt(lp, flags | PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY);
        } catch (Throwable t) {
            Log.w(TAG, "PRIVATE_FLAG screenshot exclusion unavailable: " + t.getMessage());
        }
    }

    static void markView(final View view) {
        if (view == null) return;
        view.post(new Runnable() {
            @Override
            public void run() {
                applySurfaceSkip(view, 3);
            }
        });
    }

    private static void applySurfaceSkip(final View view, final int retriesLeft) {
        try {
            Method getViewRootImpl = View.class.getMethod("getViewRootImpl");
            Object viewRoot = getViewRootImpl.invoke(view);
            if (viewRoot == null) {
                retry(view, retriesLeft);
                return;
            }

            Method getSurfaceControl = viewRoot.getClass().getMethod("getSurfaceControl");
            Object surfaceControl = getSurfaceControl.invoke(viewRoot);
            if (surfaceControl == null) {
                retry(view, retriesLeft);
                return;
            }

            Class<?> surfaceControlClass = Class.forName("android.view.SurfaceControl");
            try {
                Method isValid = surfaceControlClass.getMethod("isValid");
                Object valid = isValid.invoke(surfaceControl);
                if (valid instanceof Boolean && !((Boolean) valid)) {
                    retry(view, retriesLeft);
                    return;
                }
            } catch (Throwable ignored) {}

            Class<?> transactionClass = Class.forName("android.view.SurfaceControl$Transaction");
            Object transaction = transactionClass.getConstructor().newInstance();
            Method setSkipScreenshot = transactionClass.getMethod(
                    "setSkipScreenshot", surfaceControlClass, boolean.class);
            setSkipScreenshot.invoke(transaction, surfaceControl, true);
            transactionClass.getMethod("apply").invoke(transaction);

            try {
                transactionClass.getMethod("close").invoke(transaction);
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            if (retriesLeft > 0) {
                retry(view, retriesLeft);
            } else {
                Log.w(TAG, "SurfaceControl screenshot exclusion unavailable: " + t.getMessage());
            }
        }
    }

    private static void retry(final View view, final int retriesLeft) {
        if (retriesLeft <= 0 || view == null) return;
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                applySurfaceSkip(view, retriesLeft - 1);
            }
        }, 80L);
    }
}
