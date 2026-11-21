package cz.boredclawd.samsungmessages;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Catches SecurityException when Samsung Messages registers content observers on providers
 * that require Samsung system/platform permissions (e.g. Call & Message Continuity, logs, spam).
 */
public final class Providers {
    private static final String TAG = "SamsungMessages";

    private Providers() {}

    public static void registerContentObserver(ContentResolver resolver, Uri uri, boolean notifyForDescendants, ContentObserver observer) {
        try {
            resolver.registerContentObserver(uri, notifyForDescendants, observer);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping inaccessible content observer for " + uri + ": " + e.getMessage());
        } catch (Throwable t) {
            Log.w(TAG, "Failed to register content observer for " + uri + ": " + t.getMessage());
        }
    }

    public static void registerContentObserver(ContentResolver resolver, Uri uri, boolean notifyForDescendants, ContentObserver observer, int userHandle) {
        try {
            Method method = ContentResolver.class.getMethod(
                "registerContentObserver",
                Uri.class,
                boolean.class,
                ContentObserver.class,
                int.class
            );
            method.invoke(resolver, uri, notifyForDescendants, observer, userHandle);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SecurityException) {
                Log.w(TAG, "Skipping inaccessible content observer for " + uri + ": " + cause.getMessage());
            } else {
                Log.w(TAG, "Failed to register content observer for " + uri + ": " + cause.getMessage());
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping inaccessible content observer for " + uri + ": " + e.getMessage());
        } catch (Throwable t) {
            // Fallback to standard 3-arg registration if hidden API cannot be invoked
            registerContentObserver(resolver, uri, notifyForDescendants, observer);
        }
    }
}

