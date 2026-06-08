<p align="center">
	<img src="logo.png" width="376" height="128" alt="Winlator Logo" />
</p>

# Winlator

Winlator is an Android application that lets you to run Windows (x86_64) applications with Wine and Box86/Box64.

> **⚠️ This fork is currently under maintenance.**
>
> Do not download or attempt to use this build. It is not ready for testing and will not work properly until the first beta release.
>
> If you need a working Windows emulator for Android, use the [original Winlator](https://github.com/brunodev85/winlator) by brunodev85.

# Installation

1. Download and install the APK (Winlator_11.0.apk) from [GitHub Releases](https://github.com/brunodev85/winlator/releases)
2. Launch the app and wait for the installation process to finish

----


# Build APK with GitHub Actions

The repository contains a GitHub Actions workflow that assembles the Android APK automatically.

1. Push any branch to GitHub or open **Actions -> Build APK -> Run workflow**.
2. Select `debug` for a regular debug APK. Select `release` only after adding signing secrets.
3. After the workflow finishes, download the APK from the `winlator-fusion-*-apk` artifact.

The workflow installs JDK 17, Android SDK 34, NDK `24.0.8215888`, CMake `3.22.1`, native shader tools, downloads the required runtime assets, runs Gradle, and uploads the generated APK.

For a signed release APK, add these repository secrets before running the `release` variant:

- `RELEASE_KEYSTORE_BASE64` - base64-encoded Android keystore file.
- `RELEASE_STORE_PASSWORD` - keystore password.
- `RELEASE_KEY_ALIAS` - signing key alias.
- `RELEASE_KEY_PASSWORD` - signing key password.
- `STEAMGRID_API_KEY` - optional SteamGridDB API key used at build time.

----

[![Play on Youtube](https://img.youtube.com/vi/ETYDgKz4jBQ/3.jpg)](https://www.youtube.com/watch?v=ETYDgKz4jBQ)
[![Play on Youtube](https://img.youtube.com/vi/9E4wnKf2OsI/2.jpg)](https://www.youtube.com/watch?v=9E4wnKf2OsI)
[![Play on Youtube](https://img.youtube.com/vi/czEn4uT3Ja8/2.jpg)](https://www.youtube.com/watch?v=czEn4uT3Ja8)
[![Play on Youtube](https://img.youtube.com/vi/eD36nxfT_Z0/2.jpg)](https://www.youtube.com/watch?v=eD36nxfT_Z0)

----

# Useful Tips

- If you are experiencing performance issues, try changing the Box64 preset to `Performance` in Container Settings -> Advanced Tab.
- For applications that use .NET Framework, try installing `Wine Mono` found in Start Menu -> System Tools -> Installers.
- If some older games don't open, try adding the environment variable `MESA_EXTENSION_MAX_YEAR=2003` in Container Settings -> Environment Variables.
- Try running the games using the shortcut on the Winlator home screen, there you can define individual settings for each game.
- To display low resolution games correctly, try to enabling the `Force Fullscreen` option in the shortcut settings.
- To improve stability in games that uses Unity Engine, try changing the Box64 preset to `Stability` or in the shortcut settings add the exec argument `-force-gfx-direct`.

# Credits and Third-party apps

- GLIBC Patches by [Termux Pacman](https://github.com/termux-pacman/glibc-packages)
- Wine ([winehq.org](https://www.winehq.org/))
- Box86/Box64 by [ptitseb](https://github.com/ptitSeb)
- Mesa (Turnip/Zink/VirGL) ([mesa3d.org](https://www.mesa3d.org))
- DXVK ([github.com/doitsujin/dxvk](https://github.com/doitsujin/dxvk))
- VKD3D ([gitlab.winehq.org/wine/vkd3d](https://gitlab.winehq.org/wine/vkd3d))
- CNC Ddraw ([github.com/FunkyFr3sh/cnc-ddraw](https://github.com/FunkyFr3sh/cnc-ddraw))

Special thanks to all the developers involved in these projects.<br>
Thank you to all the people who believe in this project.
