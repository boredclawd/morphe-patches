package cz.boredclawd.samsungmessages;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Normalizes phone numbers, detects emergency contacts, and identifies destination country prefixes.
 */
public final class PhoneNumberHelper {

    private static final Set<String> COMMON_EMERGENCY_NUMBERS = new HashSet<>(Arrays.asList(
            "112", // European standard emergency
            "911", // North America standard emergency
            "999", // UK emergency
            "150", // Fire (CZ, SK)
            "155", // Ambulance (CZ, SK)
            "158", // Police (CZ, SK)
            "156", // Municipal Police (CZ, SK)
            "000", // Australia emergency
            "110", // Police (DE, JP, CN)
            "119", // Fire / Ambulance (JP, KR, CN)
            "118", // Fire (ID)
            "100", // Police (IN, GR, IL)
            "101", // Fire (IL)
            "102"  // Ambulance (IL)
    ));

    private PhoneNumberHelper() {}

    /**
     * Strips delimiters, spaces, hyphens and converts international prefix "00" to "+".
     */
    public static String normalize(String number) {
        if (number == null) return "";
        String trimmed = number.trim();
        if (trimmed.isEmpty()) return "";

        // Convert leading "00" to "+"
        if (trimmed.startsWith("00")) {
            trimmed = "+" + trimmed.substring(2);
        }

        StringBuilder sb = new StringBuilder(trimmed.length());
        boolean hasLeadingPlus = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (i == 0 && c == '+') {
                sb.append('+');
                hasLeadingPlus = true;
            } else if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Determines whether the number is an emergency service number.
     * Emergency numbers must never be blocked, questioned, or delayed.
     */
    public static boolean isEmergency(Context context, String number) {
        if (number == null || number.isEmpty()) return false;
        String normalized = normalize(number);
        String digitsOnly = normalized.startsWith("+") ? normalized.substring(1) : normalized;

        if (COMMON_EMERGENCY_NUMBERS.contains(digitsOnly) || COMMON_EMERGENCY_NUMBERS.contains(normalized)) {
            return true;
        }

        // Check platform PhoneNumberUtils API
        try {
            if (PhoneNumberUtils.isEmergencyNumber(normalized)) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            Method m = PhoneNumberUtils.class.getMethod("isPotentialEmergencyNumber", String.class);
            Object res = m.invoke(null, normalized);
            if (res instanceof Boolean && ((Boolean) res)) {
                return true;
            }
        } catch (Throwable ignored) {}

        // Check TelephonyManager.isEmergencyNumber on API 29+
        if (context != null) {
            try {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    Method m = tm.getClass().getMethod("isEmergencyNumber", String.class);
                    Object res = m.invoke(tm, normalized);
                    if (res instanceof Boolean && ((Boolean) res)) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {}
        }

        return false;
    }

    /**
     * Extracts destination 2-letter ISO country code.
     * If the number starts with an international prefix (+), it is mapped against known dialing codes.
     * If the number does not start with +, it is treated as a local domestic number matching homeCountry.
     */
    public static String extractDestinationCountry(String number, String homeCountryFallback) {
        if (number == null || number.isEmpty()) return homeCountryFallback;
        String normalized = normalize(number);
        if (normalized.startsWith("+")) {
            String iso = RoamingZone.getCountryForDialCode(normalized);
            if (iso != null && !iso.isEmpty()) {
                return iso;
            }
            return ""; // Unknown country
        }
        // Local number without prefix -> assume home base country
        return homeCountryFallback;
    }
}
