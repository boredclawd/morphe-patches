package cz.boredclawd.samsungmessages;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Handles European Union (EU) Roam-Like-At-Home (RLAH) zones, European Economic Area (EEA),
 * United Kingdom (UK), and international phone prefix mappings.
 */
public final class RoamingZone {

    // 27 EU member states (ISO 3166-1 alpha-2)
    private static final Set<String> EU_COUNTRIES = new HashSet<>(Arrays.asList(
            "at", "be", "bg", "hr", "cy", "cz", "dk", "ee", "fi", "fr",
            "de", "gr", "hu", "ie", "it", "lv", "lt", "lu", "mt", "nl",
            "pl", "pt", "ro", "sk", "si", "es", "se"
    ));

    // EEA non-EU countries included in EU RLAH
    private static final Set<String> EEA_COUNTRIES = new HashSet<>(Arrays.asList(
            "is", "li", "no"
    ));

    // EU Outermost Regions (included under EU RLAH)
    private static final Set<String> EU_OUTERMOST_REGIONS = new HashSet<>(Arrays.asList(
            "gp", // Guadeloupe
            "gf", // French Guiana
            "mq", // Martinique
            "re", // Réunion
            "yt", // Mayotte
            "mf", // Saint Martin (French part)
            "ax"  // Åland Islands (Finland)
    ));

    // United Kingdom and Crown dependencies
    private static final Set<String> UK_ZONE = new HashSet<>(Arrays.asList(
            "gb", "uk", "im", "je", "gg", "gi"
    ));

    // Dialing code (without +) to ISO-2 mapping
    private static final Map<String, String> DIAL_CODE_TO_ISO;

    static {
        Map<String, String> map = new HashMap<>(160);

        // 1-digit prefixes
        map.put("1", "us"); // North America (US, CA, etc.)
        map.put("7", "ru"); // Russia / Kazakhstan

        // 2-digit prefixes
        map.put("20", "eg"); // Egypt
        map.put("27", "za"); // South Africa
        map.put("30", "gr"); // Greece
        map.put("31", "nl"); // Netherlands
        map.put("32", "be"); // Belgium
        map.put("33", "fr"); // France
        map.put("34", "es"); // Spain
        map.put("36", "hu"); // Hungary
        map.put("39", "it"); // Italy
        map.put("40", "ro"); // Romania
        map.put("41", "ch"); // Switzerland
        map.put("43", "at"); // Austria
        map.put("44", "gb"); // United Kingdom
        map.put("45", "dk"); // Denmark
        map.put("46", "se"); // Sweden
        map.put("47", "no"); // Norway
        map.put("48", "pl"); // Poland
        map.put("49", "de"); // Germany
        map.put("51", "pe"); // Peru
        map.put("52", "mx"); // Mexico
        map.put("54", "ar"); // Argentina
        map.put("55", "br"); // Brazil
        map.put("56", "cl"); // Chile
        map.put("57", "co"); // Colombia
        map.put("58", "ve"); // Venezuela
        map.put("60", "my"); // Malaysia
        map.put("61", "au"); // Australia
        map.put("62", "id"); // Indonesia
        map.put("63", "ph"); // Philippines
        map.put("64", "nz"); // New Zealand
        map.put("65", "sg"); // Singapore
        map.put("66", "th"); // Thailand
        map.put("81", "jp"); // Japan
        map.put("82", "kr"); // South Korea
        map.put("84", "vn"); // Vietnam
        map.put("86", "cn"); // China
        map.put("90", "tr"); // Turkey
        map.put("91", "in"); // India
        map.put("92", "pk"); // Pakistan
        map.put("93", "af"); // Afghanistan
        map.put("94", "lk"); // Sri Lanka
        map.put("95", "mm"); // Myanmar
        map.put("98", "ir"); // Iran

        // 3-digit prefixes
        map.put("212", "ma"); // Morocco
        map.put("213", "dz"); // Algeria
        map.put("216", "tn"); // Tunisia
        map.put("218", "ly"); // Libya
        map.put("220", "gm"); // Gambia
        map.put("221", "sn"); // Senegal
        map.put("234", "ng"); // Nigeria
        map.put("254", "ke"); // Kenya
        map.put("255", "tz"); // Tanzania
        map.put("256", "ug"); // Uganda
        map.put("262", "re"); // Réunion / Mayotte
        map.put("350", "gi"); // Gibraltar
        map.put("351", "pt"); // Portugal
        map.put("352", "lu"); // Luxembourg
        map.put("353", "ie"); // Ireland
        map.put("354", "is"); // Iceland
        map.put("355", "al"); // Albania
        map.put("356", "mt"); // Malta
        map.put("357", "cy"); // Cyprus
        map.put("358", "fi"); // Finland
        map.put("359", "bg"); // Bulgaria
        map.put("370", "lt"); // Lithuania
        map.put("371", "lv"); // Latvia
        map.put("372", "ee"); // Estonia
        map.put("373", "md"); // Moldova
        map.put("374", "am"); // Armenia
        map.put("375", "by"); // Belarus
        map.put("376", "ad"); // Andorra
        map.put("377", "mc"); // Monaco
        map.put("378", "sm"); // San Marino
        map.put("379", "va"); // Vatican City
        map.put("380", "ua"); // Ukraine
        map.put("381", "rs"); // Serbia
        map.put("382", "me"); // Montenegro
        map.put("383", "xk"); // Kosovo
        map.put("385", "hr"); // Croatia
        map.put("386", "si"); // Slovenia
        map.put("387", "ba"); // Bosnia and Herzegovina
        map.put("389", "mk"); // North Macedonia
        map.put("420", "cz"); // Czech Republic
        map.put("421", "sk"); // Slovakia
        map.put("423", "li"); // Liechtenstein
        map.put("590", "gp"); // Guadeloupe / Saint Martin
        map.put("594", "gf"); // French Guiana
        map.put("596", "mq"); // Martinique
        map.put("961", "lb"); // Lebanon
        map.put("962", "jo"); // Jordan
        map.put("963", "sy"); // Syria
        map.put("964", "iq"); // Iraq
        map.put("965", "kw"); // Kuwait
        map.put("966", "sa"); // Saudi Arabia
        map.put("971", "ae"); // UAE
        map.put("972", "il"); // Israel
        map.put("974", "qa"); // Qatar
        map.put("994", "az"); // Azerbaijan
        map.put("995", "ge"); // Georgia

        // 4-digit prefixes
        map.put("1242", "bs"); // Bahamas
        map.put("1246", "bb"); // Barbados
        map.put("1441", "bm"); // Bermuda
        map.put("1876", "jm"); // Jamaica

        DIAL_CODE_TO_ISO = Collections.unmodifiableMap(map);
    }

    private RoamingZone() {}

    /**
     * Checks if a 2-letter ISO country code belongs to the EU or EEA.
     */
    public static boolean isEuOrEea(String iso) {
        if (iso == null || iso.isEmpty()) return false;
        String lower = iso.toLowerCase(Locale.ROOT);
        return EU_COUNTRIES.contains(lower) || EEA_COUNTRIES.contains(lower) || EU_OUTERMOST_REGIONS.contains(lower);
    }

    /**
     * Checks if a 2-letter ISO country code belongs to the UK zone (GB, UK, IM, JE, GG, GI).
     */
    public static boolean isUk(String iso) {
        if (iso == null || iso.isEmpty()) return false;
        return UK_ZONE.contains(iso.toLowerCase(Locale.ROOT));
    }

    /**
     * Checks if a 2-letter ISO country code belongs to the EU Roam-Like-At-Home zone.
     * Optionally includes the UK if configured.
     */
    public static boolean isEuZone(String iso, boolean includeUk) {
        if (iso == null || iso.isEmpty()) return false;
        if (isEuOrEea(iso)) return true;
        return includeUk && isUk(iso);
    }

    /**
     * Resolves the 2-letter ISO country code for an international phone number.
     * Expects a normalized phone number starting with '+' or digits.
     */
    public static String getCountryForDialCode(String normalizedNumber) {
        if (normalizedNumber == null || normalizedNumber.isEmpty()) return "";

        String digits = normalizedNumber.startsWith("+")
                ? normalizedNumber.substring(1)
                : normalizedNumber;

        // Try prefixes from length 4 down to 1
        int maxLen = Math.min(4, digits.length());
        for (int len = maxLen; len >= 1; len--) {
            String prefix = digits.substring(0, len);
            String iso = DIAL_CODE_TO_ISO.get(prefix);
            if (iso != null) {
                return iso;
            }
        }
        return "";
    }

    /**
     * Returns a human-readable display name for the given country ISO code in the user's locale.
     */
    public static String getCountryDisplayName(String iso, Locale locale) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            Locale countryLocale = new Locale("", iso.toUpperCase(Locale.ROOT));
            String name = countryLocale.getDisplayCountry(locale != null ? locale : Locale.getDefault());
            if (name != null && !name.isEmpty()) {
                return name;
            }
        } catch (Throwable ignored) {}
        return iso.toUpperCase(Locale.ROOT);
    }
}
