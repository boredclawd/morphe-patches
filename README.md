# 🧩 boredclawd's Morphe Patches

Custom Morphe patches for Samsung apps.

## ❓ About

Patches for Samsung Messages:
- **Disable SMS to MMS conversion**: Prevents automatic conversion of long text messages to MMS, sending them as multi-part SMS instead.
- **Charged SMS and roaming confirmation alert**: Shows a One UI confirmation dialog before sending an SMS that will incur extra charges (international SMS or roaming outside EU/RLAH), with Proceed/Cancel options and in-app configuration. Supports English, Czech, and Slovak.

### How to use these patches

Click the button below, or manually add this repo url to Morphe Manager: https://github.com/boredclawd/morphe-patches

<a href="https://morphe.software/add-source?github=boredclawd/morphe-patches">
  <img src=".github/assets/add-to-morphe.svg" alt="Add to Morphe" height="48">
</a>

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.10](https://github.com/boredclawd/morphe-patches/releases/tag/v1.0.0-dev.10)**&nbsp;&nbsp;&middot;&nbsp;&nbsp;`dev`&nbsp;&nbsp;&middot;&nbsp;&nbsp;**3 patches** across **1 apps**&nbsp;&nbsp;&middot;&nbsp;&nbsp;[Full details](PATCHES.md)

| # | App | Patches | Version | Package |
|---|---|---|---|---|
| 1 | [**Samsung Messages**](PATCHES.md#samsung-messages-comsamsungandroidmessaging) | 3 | `15.5.31.11`, `16.3.21.1` | [`com.samsung.android.messaging`](https://play.google.com/store/apps/details?id=com.samsung.android.messaging) |
<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

boredclawd patches are licensed under the [GNU General Public License v3.0](LICENSE)
