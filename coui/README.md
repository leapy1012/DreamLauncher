# COUI AppCompat

Share **one AAR file**. Other Gradle projects copy it into `libs/` — no GitHub token, no `mavenLocal()`.

Built artifact (after `exportCouiAar`):

```text
coui/dist/coui-1.0.0.aar
```

Give that file (about 4 MB) to other users — USB, chat, or drop it into each app’s `libs/`. `coui/dist/` is a local build output, not committed. AOSP apps should still use source `static_libs: ["coui-appcompat"]`, not the AAR.

Optional: tag `coui-v1.0.0` and push. GitHub Actions attaches the AAR to a public Release so others can download it without a token.

## Build the AAR (once, on this machine)

From the DreamLauncher root:

```powershell
.\gradlew.bat :coui:exportCouiAar
```

If there is no wrapper:

```powershell
gradle :coui:exportCouiAar
```

Output: `coui/dist/coui-1.0.0.aar`. Bump `COUI_VERSION` in `gradle.properties` when you cut a new file.

## Use in any other Gradle project

1. Copy `coui-1.0.0.aar` into that app’s `app/libs/` (create the folder if needed).

2. `app/build.gradle`:

```gradle
android {
    compileSdk 34
    defaultConfig {
        minSdk 28
    }
}

dependencies {
    implementation files("libs/coui-1.0.0.aar")

    // A raw AAR does not pull Maven transitives. Add these in every consumer:
    implementation "androidx.appcompat:appcompat:1.7.0"
    implementation "androidx.core:core:1.13.0"
    implementation "androidx.collection:collection:1.3.0"
    implementation "androidx.fragment:fragment:1.6.2"
    implementation "androidx.constraintlayout:constraintlayout:2.2.0-alpha13"
    implementation "androidx.dynamicanimation:dynamicanimation:1.1.0"
    implementation "androidx.preference:preference:1.2.1"
    implementation "androidx.recyclerview:recyclerview:1.2.1"
    implementation "androidx.viewpager:viewpager:1.0.0"
    implementation "androidx.viewpager2:viewpager2:1.0.0"
    implementation "com.google.android.material:material:1.7.0-alpha03"
    implementation "com.airbnb.android:lottie:6.0.0"
}
```

`google()` + `mavenCentral()` must be in the consumer’s repositories (for those AndroidX/Lottie lines). Rebound is already inside the AAR.

3. Theme — required or widgets look like stock AppCompat:

```xml
<application android:theme="@style/Theme.COUI">
    <activity
        android:name=".MainActivity"
        android:theme="@style/Theme.COUI.Blue" />
</application>
```

Preferences: `preferenceTheme` = `@style/PreferenceThemeOverlay.COUITheme`.

## Several apps in one company

Keep a single copy, for example `\\share\android\coui-1.0.0.aar` or a `libs/` folder in a small “sdk” git repo. Each app points `implementation files(...)` at that path (or copies the file into its own `libs/` so the app still builds offline).

When you ship 1.0.1, replace the file and bump the filename in every `build.gradle`.

## AOSP

```bp
static_libs: ["coui-appcompat"],
```

Same `coui/` sources. Do not import the Gradle AAR into Soong.
