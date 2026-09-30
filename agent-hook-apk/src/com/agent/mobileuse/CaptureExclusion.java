package com.agent.mobileuse;

import android.util.Log;
import android.view.View;

import java.lang.reflect.Method;

/**
 * Best-effort screenshot exclusion for an already attached local overlay Surface.
 *
 * Do NOT use PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY from an ordinary app window:
 * WindowManager treats that flag as an internal-system-window capability and denies
 * adding the window when the caller lacks INTERNAL_SYSTEM_WINDOW.
 *
 * Instead, once the view is attached, mark only the app-owned SurfaceControl with
 * SKIP_SCREENSHOT. If an OEM blocks the hidden SurfaceControl API, this fails open:
 * the overlay remains usable and only screenshot exclusion is unavailable.
 */
final class CaptureExclusion {
    private static final String TAG = "CaptureExclusion";

    private CaptureExclusion() {}

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

            Log.i(TAG, "SKIP_SCREENSHOT applied to attached overlay Surface");
        } catch (Throwable t) {
            if (retriesLeft > 0) {
                retry(view, retriesLeft);
            } else {
                Log.w(TAG, "SurfaceControl screenshot exclusion unavailable; overlay left usable: "
                        + t.getClass().getSimpleName() + ": " + t.getMessage());
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
