package cz.boredclawd.samsungmessages;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.Locale;

/**
 * Displays native One UI confirmation dialogs before sending charged SMS,
 * as well as the settings dialog for configuring the guard.
 */
public final class ChargedSmsDialog {

    private ChargedSmsDialog() {}

    /**
     * Shows a One UI styled confirmation alert dialog.
     */
    public static void showConfirmationDialog(
            final Activity activity,
            final ChargeAssessment assessment,
            final Runnable onProceed
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    buildAndShowDialog(activity, assessment, onProceed);
                } catch (Throwable t) {
                    // Fallback: don't block send if UI dialog fails
                    if (onProceed != null) onProceed.run();
                }
            }
        });
    }

    private static void buildAndShowDialog(
            final Activity activity,
            final ChargeAssessment assessment,
            final Runnable onProceed
    ) {
        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage().toLowerCase(Locale.ROOT);

        String destCountryName = RoamingZone.getCountryDisplayName(assessment.getDestCountry(), locale);
        String netCountryName = RoamingZone.getCountryDisplayName(assessment.getNetworkCountry(), locale);
        String number = assessment.getRecipientNumber();

        String title;
        String message;
        String proceedText;
        String cancelText;
        String settingsText;

        if ("cs".equals(lang)) {
            title = "Upozornění na zpoplatněnou SMS";
            proceedText = "Odeslat";
            cancelText = "Zrušit";
            settingsText = "Nastavení";

            switch (assessment.getReason()) {
                case ROAMING_OUTSIDE_EU:
                    message = String.format(
                            "Nacházíte se v roamingu mimo EU (%s).\nOdeslání zprávy příjemci %s (%s) bude zpoplatněno roamingovým tarifem.\n\nPřejete si zprávu odeslat?",
                            netCountryName, number, destCountryName
                    );
                    break;
                case ROAMING_DEST_OUTSIDE_EU:
                    message = String.format(
                            "Nacházíte se v roamingu v EU (%s), avšak příjemce %s (%s) je mimo EU roamingovou zónu.\nOdeslání zprávy bude zpoplatněno.\n\nPřejete si zprávu odeslat?",
                            netCountryName, number, destCountryName
                    );
                    break;
                case ALL_ROAMING_POLICY:
                    message = String.format(
                            "Nacházíte se v roamingu (%s).\nOdeslání zprávy příjemci %s (%s) může být zpoplatněno.\n\nPřejete si zprávu odeslat?",
                            netCountryName, number, destCountryName
                    );
                    break;
                case INTERNATIONAL_FROM_HOME:
                default:
                    message = String.format(
                            "Tato zpráva příjemci %s (%s) je mezinárodní a bude vaším operátorem zpoplatněna nad rámec běžného tarifu.\n\nPřejete si zprávu odeslat?",
                            number, destCountryName
                    );
                    break;
            }
        } else if ("sk".equals(lang)) {
            title = "Upozornenie na spoplatnenú SMS";
            proceedText = "Odoslať";
            cancelText = "Zrušiť";
            settingsText = "Nastavenia";

            switch (assessment.getReason()) {
                case ROAMING_OUTSIDE_EU:
                    message = String.format(
                            "Nachádzate sa v roamingu mimo EÚ (%s).\nOdoslanie správy príjemcovi %s (%s) bude spoplatnené roamingovou tarifou.\n\nŽeláte si správu odoslať?",
                            netCountryName, number, destCountryName
                    );
                    break;
                case ROAMING_DEST_OUTSIDE_EU:
                    message = String.format(
                            "Nachádzate sa v roamingu v EÚ (%s), avšak príjemca %s (%s) je mimo roamingovú zónu EÚ.\nOdoslanie správy bude spoplatnené.\n\nŽeláte si správu odoslať?",
                            netCountryName, number, destCountryName
                    );
                    break;
                case ALL_ROAMING_POLICY:
                    message = String.format(
                            "Nachádzate sa v roamingu (%s).\nOdoslanie správy príjemcovi %s (%s) môže byť spoplatnené.\n\nŽeláte si správu odoslať?",
                            netCountryName, number, destCountryName
                    );
                    break;
                case INTERNATIONAL_FROM_HOME:
                default:
                    message = String.format(
                            "Táto správa príjemcovi %s (%s) je medzinárodná a bude vaším operátorom spoplatnená nad rámec bežného paušálu.\n\nŽeláte si správu odoslať?",
                            number, destCountryName
                    );
                    break;
            }
        } else {
            // English default
            title = "Charged SMS Alert";
            proceedText = "Send";
            cancelText = "Cancel";
            settingsText = "Settings";

            switch (assessment.getReason()) {
                case ROAMING_OUTSIDE_EU:
                    message = String.format(
                            "You are currently roaming outside the EU (%s).\nSending an SMS to %s (%s) will be billed at roaming rates.\n\nDo you want to proceed?",
                            netCountryName, number, destCountryName
                    );
                    break;
                case ROAMING_DEST_OUTSIDE_EU:
                    message = String.format(
                            "You are roaming in the EU (%s), but the recipient %s (%s) is outside the EU roaming zone.\nExtra carrier charges will apply.\n\nDo you want to proceed?",
                            netCountryName, number, destCountryName
                    );
                    break;
                case ALL_ROAMING_POLICY:
                    message = String.format(
                            "You are roaming (%s).\nSending an SMS to %s (%s) may incur extra carrier charges.\n\nDo you want to proceed?",
                            netCountryName, number, destCountryName
                    );
                    break;
                case INTERNATIONAL_FROM_HOME:
                default:
                    message = String.format(
                            "This message to %s (%s) is an international SMS and will incur extra carrier charges.\n\nDo you want to proceed?",
                            number, destCountryName
                    );
                    break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(proceedText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (onProceed != null) {
                    onProceed.run();
                }
            }
        });
        builder.setNegativeButton(cancelText, null);
        builder.setNeutralButton(settingsText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showSettingsDialog(activity);
            }
        });
        builder.show();
    }

    /**
     * Shows the configuration dialog for SMS roaming guard options.
     */
    public static void showSettingsDialog(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage().toLowerCase(Locale.ROOT);

        String settingsTitle;
        String itemGuard;
        String itemRoamingMode;
        String itemUk;
        String itemNotifReply;
        String closeText;

        if ("cs".equals(lang)) {
            settingsTitle = "Nastavení ochrany SMS";
            itemGuard = "Aktivní ochrana: ";
            itemRoamingMode = "Režim roamingu: ";
            itemUk = "Zahrnout Velkou Británii do EU: ";
            itemNotifReply = "Kontrola odpovědí z oznámení: ";
            closeText = "Zavřít";
        } else if ("sk".equals(lang)) {
            settingsTitle = "Nastavenie ochrany SMS";
            itemGuard = "Aktívna ochrana: ";
            itemRoamingMode = "Režim roamingu: ";
            itemUk = "Zahrnúť Spojené kráľovstvo do EÚ: ";
            itemNotifReply = "Kontrola odpovedí z oznámení: ";
            closeText = "Zavrieť";
        } else {
            settingsTitle = "SMS Guard Settings";
            itemGuard = "Guard Enabled: ";
            itemRoamingMode = "Roaming Mode: ";
            itemUk = "Include UK in EU Zone: ";
            itemNotifReply = "Check Notification Replies: ";
            closeText = "Close";
        }

        final boolean enabled = GuardPreferences.isGuardEnabled(activity);
        final int roamingMode = GuardPreferences.getRoamingMode(activity);
        final boolean includeUk = GuardPreferences.isIncludeUkInEu(activity);
        final boolean checkNotif = GuardPreferences.isCheckNotificationReply(activity);

        String onStr = "cs".equals(lang) ? "Zapnuto" : ("sk".equals(lang) ? "Zapnuté" : "ON");
        String offStr = "cs".equals(lang) ? "Vypnuto" : ("sk".equals(lang) ? "Vypnuté" : "OFF");
        String rlahStr = "cs".equals(lang) ? "EU RLAH (zdarma v EU)" : ("sk".equals(lang) ? "EÚ RLAH (zadarmo v EÚ)" : "EU RLAH (Free in EU)");
        String allStr = "cs".equals(lang) ? "Všechny roamingové SMS" : ("sk".equals(lang) ? "Všetky roamingové SMS" : "All Roaming SMS");

        String[] items = new String[]{
                itemGuard + (enabled ? onStr : offStr),
                itemRoamingMode + (roamingMode == GuardPreferences.MODE_EU_RLAH ? rlahStr : allStr),
                itemUk + (includeUk ? onStr : offStr),
                itemNotifReply + (checkNotif ? onStr : offStr)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(settingsTitle);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        GuardPreferences.setGuardEnabled(activity, !enabled);
                        showSettingsDialog(activity);
                        break;
                    case 1:
                        int nextMode = roamingMode == GuardPreferences.MODE_EU_RLAH
                                ? GuardPreferences.MODE_ALL_ROAMING
                                : GuardPreferences.MODE_EU_RLAH;
                        GuardPreferences.setRoamingMode(activity, nextMode);
                        showSettingsDialog(activity);
                        break;
                    case 2:
                        GuardPreferences.setIncludeUkInEu(activity, !includeUk);
                        showSettingsDialog(activity);
                        break;
                    case 3:
                        GuardPreferences.setCheckNotificationReply(activity, !checkNotif);
                        showSettingsDialog(activity);
                        break;
                }
            }
        });
        builder.setPositiveButton(closeText, null);
        builder.show();
    }
}
