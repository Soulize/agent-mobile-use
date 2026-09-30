package com.agent.mobileuse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Root daemon bridge for very short foreground tool operations.
 *
 * The overlay stays rendered, but its Activity window becomes non-focusable and
 * non-touchable long enough for Accessibility/InputDispatcher to reach the app below.
 */
public class OverlayControlReceiver extends BroadcastReceiver {
    public static final String ACTION_PASSTHROUGH =
            "com.agent.mobileuse.ACTION_AGENT_PASSTHROUGH";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_PASSTHROUGH.equals(intent.getAction())) return;
        boolean enabled = intent.getBooleanExtra("enabled", false);
        DemoDialogActivity.setAgentPassthrough(enabled);
        Log.i("OverlayControlReceiver", "passthrough=" + enabled);
    }
}
