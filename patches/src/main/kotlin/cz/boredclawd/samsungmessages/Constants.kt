package cz.boredclawd.samsungmessages

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_SAMSUNG_MESSAGES = Compatibility(
        name = "Samsung Messages",
        packageName = "com.samsung.android.messaging",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x20AFFF,
        targets = listOf(
            AppTarget(version = "15.5.31.11", minSdk = 31),
            AppTarget(version = "16.3.21.1", minSdk = 33),
        ),
    )
}
