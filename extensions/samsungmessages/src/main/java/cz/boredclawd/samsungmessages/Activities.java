package cz.boredclawd.samsungmessages;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public final class Activities {
    private static final String TAG = "SamsungMessages";

    private Activities() {}

    private static void fixIntent(Context context, Intent intent) {
        if (intent == null || context == null) return;
        String myPkg = context.getPackageName();
        if ("com.samsung.android.messaging".equals(intent.getPackage())) {
            intent.setPackage(myPkg);
        }
        ComponentName component = intent.getComponent();
        if (component != null && "com.samsung.android.messaging".equals(component.getPackageName())) {
            intent.setComponent(new ComponentName(myPkg, component.getClassName()));
        }
    }

    public static void startActivity(Context context, Intent intent) {
        fixIntent(context, intent);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Activity not found: " + intent, e);
        }
    }

    public static void startActivity(Context context, Intent intent, Bundle options) {
        fixIntent(context, intent);
        try {
            context.startActivity(intent, options);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Activity not found: " + intent, e);
        }
    }

    public static void startActivity(Activity activity, Intent intent) {
        fixIntent(activity, intent);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Activity not found: " + intent, e);
        }
    }

    public static void startActivity(Activity activity, Intent intent, Bundle options) {
        fixIntent(activity, intent);
        try {
            activity.startActivity(intent, options);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Activity not found: " + intent, e);
        }
    }

    public static void startActivityForResult(Activity activity, Intent intent, int requestCode) {
        fixIntent(activity, intent);
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Activity not found: " + intent, e);
        }
    }

    public static void startActivityForResult(Activity activity, Intent intent, int requestCode, Bundle options) {
        fixIntent(activity, intent);
        try {
            activity.startActivityForResult(intent, requestCode, options);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Activity not found: " + intent, e);
        }
    }
}
