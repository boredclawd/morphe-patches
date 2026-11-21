package cz.boredclawd.samsungmessages

import app.morphe.patcher.Fingerprint

/**
 * Resolves whether long messages should be converted to MMS after reaching the page count threshold.
 * Returning false disables conversion to MMS, keeping messages as multi-part SMS.
 */
internal object SmsToMmsByThresholdFingerprint : Fingerprint(
    definingClass = "Lcom/samsung/android/messaging/common/configuration/Feature;",
    name = "getSmsToMmsByThreshold",
    returnType = "Z",
    parameters = listOf("I", "I"),
)

/**
 * Returns the maximum SMS segment page count before conversion or character warning.
 * Setting this to a high value (e.g. 999) prevents the composer from showing character limit alerts.
 */
internal object SmsMaxPageCountFingerprint : Fingerprint(
    definingClass = "Lcom/samsung/android/messaging/common/setting/Setting;",
    name = "getSmsMaxPageCount",
    returnType = "I",
    parameters = listOf("I", "I"),
)
