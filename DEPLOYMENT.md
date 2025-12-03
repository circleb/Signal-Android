# Google Play Console Deployment Guide

This guide will help you build and deploy the Signal Android app to Google Play Console.

## Important: Package Name

**This app uses a custom package name**: `org.homesteadheritage.hcp` (instead of the original `org.thoughtcrime.securesms`) to avoid conflicts with the official Signal app on Google Play. This change has been made in:

- Application ID in `build.gradle.kts`
- Content provider authorities (automatically use `${applicationId}`)
- App authentication redirect scheme
- MIME types and manifest references

If you need to change the package name, edit `applicationId` in `app/build.gradle.kts`.

## Prerequisites

1. **Google Play Developer Account**: You need a Google Play Developer account ($25 one-time fee)
2. **Java Development Kit (JDK)**: Ensure you have JDK installed
3. **Android SDK**: Make sure Android SDK is properly configured
4. **Release Keystore**: You'll need a signing key for release builds

## Step 1: Create a Release Keystore

If you don't have a release keystore yet, create one:

```bash
keytool -genkey -v -keystore release-key.keystore -alias release-key -keyalg RSA -keysize 2048 -validity 10000
```

**Important**:

- Store the keystore file in a secure location (outside the project directory)
- Remember the passwords and alias name - you'll need them for all future updates
- If you lose the keystore, you cannot update your app on Google Play

## Step 2: Configure Release Signing

1. Copy the template file:

   ```bash
   cp keystore.release.properties.template keystore.release.properties
   ```

2. Edit `keystore.release.properties` and fill in your keystore details:

   ```properties
   storeFile=path/to/your/release-key.keystore
   storePassword=your-store-password
   keyAlias=your-key-alias
   keyPassword=your-key-password
   ```

   **Note**: Use an absolute path or a path relative to the project root for `storeFile`.

3. Verify the file is in `.gitignore` (it should be already)

## Step 3: Update Version Information (Optional)

If you want to change the version before building, edit `app/build.gradle.kts`:

```kotlin
val canonicalVersionCode = 1623  // Increment for each release
val canonicalVersionName = "7.66.3"  // Update version name
val currentHotfixVersion = 0  // Use for hotfixes (0-99)
```

**Important**: Each release must have a higher `versionCode` than the previous one.

## Step 4: Set Up Java 17 (if needed)

This project requires Java 17. If you don't have it set up:

1. **Install Java 17** (if not already installed):

   ```bash
   brew install openjdk@17
   ```

2. **Use the Java 17 wrapper script** (recommended):

   ```bash
   ./gradlew-java17 bundlePlayProdRelease
   ```

   Or set JAVA_HOME permanently in your `~/.zshrc`:

   ```bash
   echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home' >> ~/.zshrc
   source ~/.zshrc
   ```

## Step 5: Build the Android App Bundle (AAB)

Google Play requires Android App Bundles (AAB) for new apps. Build the release AAB:

```bash
./gradlew-java17 bundlePlayProdRelease
```

(Or use `./gradlew` if you've set JAVA_HOME permanently)

The AAB file will be generated at:

```
app/build/outputs/bundle/playProdRelease/app-play-prod-release.aab
```

### Alternative: Build APK (for testing)

If you need an APK for testing (not for Play Store upload):

```bash
./gradlew-java17 assemblePlayProdRelease
```

The APK will be at:

```
app/build/outputs/apk/playProdRelease/app-play-prod-release-7.66.3.apk
```

## Step 6: Test Your Build

Before uploading to Google Play, test the release build:

1. Install the APK on a test device:

   ```bash
   adb install app/build/outputs/apk/playProdRelease/app-play-prod-release-7.66.3.apk
   ```

2. Test all critical functionality

## Step 7: Privacy Policy (Required)

**IMPORTANT**: Google Play requires a privacy policy URL for apps that use sensitive permissions. This app uses the following permissions that require a privacy policy:

- `android.permission.CAMERA` - For taking photos and videos
- `android.permission.RECORD_AUDIO` - For voice messages and calls
- `android.permission.ACCESS_FINE_LOCATION` - For location sharing
- `android.permission.READ_CONTACTS` - For contact integration
- `android.permission.READ_PHONE_STATE` - For phone number verification

**Before uploading**, you must:

1. Create a privacy policy that explains:

   - What data you collect
   - How you use the data
   - How you store and protect the data
   - User rights regarding their data

2. Host the privacy policy on a publicly accessible URL

   **Privacy Policy URL**: `https://my.homesteadheritage.org/privacy.html`

3. Add the privacy policy URL in Google Play Console:
   - Go to your app in Play Console
   - Navigate to **Policy** → **App content** → **Privacy Policy**
   - Enter your privacy policy URL: `https://my.homesteadheritage.org/privacy.html`

**Without a privacy policy URL, Google Play will reject your app upload.**

**Note**: Make sure the privacy policy page is publicly accessible (no login required) and returns a valid HTML page with HTTP 200 status code.

## Step 8: Upload to Google Play Console

1. **Sign in to Google Play Console**: https://play.google.com/console

2. **Select your app** (or create a new one)

3. **Go to Production** (or Internal Testing / Closed Testing / Open Testing)

4. **Create a new release**:

   - Click "Create new release"
   - Upload your AAB file (`app-play-prod-release.aab`)
   - Add release notes
   - Review the release

5. **Review and Rollout**:
   - Review all sections (Content rating, App content, etc.)
   - Once everything is approved, click "Start rollout to Production"

## Step 9: Monitor the Release

- Check the release status in Google Play Console
- Monitor crash reports and user feedback
- Track rollout progress

## Troubleshooting

### Build Fails with "Keystore file not found"

- Verify the path in `keystore.release.properties` is correct
- Use absolute paths or paths relative to project root

### "Signing config not found" error

- Ensure `keystore.release.properties` exists and is properly formatted
- Check that all required properties are set

### Version Code Error

- Each new release must have a higher version code than the previous one
- The version code is calculated as: `(canonicalVersionCode * 100) + currentHotfixVersion`
- To increment for a new release:
  - **For hotfixes**: Increment `currentHotfixVersion` (0-99) in `build.gradle.kts`
  - **For major releases**: Increment `canonicalVersionCode` in `build.gradle.kts`
- Example: Current version code is 162301 (canonicalVersionCode=1623, currentHotfixVersion=1)

### Google Play Rejects the Upload

- Ensure you're uploading an AAB, not an APK (for new apps)
- Check that the app is properly signed
- Verify all required metadata is filled in Play Console

## Using Google Play App Signing (Recommended)

Google Play App Signing is recommended for security:

1. **First Upload**: Upload your AAB signed with your upload key
2. **Google Generates App Signing Key**: Google will generate and manage the final signing key
3. **Future Uploads**: You can use the same upload key, or enroll in Play App Signing to use a different upload key

To enroll in Play App Signing:

1. Upload your first release
2. Go to Release > Setup > App signing
3. Follow the enrollment process

## Additional Resources

- [Google Play Console Help](https://support.google.com/googleplay/android-developer)
- [Android App Bundle Guide](https://developer.android.com/guide/app-bundle)
- [App Signing Best Practices](https://developer.android.com/studio/publish/app-signing)

## Security Notes

- **Never commit** `keystore.release.properties` or your keystore file to version control
- Store keystore backups in a secure location (encrypted storage, password manager)
- Consider using Google Play App Signing for additional security
- Keep your keystore passwords secure and backed up
