package cz.boredclawd.samsungmessages;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * Persistent preferences for the Charged SMS and Roaming alert guard.
 */
public final class GuardPreferences {

    public static final String PREFS_NAME = "cz.boredclawd.samsungmessages.prefs";

    public static final String KEY_ENABLED = "guard_enabled";
    public static final String KEY_ROAMING_MODE = "roaming_mode";
    public static final String KEY_INCLUDE_UK = "include_uk_in_eu";
    public static final String KEY_CHECK_NOTIFICATION_REPLY = "check_notification_reply";
    public static final String KEY_CUSTOM_HOME_COUNTRY = "custom_home_country";

    /** Roam Like At Home: warn only when outside EU/EEA or when destination is non-EU. */
    public static final int MODE_EU_RLAH = 0;

    /** Warn on any SMS while roaming, regardless of destination. */
    public static final int MODE_ALL_ROAMING = 1;

    private GuardPreferences() {}

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isGuardEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_ENABLED, true);
    }

    public static void setGuardEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static int getRoamingMode(Context context) {
        return getPrefs(context).getInt(KEY_ROAMING_MODE, MODE_EU_RLAH);
    }

    public static void setRoamingMode(Context context, int mode) {
        getPrefs(context).edit().putInt(KEY_ROAMING_MODE, mode).apply();
    }

    public static boolean isIncludeUkInEu(Context context) {
        return getPrefs(context).getBoolean(KEY_INCLUDE_UK, true);
    }

    public static void setIncludeUkInEu(Context context, boolean includeUk) {
        getPrefs(context).edit().putBoolean(KEY_INCLUDE_UK, includeUk).apply();
    }

    public static boolean isCheckNotificationReply(Context context) {
        return getPrefs(context).getBoolean(KEY_CHECK_NOTIFICATION_REPLY, true);
    }

    public static void setCheckNotificationReply(Context context, boolean checkNotificationReply) {
        getPrefs(context).edit().putBoolean(KEY_CHECK_NOTIFICATION_REPLY, checkNotificationReply).apply();
    }

    public static String getCustomHomeCountry(Context context) {
        return getPrefs(context).getString(KEY_CUSTOM_HOME_COUNTRY, "");
    }

    public static void setCustomHomeCountry(Context context, String countryIso) {
        String cleaned = countryIso != null ? countryIso.trim().toLowerCase(Locale.ROOT) : "";
        getPrefs(context).edit().putString(KEY_CUSTOM_HOME_COUNTRY, cleaned).apply();
    }
}
