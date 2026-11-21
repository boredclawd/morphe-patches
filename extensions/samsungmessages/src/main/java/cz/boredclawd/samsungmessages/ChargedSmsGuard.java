package cz.boredclawd.samsungmessages;

import android.app.Activity;
import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.View;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Intercepts Samsung Messages send button clicks to verify whether an SMS/MMS
 * will incur additional carrier charges (international SMS or out-of-EU roaming).
 */
public final class ChargedSmsGuard {

    private static class ButtonState {
        int composerMode = 1; // 1 = SMS, 2 = MMS, 3 = RCS/IM
        int simSlot = 0;      // 0 = SIM 1, 1 = SIM 2
    }

    private static final Map<View, ButtonState> BUTTON_STATES =
            Collections.synchronizedMap(new WeakHashMap<View, ButtonState>());

    private ChargedSmsGuard() {}

    /**
     * Hooked at the entry of SendButtonLayout's state update method:
     * - 15.5.31.11: b(IIZZZ[Z[Z)V
     * - 16.3.21.1:  a(IIZZZ[Z[Z)V
     */
    public static void onUpdateState(View sendButton, int composerMode, int simSlot) {
        if (sendButton == null) return;
        ButtonState state = BUTTON_STATES.get(sendButton);
        if (state == null) {
            state = new ButtonState();
            BUTTON_STATES.put(sendButton, state);
        }
        state.composerMode = composerMode;
        state.simSlot = simSlot;
    }

    /**
     * Hooked at the entry of SendButtonLayout's click listener setter:
     * - 15.5.31.11: j(View$OnClickListener, View$OnLongClickListener)V
     * - 16.3.21.1:  e(View$OnClickListener, View$OnLongClickListener)V
     */
    public static View.OnClickListener wrapSendClickListener(
            final View sendButton,
            final View.OnClickListener originalListener
    ) {
        if (sendButton == null || originalListener == null) {
            return originalListener;
        }

        return new View.OnClickListener() {
            @Override
            public void onClick(final View clickedView) {
                handleSendClick(sendButton, originalListener, clickedView);
            }
        };
    }

    private static void handleSendClick(
            final View sendButton,
            final View.OnClickListener originalListener,
            final View clickedView
    ) {
        Context context = sendButton.getContext();
        if (context == null) {
            originalListener.onClick(clickedView);
            return;
        }

        // 1. Check if guard is enabled in preferences
        if (!GuardPreferences.isGuardEnabled(context)) {
            originalListener.onClick(clickedView);
            return;
        }

        // 2. Check composer mode from captured state
        ButtonState state = BUTTON_STATES.get(sendButton);
        int composerMode = state != null ? state.composerMode : 1;
        int simSlot = state != null ? state.simSlot : 0;

        // RCS chat messages use IP data / Wi-Fi, not carrier SMS tariffs
        if (composerMode == 3) {
            originalListener.onClick(clickedView);
            return;
        }

        // 3. Resolve active SIM and telephony status
        SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        SubscriptionInfo subInfo = null;
        if (sm != null) {
            try {
                subInfo = sm.getActiveSubscriptionInfoForSimSlotIndex(simSlot);
            } catch (Throwable ignored) {}
        }

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (subInfo != null && tm != null) {
            try {
                tm = tm.createForSubscriptionId(subInfo.getSubscriptionId());
            } catch (Throwable ignored) {}
        }

        // Determine home country
        String homeCountry;
        String customHome = GuardPreferences.getCustomHomeCountry(context);
        if (customHome != null && !customHome.isEmpty()) {
            homeCountry = customHome;
        } else if (subInfo != null && subInfo.getCountryIso() != null && !subInfo.getCountryIso().isEmpty()) {
            homeCountry = subInfo.getCountryIso().toLowerCase(Locale.ROOT);
        } else if (tm != null && tm.getSimCountryIso() != null && !tm.getSimCountryIso().isEmpty()) {
            homeCountry = tm.getSimCountryIso().toLowerCase(Locale.ROOT);
        } else {
            homeCountry = Locale.getDefault().getCountry().toLowerCase(Locale.ROOT);
        }

        // Determine network country and roaming status
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

        int roamingMode = GuardPreferences.getRoamingMode(context);
        boolean includeUk = GuardPreferences.isIncludeUkInEu(context);

        // 4. Resolve recipient phone numbers
        List<String> recipients = RecipientResolver.resolveRecipients(sendButton);
        if (recipients.isEmpty()) {
            // Recipient could not be determined; allow user to proceed without blocking
            originalListener.onClick(clickedView);
            return;
        }

        // 5. Check if any recipient causes extra charges
        ChargeAssessment chargedAssessment = null;
        for (String recipient : recipients) {
            if (PhoneNumberHelper.isEmergency(context, recipient)) {
                // Emergency numbers are never questioned or delayed
                continue;
            }

            String destCountry = PhoneNumberHelper.extractDestinationCountry(recipient, homeCountry);
            ChargeAssessment assessment = assessCharge(
                    recipient, homeCountry, networkCountry, destCountry, isRoaming, roamingMode, includeUk
            );

            if (assessment.isCharged()) {
                chargedAssessment = assessment;
                break;
            }
        }

        // 6. Show confirmation dialog if charged, otherwise proceed immediately
        if (chargedAssessment != null) {
            Activity activity = RecipientResolver.findActivity(sendButton.getContext());
            if (activity != null) {
                final ChargeAssessment finalAssessment = chargedAssessment;
                ChargedSmsDialog.showConfirmationDialog(activity, finalAssessment, new Runnable() {
                    @Override
                    public void run() {
                        originalListener.onClick(clickedView);
                    }
                });
                return;
            }
        }

        originalListener.onClick(clickedView);
    }

    /**
     * Determines whether an SMS is charged based on roaming status and country prefixes.
     */
    public static ChargeAssessment assessCharge(
            String recipientNumber,
            String homeCountry,
            String networkCountry,
            String destCountry,
            boolean isRoaming,
            int roamingMode,
            boolean includeUk
    ) {
        // Case 1: NOT in roaming (at home)
        if (!isRoaming) {
            if (destCountry.equalsIgnoreCase(homeCountry)) {
                return ChargeAssessment.free(recipientNumber, destCountry, homeCountry, networkCountry);
            }
            // Destination does not match base country -> International SMS!
            return ChargeAssessment.charged(
                    ChargeAssessment.Reason.INTERNATIONAL_FROM_HOME,
                    recipientNumber, destCountry, homeCountry, networkCountry
            );
        }

        // Case 2: IN roaming
        if (roamingMode == GuardPreferences.MODE_ALL_ROAMING) {
            return ChargeAssessment.charged(
                    ChargeAssessment.Reason.ALL_ROAMING_POLICY,
                    recipientNumber, destCountry, homeCountry, networkCountry
            );
        }

        // EU Roam Like At Home (RLAH) policy
        boolean inEuRoaming = RoamingZone.isEuZone(networkCountry, includeUk);
        if (inEuRoaming) {
            boolean destInEu = RoamingZone.isEuZone(destCountry, includeUk);
            if (destInEu) {
                // Sending within EU while roaming in EU is covered by RLAH
                return ChargeAssessment.free(recipientNumber, destCountry, homeCountry, networkCountry);
            } else {
                // Roaming in EU, but destination is outside EU
                return ChargeAssessment.charged(
                    ChargeAssessment.Reason.ROAMING_DEST_OUTSIDE_EU,
                    recipientNumber, destCountry, homeCountry, networkCountry
                );
            }
        } else {
            // Roaming outside EU (e.g. Tunisia)
            return ChargeAssessment.charged(
                ChargeAssessment.Reason.ROAMING_OUTSIDE_EU,
                recipientNumber, destCountry, homeCountry, networkCountry
            );
        }
    }
}
