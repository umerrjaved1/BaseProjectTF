# Product Factory: New App Checklist

This document outlines the standard operating procedure (SOP) for spinning up a new application from this base Product Factory. Follow these steps sequentially whenever you want to create a new app from the list (e.g., Voice Changer, Live Earth, etc.).

## 1. Generate the New App Codebase
Use the included Python automation script to clone the base project and automatically rename the package and app name.

> [!IMPORTANT]
> Run this command from the root of the base project directory. Make sure you use the `--dest` flag so it creates a new folder for the new app, leaving your base project intact.

```bash
python generate_app.py --name "Your New App Name" --package "com.tf.yournewapp" --dest "../YourNewAppFolder"
```

## 2. Firebase Configuration
The new app needs its own Firebase configuration for Crashlytics, Analytics, and other services.

- [ ] Go to the Firebase Console.
- [ ] Create a new project (or add a new Android App to an existing project).
- [ ] Enter your new package name (`com.tf.yournewapp`).
- [ ] Download the `google-services.json` file.
- [ ] Replace the existing `app/google-services.json` in your **new** project directory with the downloaded file.

## 3. Branding & Assets
Update the visual identity of the app so it doesn't look like the base project.

- [ ] **App Icon:** Replace the launcher icons in `app/src/main/res/mipmap-*`. You can use Android Studio's **Image Asset Studio** (File > New > Image Asset) to generate these easily.
- [ ] **Splash Screen:** Update any splash screen logos or animations located in `app/src/main/res/drawable` or `core/designsystem`.
- [ ] **Theming:** Open `core/designsystem/src/main/java/.../Theme.kt` and `Color.kt` to define the primary, secondary, and background colors specific to this new app.

## 4. Monetization (Ads)
If the app uses ads, you must update the IDs to ensure revenue is tracked correctly.

- [ ] **App ID:** Update the AdMob/AppLovin App ID in `app/src/main/AndroidManifest.xml` (look for `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" ... />`).
- [ ] **Ad Units:** Update your Banner, Interstitial, Native, and App Open Ad Unit IDs in your Ad utility classes (e.g., `AdUtils.kt` or `RemoteConfig`).

## 5. Deployment & CI/CD (Fastlane / GitHub Actions)
Ensure your automated deployment pipelines point to the correct Google Play Console entries.

- [ ] **Fastlane Appfile:** If you use an `Appfile`, update `package_name` to your new package name.
- [ ] **Keystore / Signing:** Generate a new release Keystore for the app, or use your company's shared Keystore. Update the `signingConfigs` in `app/build.gradle.kts`.
- [ ] **GitHub Secrets:** If you push this new app to a new GitHub repository, remember to set up the necessary Action Secrets (e.g., `PLAY_STORE_CREDENTIALS`, `KEYSTORE_FILE`) for `.github/workflows/android.yml` to work.

## 6. Review App-Specific Features
Once the boilerplate is out of the way, you can start building the unique features!

- [ ] Strip out any feature code from the base project that isn't needed in the new app (if applicable).
- [ ] Implement the core logic and UI for the new product.
