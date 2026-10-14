# App-opening behavior

A selected app entering the foreground requires one mental math answer. Solving it grants access for that foreground session. Home, another app, and screen lock end the session; returning requires a new answer. Scrolling, activity changes within the same package, keyboards, and notification overlays do not start another challenge.

Difficulty is shared across selected apps and saved in DataStore. Each opening advances the next level up to 15. Abandoned challenges count as openings. Leaving all selected apps or locking the screen starts recovery: each full interval away drops the next level by one, down to the starting difficulty. The Settings slider chooses 5–120 minutes per level in 5-minute steps (default 30 minutes). Returning consumes the accumulated recovery and begins a new foreground session; continuous use and answering a challenge do not count as time away. Changing the slider applies its interval to the current time-away period. Unlocks deliberately do not survive service or process restarts.

Back and Return to home abandon the challenge without granting access.

## Local setup

In this Windows workspace, portable Java and the Android SDK are installed in `../.tools`. Run `./build-local.ps1` in PowerShell to rebuild the APK, run unit/screenshot tests, and check Kotlin formatting with these tools. The script does not install anything or modify global settings.

Install Java 17 and Android SDK platform 35 / build-tools 35.0.0. Set JAVA_HOME and sdk.dir in local.properties (or ANDROID_HOME), then run:

```
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:ktlintCheck
./gradlew :app:recordPaparazziDebug
./gradlew :app:verifyPaparazziDebug
```

## Device checks

Install the debug APK, enable the required permissions, and select two apps. When upgrading, disable and re-enable PlugBrain accessibility to refresh its window configuration.

1. Open the first app, solve, and scroll longer than the old interval: no second prompt.
2. Visit Home and reopen: a harder challenge.
3. Switch to the other selected app: another challenge.
4. Show the keyboard, switch tabs, and open notification shade: no new challenge after solving.
5. Lock and unlock: the selected app requires another challenge.
6. Enter a wrong answer or nonnumeric input: inline error; access stays blocked.
7. Abandon with Back, reopen, and restart the service: access still requires a challenge.
8. Repeat to level 15: challenges continue without exceeding the supported level.
9. Set recovery to 5 minutes, leave all selected apps for 10 minutes, and return: the next challenge is two levels lower, bounded by starting difficulty. Scroll for 10 minutes inside a selected app: no recovery or timed interruption.

Accessibility window delivery and background activity launch behavior vary by Android version and manufacturer. These device checks supplement JVM and rendered UI tests.
