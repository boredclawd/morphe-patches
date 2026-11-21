package cz.boredclawd.samsungmessages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import cz.boredclawd.samsungmessages.Constants.COMPATIBILITY_SAMSUNG_MESSAGES

internal const val SAMSUNG_MESSAGES_EXTENSION = "extensions/samsungmessages.mpe"

/**
 * Intercepts send button clicks in Samsung Messages to warn the user before sending
 * an SMS that will incur extra carrier charges (international SMS or roaming outside EU/RLAH).
 */
@Suppress("unused")
val chargedSmsAlertPatch = bytecodePatch(
    name = "Charged SMS and roaming confirmation alert",
    description = "Shows a One UI confirmation dialog before sending an SMS that will incur extra charges (international SMS or roaming outside EU/RLAH).",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SAMSUNG_MESSAGES)
    extendWith(SAMSUNG_MESSAGES_EXTENSION)

    execute {
        val sendButtonClass = mutableClassDefBy("Lcom/samsung/android/messaging/ui/view/composer/messageeditor/component/sendbutton/SendButtonLayout;")

        // Hook state updates: b(IIZZZ[Z[Z)V in 15.5.31.11, a(IIZZZ[Z[Z)V in 16.3.21.1
        val updateStateMethod = sendButtonClass.methods.single {
            it.returnType == "V" && it.parameterTypes.map(CharSequence::toString) == listOf("I", "I", "Z", "Z", "Z", "[Z", "[Z")
        }
        updateStateMethod.addInstructions(
            0,
            """
                invoke-static { p0, p1, p2 }, Lcz/boredclawd/samsungmessages/ChargedSmsGuard;->onUpdateState(Landroid/view/View;II)V
            """,
        )

        // Hook click listener setter: j(View$OnClickListener, View$OnLongClickListener)V in 15.5.31.11, e(...)V in 16.3.21.1
        val setClickListenerMethod = sendButtonClass.methods.single {
            it.returnType == "V" && it.parameterTypes.map(CharSequence::toString) == listOf(
                "Landroid/view/View\$OnClickListener;",
                "Landroid/view/View\$OnLongClickListener;",
            )
        }
        setClickListenerMethod.addInstructions(
            0,
            """
                invoke-static { p0, p1 }, Lcz/boredclawd/samsungmessages/ChargedSmsGuard;->wrapSendClickListener(Landroid/view/View;Landroid/view/View${'$'}OnClickListener;)Landroid/view/View${'$'}OnClickListener;
                move-result-object p1
            """,
        )

        // Hook NotificationActionService to intercept notification inline replies (RemoteInput)
        val notificationServiceClass = mutableClassDefByOrNull("Lcom/samsung/android/messaging/ui/service/notification/NotificationActionService;")
        val onHandleIntentMethod = notificationServiceClass?.methods?.singleOrNull {
            it.name == "onHandleIntent" && it.returnType == "V" && it.parameterTypes.map(CharSequence::toString) == listOf("Landroid/content/Intent;")
        }
        onHandleIntentMethod?.addInstructions(
            0,
            """
                invoke-static { p0, p1 }, Lcz/boredclawd/samsungmessages/NotificationReplyGuard;->shouldHandleIntent(Landroid/app/Service;Landroid/content/Intent;)Z
                move-result v0
                if-nez v0, :cond_continue_send
                return-void
                :cond_continue_send
            """,
        )
    }
}
