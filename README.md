## Appboy Kit Integration

This repository contains the [Appboy](https://www.appboy.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. The Appboy Kit requires that you add Appboy's Maven server to your buildscript:

    ```
    repositories {
        maven { url "http://appboy.github.io/appboy-android-sdk/sdk" }
        //Appboy's library depends on the Google Support Library, which is now distributed via Maven
        maven { url "https://maven.google.com" }
        ...
    }
    ```

2. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        compile 'com.mparticle:android-appboy-kit:5+'
    }
    ```

3. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Appboy detected"` in the output of `adb logcat`.
4. Reference mParticle's integration docs below to enable the integration.

### Documentation

[Appboy integration](http://docs.mparticle.com/?java#appboy)

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)