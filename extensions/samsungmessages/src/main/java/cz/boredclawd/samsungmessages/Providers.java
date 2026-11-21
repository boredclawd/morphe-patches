package cz.boredclawd.samsungmessages;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Catches SecurityException when Samsung Messages accesses providers
 * that require Samsung system/platform permissions (e.g. Call & Message Continuity, logs, spam, IMS settings).
 */
public final class Providers {
    private static final String TAG = "SamsungMessages";

    private Providers() {}

    public static Cursor query(ContentResolver resolver, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping query for " + uri + ": " + e.getMessage());
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "Failed query for " + uri + ": " + t.getMessage());
            return null;
        }
    }

    public static Cursor query(ContentResolver resolver, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        try {
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping query for " + uri + ": " + e.getMessage());
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "Failed query for " + uri + ": " + t.getMessage());
            return null;
        }
    }

    public static Cursor query(ContentResolver resolver, Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal) {
        try {
            return resolver.query(uri, projection, queryArgs, cancellationSignal);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping query for " + uri + ": " + e.getMessage());
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "Failed query for " + uri + ": " + t.getMessage());
            return null;
        }
    }

    public static Uri insert(ContentResolver resolver, Uri uri, ContentValues values) {
        try {
            return resolver.insert(uri, values);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping insert for " + uri + ": " + e.getMessage());
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "Failed insert for " + uri + ": " + t.getMessage());
            return null;
        }
    }

    public static Uri insert(ContentResolver resolver, Uri uri, ContentValues values, Bundle extras) {
        try {
            return resolver.insert(uri, values, extras);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping insert for " + uri + ": " + e.getMessage());
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "Failed insert for " + uri + ": " + t.getMessage());
            return null;
        }
    }

    public static int bulkInsert(ContentResolver resolver, Uri uri, ContentValues[] values) {
        try {
            return resolver.bulkInsert(uri, values);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping bulkInsert for " + uri + ": " + e.getMessage());
            return 0;
        } catch (Throwable t) {
            Log.w(TAG, "Failed bulkInsert for " + uri + ": " + t.getMessage());
            return 0;
        }
    }

    public static int update(ContentResolver resolver, Uri uri, ContentValues values, String where, String[] selectionArgs) {
        try {
            return resolver.update(uri, values, where, selectionArgs);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping update for " + uri + ": " + e.getMessage());
            return 0;
        } catch (Throwable t) {
            Log.w(TAG, "Failed update for " + uri + ": " + t.getMessage());
            return 0;
        }
    }

    public static int update(ContentResolver resolver, Uri uri, ContentValues values, Bundle extras) {
        try {
            return resolver.update(uri, values, extras);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping update for " + uri + ": " + e.getMessage());
            return 0;
        } catch (Throwable t) {
            Log.w(TAG, "Failed update for " + uri + ": " + t.getMessage());
            return 0;
        }
    }

    public static int delete(ContentResolver resolver, Uri uri, String where, String[] selectionArgs) {
        try {
            return resolver.delete(uri, where, selectionArgs);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping delete for " + uri + ": " + e.getMessage());
            return 0;
        } catch (Throwable t) {
            Log.w(TAG, "Failed delete for " + uri + ": " + t.getMessage());
            return 0;
        }
    }

    public static int delete(ContentResolver resolver, Uri uri, Bundle extras) {
        try {
            return resolver.delete(uri, extras);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping delete for " + uri + ": " + e.getMessage());
            return 0;
        } catch (Throwable t) {
            Log.w(TAG, "Failed delete for " + uri + ": " + t.getMessage());
            return 0;
        }
    }

    public static Bundle call(ContentResolver resolver, Uri uri, String method, String arg, Bundle extras) {
        try {
            return resolver.call(uri, method, arg, extras);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping call for " + uri + ": " + e.getMessage());
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "Failed call for " + uri + ": " + t.getMessage());
            return null;
        }
    }

    public static Bundle call(ContentResolver resolver, String authority, String method, String arg, Bundle extras) {
        try {
            return resolver.call(authority, method, arg, extras);
        } catch (SecurityException e) {
            Log.w(TAG, "Skipping call for " + authority + ": " + e.getMessage());
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "Failed call for " + authority + ": " + t.getMessage());
            return null;
        }
    }

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
