package cz.boredclawd.samsungmessages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import cz.boredclawd.samsungmessages.Constants.COMPATIBILITY_SAMSUNG_MESSAGES

/**
 * Prevents Samsung Messages from automatically converting long text messages to MMS
 * when the character or page count threshold (default 3 segments) is exceeded.
 */
@Suppress("unused")
val disableSmsToMmsConversionPatch = bytecodePatch(
    name = "Disable SMS to MMS conversion",
    description = "Sends long text messages as multi-part SMS instead of automatically converting them to MMS.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SAMSUNG_MESSAGES)

    execute {
        SmsToMmsByThresholdFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        SmsMaxPageCountFingerprint.method.addInstructions(
            0,
            """
                const/16 v0, 0x3e7
                return v0
            """,
        )
    }
}
