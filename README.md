## Appboy Kit Integration

This repository contains the [Appboy](https://www.appboy.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk). 

### Adding the integration

1. [Enable the integration](https://app.mparticle.com/providers) for your mParticle app.
2. Add the kit dependency to your project. Kits are all made available via Maven Central, you can add them to your project just by adding their maven artifact (ex. `com.mparticle:android-<integration-name>-kit`). [See here for the current list of all kits](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22).
3. If you haven't already added it, the core mParticle Android SDK will automatically be pulled in as a dependency of the kit. Follow the quick start of the mParticle Core SDK, then re-build and launch your app, and verify that you see `"<Integration Name> detected"` in the output of `adb logcat`.

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
