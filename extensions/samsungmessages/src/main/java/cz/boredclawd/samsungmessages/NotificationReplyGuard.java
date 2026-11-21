package cz.boredclawd.samsungmessages;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Validates notification inline replies (RemoteInput) in NotificationActionService.
 * If an outgoing notification reply would incur additional carrier charges,
 * the immediate background send is halted and an actionable confirmation notification is displayed.
 */
public final class NotificationReplyGuard {

    public static final String ACTION_CANCEL_REPLY = "cz.boredclawd.samsungmessages.action.CANCEL_REPLY";
    public static final String EXTRA_CONFIRMED_CHARGED = "cz.boredclawd.CONFIRMED_CHARGED";
    public static final String EXTRA_SAVED_REPLY_TEXT = "cz.boredclawd.SAVED_REPLY_TEXT";
    public static final String EXTRA_NOTIFICATION_ID = "cz.boredclawd.NOTIFICATION_ID";

    private static final String CHANNEL_ID = "charged_sms_alert_channel";

    private NotificationReplyGuard() {}

    /**
     * Intercepts NotificationActionService.onHandleIntent.
     * Returns true to allow original handling to continue, or false to abort.
     */
    public static boolean shouldHandleIntent(Service service, Intent intent) {
        if (service == null || intent == null) return true;

        String action = intent.getAction();

        // 1. Handle user tapping "Cancel" on our confirmation notification
        if (ACTION_CANCEL_REPLY.equals(action)) {
            int notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
            if (notifId != -1) {
                NotificationManager nm = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancel(notifId);
            }
            return false; // Handled cancel, abort further service processing
        }

        // Only validate direct reply action
        if (!"com.samsung.android.messaging.action.REPLY_MESSAGE".equals(action)) {
            return true;
        }

        // 2. Check preferences: is guard enabled & notification reply check enabled?
        if (!GuardPreferences.isGuardEnabled(service) || !GuardPreferences.isCheckNotificationReply(service)) {
            return true;
        }

        // 3. If user already tapped "Send Anyway" on our notification, dismiss notification and proceed
        if (intent.getBooleanExtra(EXTRA_CONFIRMED_CHARGED, false)) {
            int notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
            if (notifId != -1) {
                NotificationManager nm = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancel(notifId);
            }
            // Ensure RemoteInput results exist for original code
            ensureReplyTextAttached(intent);
            return true;
        }

        // 4. Extract reply text
        CharSequence replyText = extractReplyText(intent);
        if (replyText == null || replyText.length() == 0) {
            return true; // No reply text, let original code handle/skip
        }

        // 5. Extract thread / conversation ID
        long threadId = intent.getLongExtra("conversationId", -1);
        if (threadId <= 0) threadId = intent.getLongExtra("thread_id", -1);
        if (threadId <= 0) threadId = intent.getLongExtra("conversation_id", -1);
        if (threadId <= 0) return true; // Cannot determine conversation

        // 6. Extract SIM slot
        int simSlot = intent.getIntExtra("sim_slot", 0);
        if (simSlot < 0) simSlot = intent.getIntExtra("simSlot", 0);
        if (simSlot < 0) simSlot = 0;

        // 7. Resolve recipient phone numbers for this conversation thread
        List<String> recipients = RecipientResolver.resolveRecipientsFromThreadId(service, threadId);
        if (recipients.isEmpty()) {
            String addr = intent.getStringExtra("address");
            if (addr == null) addr = intent.getStringExtra("recipient");
            if (addr != null && !addr.isEmpty()) {
                recipients = Collections.singletonList(PhoneNumberHelper.normalize(addr));
            }
        }
        if (recipients.isEmpty()) {
            return true; // Cannot resolve recipient, do not block
        }

        // 8. Determine telephony and roaming status
        SubscriptionManager sm = (SubscriptionManager) service.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        SubscriptionInfo subInfo = null;
        if (sm != null) {
            try {
                subInfo = sm.getActiveSubscriptionInfoForSimSlotIndex(simSlot);
            } catch (Throwable ignored) {}
        }

        TelephonyManager tm = (TelephonyManager) service.getSystemService(Context.TELEPHONY_SERVICE);
        if (subInfo != null && tm != null) {
            try {
                tm = tm.createForSubscriptionId(subInfo.getSubscriptionId());
            } catch (Throwable ignored) {}
        }

        String homeCountry;
        String customHome = GuardPreferences.getCustomHomeCountry(service);
        if (customHome != null && !customHome.isEmpty()) {
            homeCountry = customHome;
        } else if (subInfo != null && subInfo.getCountryIso() != null && !subInfo.getCountryIso().isEmpty()) {
            homeCountry = subInfo.getCountryIso().toLowerCase(Locale.ROOT);
        } else if (tm != null && tm.getSimCountryIso() != null && !tm.getSimCountryIso().isEmpty()) {
            homeCountry = tm.getSimCountryIso().toLowerCase(Locale.ROOT);
        } else {
            homeCountry = Locale.getDefault().getCountry().toLowerCase(Locale.ROOT);
        }

        String networkCountry = "";
        if (tm != null && tm.getNetworkCountryIso() != null) {
            networkCountry = tm.getNetworkCountryIso().toLowerCase(Locale.ROOT);
        }

        boolean isRoaming = false;
        if (tm != null && tm.isNetworkRoaming()) {
            isRoaming = true;
        } else if (!networkCountry.isEmpty() && !networkCountry.equalsIgnoreCase(homeCountry)) {
            isRoaming = true;
        }

        if (networkCountry.isEmpty()) {
            networkCountry = isRoaming ? "roaming" : homeCountry;
        }

        int roamingMode = GuardPreferences.getRoamingMode(service);
        boolean includeUk = GuardPreferences.isIncludeUkInEu(service);

        // 9. Assess charge for each recipient
        ChargeAssessment chargedAssessment = null;
        for (String recipient : recipients) {
            if (PhoneNumberHelper.isEmergency(service, recipient)) continue;

            String destCountry = PhoneNumberHelper.extractDestinationCountry(recipient, homeCountry);
            ChargeAssessment assessment = ChargedSmsGuard.assessCharge(
                    recipient, homeCountry, networkCountry, destCountry, isRoaming, roamingMode, includeUk
            );
            if (assessment.isCharged()) {
                chargedAssessment = assessment;
                break;
            }
        }

        // 10. If charged, halt background send and post confirmation notification
        if (chargedAssessment != null) {
            showConfirmationNotification(service, intent, threadId, replyText, chargedAssessment);
            return false; // ABORT immediate background send!
        }

        return true;
    }

    private static CharSequence extractReplyText(Intent intent) {
        Bundle results = RemoteInput.getResultsFromIntent(intent);
        if (results != null) {
            CharSequence text = results.getCharSequence("KEY_DIRECT_REPLY");
            if (text != null && text.length() > 0) return text;
            for (String key : results.keySet()) {
                CharSequence cs = results.getCharSequence(key);
                if (cs != null && cs.length() > 0) return cs;
            }
        }
        String saved = intent.getStringExtra(EXTRA_SAVED_REPLY_TEXT);
        if (saved != null && !saved.isEmpty()) return saved;
        return null;
    }

    private static void ensureReplyTextAttached(Intent intent) {
        String saved = intent.getStringExtra(EXTRA_SAVED_REPLY_TEXT);
        if (saved != null && !saved.isEmpty()) {
            Bundle bundle = RemoteInput.getResultsFromIntent(intent);
            if (bundle == null) bundle = new Bundle();
            bundle.putCharSequence("KEY_DIRECT_REPLY", saved);
            RemoteInput.addResultsToIntent(
                    new RemoteInput[]{new RemoteInput.Builder("KEY_DIRECT_REPLY").build()},
                    intent,
                    bundle
            );
        }
    }

    private static void showConfirmationNotification(
            Service service,
            Intent originalIntent,
            long threadId,
            CharSequence replyText,
            ChargeAssessment assessment
    ) {
        NotificationManager nm = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        createNotificationChannel(service, nm);

        int notifId = (int) (threadId > 0 ? (threadId & 0x7FFFFFFF) : 987654);

        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage().toLowerCase(Locale.ROOT);

        String destCountryName = RoamingZone.getCountryDisplayName(assessment.getDestCountry(), locale);
        String number = assessment.getRecipientNumber();

        String title;
        String message;
        String sendAnywayText;
        String cancelText;

        if ("cs".equals(lang)) {
            title = "Upozornění: Zpoplatněná odpověď";
            message = String.format("Odpověď příjemci %s (%s) bude zpoplatněna. Přejete si ji přesto odeslat?", number, destCountryName);
            sendAnywayText = "Odeslat přesto";
            cancelText = "Zrušit";
        } else if ("sk".equals(lang)) {
            title = "Upozornenie: Spoplatnená odpoveď";
            message = String.format("Odpoveď príjemcovi %s (%s) bude spoplatnená. Želáte si ju aj tak odoslať?", number, destCountryName);
            sendAnywayText = "Odoslať aj tak";
            cancelText = "Zrušiť";
        } else {
            title = "Charged SMS Alert: Direct Reply";
            message = String.format("Direct reply to %s (%s) will incur carrier charges. Send anyway?", number, destCountryName);
            sendAnywayText = "Send Anyway";
            cancelText = "Cancel";
        }

        // Action 1: Send Anyway
        Intent sendAnywayIntent = new Intent(originalIntent);
        sendAnywayIntent.setClass(service, service.getClass());
        sendAnywayIntent.putExtra(EXTRA_CONFIRMED_CHARGED, true);
        sendAnywayIntent.putExtra(EXTRA_SAVED_REPLY_TEXT, replyText.toString());
        sendAnywayIntent.putExtra(EXTRA_NOTIFICATION_ID, notifId);
        Bundle replyBundle = new Bundle();
        replyBundle.putCharSequence("KEY_DIRECT_REPLY", replyText);
        RemoteInput.addResultsToIntent(
                new RemoteInput[]{new RemoteInput.Builder("KEY_DIRECT_REPLY").build()},
                sendAnywayIntent,
                replyBundle
        );
        PendingIntent piSendAnyway = PendingIntent.getService(
                service,
                (int) (threadId * 10 + 1),
                sendAnywayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        // Action 2: Cancel
        Intent cancelIntent = new Intent(service, service.getClass());
        cancelIntent.setAction(ACTION_CANCEL_REPLY);
        cancelIntent.putExtra(EXTRA_NOTIFICATION_ID, notifId);
        PendingIntent piCancel = PendingIntent.getService(
                service,
                (int) (threadId * 10 + 2),
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Content intent (tap notification body to open app)
        Intent openAppIntent = new Intent();
        openAppIntent.setClassName(service.getPackageName(), "com.samsung.android.messaging.ui.view.main.WithActivity");
        openAppIntent.putExtra("thread_id", threadId);
        openAppIntent.putExtra("conversationId", threadId);
        openAppIntent.putExtra("address", number);
        openAppIntent.putExtra("draft_text", replyText.toString());
        openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent piOpen = PendingIntent.getActivity(
                service,
                (int) (threadId * 10 + 3),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int iconRes = service.getApplicationInfo().icon;
        if (iconRes == 0) iconRes = android.R.drawable.stat_notify_chat;

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(service, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(service);
        }

        builder.setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(piOpen)
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(0, sendAnywayText, piSendAnyway).build())
                .addAction(new Notification.Action.Builder(0, cancelText, piCancel).build());

        nm.notify(notifId, builder.build());
    }

    private static void createNotificationChannel(Context context, NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Charged SMS Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Alerts when an SMS reply would incur additional carrier charges.");
                nm.createNotificationChannel(channel);
            } catch (Throwable ignored) {}
        }
    }
}
