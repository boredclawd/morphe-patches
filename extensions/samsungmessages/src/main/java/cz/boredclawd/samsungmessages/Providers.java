package cz.boredclawd.samsungmessages;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.util.Log;

/**
 * Catches SecurityException when Samsung Messages registers content observers on providers
 * that require Samsung system/platform permissions (e.g. Call & Message Continuity, logs, spam).
 */
public final class Providers {
    private Providers() {}

    public static void registerContentObserver(ContentResolver resolver, Uri uri, boolean notifyForDescendants, ContentObserver observer) {
        try {
            resolver.registerContentObserver(uri, notifyForDescendants, observer);
        } catch (SecurityException e) {
            Log.w("SamsungMessages", "Skipping inaccessible content observer for " + uri + ": " + e.getMessage());
        }
    }

    public static void registerContentObserver(ContentResolver resolver, Uri uri, boolean notifyForDescendants, ContentObserver observer, int userHandle) {
        try {
            resolver.registerContentObserver(uri, notifyForDescendants, observer, userHandle);
        } catch (SecurityException e) {
            Log.w("SamsungMessages", "Skipping inaccessible content observer for " + uri + ": " + e.getMessage());
        }
    }
}
