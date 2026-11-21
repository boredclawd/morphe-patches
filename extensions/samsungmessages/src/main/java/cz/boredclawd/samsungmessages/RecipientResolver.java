package cz.boredclawd.samsungmessages;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Robust recipient resolver for Samsung Messages across different One UI versions.
 * Uses a multi-stage waterfall: View hierarchy -> Intent extras -> Telephony Provider -> Active Fragments.
 */
public final class RecipientResolver {

    private static final Uri CANONICAL_ADDRESS_URI = Uri.parse("content://mms-sms/canonical-address");
    private static final Uri CONVERSATIONS_SIMPLE_URI = Uri.parse("content://mms-sms/conversations?simple=true");
    private static final Uri SAMSUNG_CONVERSATION_RECIPIENTS_URI =
            Uri.parse("content://com.samsung.android.messaging.service.provider.MessageContentProvider/recipients_by_conversation_id");

    private RecipientResolver() {}

    /**
     * Attempts to resolve all recipient phone numbers for the active composition window.
     */
    public static List<String> resolveRecipients(View sendButton) {
        if (sendButton == null) return Collections.emptyList();

        Set<String> resultSet = new HashSet<>();

        // 1. Inspect View hierarchy (RecipientsEditor, ChipListView, RecipientsPanel)
        try {
            View root = sendButton.getRootView();
            if (root instanceof ViewGroup) {
                collectFromViewHierarchy((ViewGroup) root, resultSet);
            }
        } catch (Throwable ignored) {}

        // If found from editor or chips, return immediately
        if (!resultSet.isEmpty()) {
            return new ArrayList<>(resultSet);
        }

        // 2. Unwrap Context to find Activity
        Activity activity = findActivity(sendButton.getContext());
        if (activity != null) {
            // 2a. Check Intent extras
            try {
                collectFromIntent(activity.getIntent(), resultSet);
            } catch (Throwable ignored) {}

            // 2b. Check Fragments
            try {
                collectFromFragments(activity, resultSet);
            } catch (Throwable ignored) {}

            // 2c. Check thread_id from Intent via ContentResolver
            if (resultSet.isEmpty()) {
                try {
                    Intent intent = activity.getIntent();
                    if (intent != null) {
                        long threadId = intent.getLongExtra("thread_id", -1);
                        if (threadId <= 0) {
                            threadId = intent.getLongExtra("conversation_id", -1);
                        }
                        if (threadId > 0) {
                            collectFromThreadId(activity, threadId, resultSet);
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        return new ArrayList<>(resultSet);
    }

    public static Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    private static void collectFromViewHierarchy(ViewGroup root, Set<String> out) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child == null) continue;

            String className = child.getClass().getName();

            // Check RecipientsEditor
            if (className.endsWith("RecipientsEditor")) {
                if (child instanceof TextView) {
                    CharSequence text = ((TextView) child).getText();
                    if (text != null && text.length() > 0) {
                        addParsedAddresses(text.toString(), out);
                    }
                }
            }
            // Check RecipientsPanel chips
            else if (className.endsWith("RecipientsPanel") || className.endsWith("RecipientChipListView")) {
                extractFromChips(child, out);
            }

            if (child instanceof ViewGroup) {
                collectFromViewHierarchy((ViewGroup) child, out);
            }
        }
    }

    private static void extractFromChips(View chipContainer, Set<String> out) {
        try {
            // Try getChipList() method via reflection
            Method getChipList = chipContainer.getClass().getMethod("getChipList");
            Object chips = getChipList.invoke(chipContainer);
            if (chips instanceof Collection) {
                for (Object chip : (Collection<?>) chips) {
                    if (chip != null) {
                        addParsedAddresses(chip.toString(), out);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Also check any TextView children inside chip container
        if (chipContainer instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) chipContainer;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof TextView) {
                    CharSequence text = ((TextView) child).getText();
                    if (text != null && text.length() > 0) {
                        addParsedAddresses(text.toString(), out);
                    }
                }
            }
        }
    }

    private static void collectFromIntent(Intent intent, Set<String> out) {
        if (intent == null) return;

        String address = intent.getStringExtra("address");
        if (address != null && !address.isEmpty()) {
            addParsedAddresses(address, out);
        }

        String[] recipients = intent.getStringArrayExtra("recipients");
        if (recipients != null) {
            for (String r : recipients) {
                addParsedAddresses(r, out);
            }
        }

        Uri data = intent.getData();
        if (data != null) {
            String ssp = data.getSchemeSpecificPart();
            if (ssp != null && !ssp.isEmpty()) {
                addParsedAddresses(ssp, out);
            }
        }
    }

    /**
     * Resolves all recipient phone numbers for a given thread / conversation ID.
     */
    public static List<String> resolveRecipientsFromThreadId(Context context, long threadId) {
        if (context == null || threadId <= 0) return Collections.emptyList();
        Set<String> set = new HashSet<>();
        collectFromThreadId(context, threadId, set);
        return new ArrayList<>(set);
    }

    private static void collectFromThreadId(Context context, long threadId, Set<String> out) {
        // First try standard Android Telephony simple threads
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    CONVERSATIONS_SIMPLE_URI,
                    new String[]{"_id", "recipient_ids"},
                    "_id = ?",
                    new String[]{String.valueOf(threadId)},
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                int colRecipientIds = cursor.getColumnIndex("recipient_ids");
                if (colRecipientIds >= 0) {
                    String recipientIdsStr = cursor.getString(colRecipientIds);
                    if (recipientIdsStr != null && !recipientIdsStr.isEmpty()) {
                        String[] ids = recipientIdsStr.trim().split("\\s+");
                        for (String id : ids) {
                            String addr = queryCanonicalAddress(context, id);
                            if (addr != null && !addr.isEmpty()) {
                                addParsedAddresses(addr, out);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }

        // Second: try Samsung provider
        if (out.isEmpty()) {
            Cursor sCursor = null;
            try {
                Uri samsungUri = Uri.withAppendedPath(SAMSUNG_CONVERSATION_RECIPIENTS_URI, String.valueOf(threadId));
                sCursor = context.getContentResolver().query(samsungUri, null, null, null, null);
                if (sCursor != null && sCursor.moveToFirst()) {
                    int addrCol = sCursor.getColumnIndex("address");
                    if (addrCol >= 0) {
                        do {
                            String addr = sCursor.getString(addrCol);
                            if (addr != null && !addr.isEmpty()) {
                                addParsedAddresses(addr, out);
                            }
                        } while (sCursor.moveToNext());
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (sCursor != null) sCursor.close();
            }
        }
    }

    private static String queryCanonicalAddress(Context context, String recipientId) {
        Cursor cursor = null;
        try {
            Uri uri = Uri.withAppendedPath(CANONICAL_ADDRESS_URI, recipientId);
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex("address");
                if (col >= 0) {
                    return cursor.getString(col);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    private static void collectFromFragments(Activity activity, Set<String> out) {
        try {
            Method getFM = activity.getClass().getMethod("getSupportFragmentManager");
            Object fm = getFM.invoke(activity);
            if (fm == null) return;

            Method getFragments = fm.getClass().getMethod("getFragments");
            Object frags = getFragments.invoke(fm);
            if (frags instanceof List) {
                for (Object f : (List<?>) frags) {
                    if (f == null) continue;
                    // Check if fragment has getAddresses or getRecipients or controller
                    inspectObjectForRecipients(f, out);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void inspectObjectForRecipients(Object obj, Set<String> out) {
        if (obj == null) return;
        try {
            for (Method m : obj.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0) {
                    String name = m.getName();
                    if (name.equals("getRecipients") || name.equals("getAddresses") || name.equals("getRecipientList")) {
                        Object res = m.invoke(obj);
                        if (res instanceof String[]) {
                            for (String s : (String[]) res) addParsedAddresses(s, out);
                        } else if (res instanceof Collection) {
                            for (Object o : (Collection<?>) res) {
                                if (o != null) addParsedAddresses(o.toString(), out);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void addParsedAddresses(String raw, Set<String> out) {
        if (raw == null || raw.isEmpty()) return;
        // Split on common delimiters (comma, semicolon, newline)
        String[] tokens = raw.split("[,;\n]+");
        for (String token : tokens) {
            String norm = PhoneNumberHelper.normalize(token);
            // Must have at least 3 digits to be a valid phone number
            if (norm.length() >= 3) {
                out.add(norm);
            }
        }
    }
}
