extension {
    name = "extensions/samsungmessages.mpe"
}

android {
    namespace = "cz.boredclawd.samsungmessages"

    defaultConfig {
        minSdk = 31
    }
}

// Plain Java, like the other extensions: keep the Kotlin stdlib out of the dex.
configurations.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains", module = "annotations")
}
