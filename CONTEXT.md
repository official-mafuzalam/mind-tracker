# Mind Tracker — Project Context & AI Guidelines

Welcome to **Mind Tracker** (`com.octosync.mindtracker`). This document serves as the primary context file for AI assistants (like Claude, Gemini, GPT, Cursor, etc.) and developers working on this codebase.

---

## 1. Project Overview & Specs

- **Application Name**: Mind Tracker
- **Package Name**: `com.octosync.mindtracker`
- **Primary Language**: Java 11 (Source & Target compatibility)
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 36 (Android 16)
- **Compile SDK**: 36 (Android 16)
- **Gradle Version**: 9.0 (Gradle Wrapper)
- **Android Gradle Plugin (AGP)**: 8.10.1
- **Dependency Management**: Centralized Version Catalog (`gradle/libs.versions.toml`)

---

## 2. Tech Stack & Dependencies

- **UI Framework**: Native Android XML Views with Material Components (`com.google.android.material:material:1.13.0`).
- **Navigation & Layouts**: `ViewPager`, `TabLayout`, `ConstraintLayout`, `RecyclerView`, `ChipGroup` / `Chip`.
- **Data Visualization**: `MPAndroidChart` (`com.github.PhilJay:MPAndroidChart:v3.1.0`) for `PieChart` (mood breakdown) and `BarChart` (daily mood trends).
- **Background Tasks**: `androidx.work:work-runtime:2.10.0` (`WorkManager`) for customizable daily mood logging reminder notifications.
- **Biometric Security**: `androidx.biometric:biometric:1.1.0` for Fingerprint / Face Unlock / PIN App Lock.
- **Widgets**: Native Android App Widget (`MoodWidget` & `AppWidgetProvider`).
- **Data Persistence**: `SharedPreferences` for mood entries, notes, tags, notification settings, and widget states.
- **Web Browsing**: `androidx.browser:browser:1.8.0` (`CustomTabsIntent`) for external links (privacy policy, support).

---

## 3. Architecture & File Structure

```
mind-tracker/
├── app/
│   ├── build.gradle                   # Module-level build configuration (SDKs, R8, resConfigs)
│   ├── proguard-rules.pro             # ProGuard / R8 keep rules (including MPAndroidChart rules)
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml    # App manifest (Activities, Receiver, Permissions)
│           ├── java/com/octosync/mindtracker/
│           │   ├── MainActivity.java        # Host activity with ViewPager, App Lock, TimePicker, Export
│           │   ├── MainPagerAdapter.java    # ViewPager adapter for tab fragments
│           │   ├── MoodFragment.java        # Tab 1: Log daily mood, activity tags, and notes
│           │   ├── InsightsFragment.java    # Tab 2: History log (dates, moods, tags, notes, streak)
│           │   ├── StatsFragment.java       # Tab 3: Analytics (PieChart & BarChart)
│           │   ├── MoodButton.java          # Custom animated MaterialButton for mood selection
│           │   ├── MoodReminderWorker.java  # WorkManager daily reminder notification worker
│           │   └── MoodWidget.java          # App Widget provider for home screen interactions
│           └── res/
│               ├── layout/                  # Layout XMLs (activity_main, fragment_*, item_mood_history)
│               ├── menu/                    # Action bar options menu (main_menu.xml)
│               ├── values/                  # Colors, dimensions, themes, strings
│               ├── color/ & color-night/    # State color lists (light & dark mode support)
│               └── xml/                     # Widget info, backup rules, data extraction rules
├── gradle/
│   └── libs.versions.toml             # Version Catalog (AGP, AndroidX, MPAndroidChart, Biometric)
├── build.gradle                       # Top-level build configuration
├── settings.gradle                    # Project settings & plugin/repository definitions
└── CONTEXT.md                         # Primary AI context & developer guidance file
```

---

## 4. Key Workflows & Features

1. **Logging Moods, Notes & Activity Tags (`MoodFragment.java`)**:
   - User selects a mood emoji: `Happy`, `Neutral`, `Sad`, `Angry`, `Tired`.
   - Selects activity/context tags (`Work`, `Exercise`, `Sleep`, `Social`, `Health`, `Relax`).
   - Inputs optional daily journal note.
   - Saves timestamps, mood, note, and tags into `SharedPreferences`.
   - Triggers `AppWidgetManager` broadcast to update `MoodWidget`.

2. **Viewing History & Insights (`InsightsFragment.java`)**:
   - Reads recorded entries, notes, and activity tags from `SharedPreferences`.
   - Displays formatted history list in a `RecyclerView`.
   - Calculates logging streaks and most frequent mood states.

3. **Analytics & Charts (`StatsFragment.java`)**:
   - Computes mood frequency and distributions for current week/month.
   - Renders interactive `PieChart` and `BarChart` using MPAndroidChart.

4. **Customizable Daily Reminders (`MoodReminderWorker.java` & `MainActivity.java`)**:
   - Allows users to configure their exact preferred daily notification time via `TimePickerDialog`.
   - Schedules periodic notification via WorkManager (`ExistingPeriodicWorkPolicy.REPLACE`).
   - Handles `POST_NOTIFICATIONS` runtime permission check for Android 13+ (API 33+).

5. **Biometric & Passcode App Lock (`MainActivity.java`)**:
   - Protects private mood records using `BiometricPrompt` (Fingerprint, Face Unlock, or Device PIN).
   - Toggleable via Options Menu.

6. **Data Export & Report Summary (`MainActivity.java`)**:
   - Export mood logs as a structured **CSV file** (`Intent.ACTION_CREATE_DOCUMENT`).
   - Share as a formatted **Text Summary Report** (`Intent.ACTION_SEND` share sheet) for doctors, therapists, or personal archives.

7. **App Widget (`MoodWidget.java`)**:
   - Home screen widget displaying current mood status and quick action shortcut.

---

## 5. Development & AI Best Practices

When modifying code or creating new features in this project, AI models and developers must adhere to the following rules:

### A. Code Style & Compatibility
- Write **clean, idiomatic Java** targeting Java 11 compatibility.
- Use explicit nullability annotations (`@NonNull`, `@Nullable`) on fragment lifecycles and parameter methods.
- Handle Edge-to-Edge window insets cleanly in activities (`WindowCompat.setDecorFitsSystemWindows`).

### B. Size Optimization & Resources
- Keep `resourceConfigurations += ["en"]` in `app/build.gradle` to prevent pulling in unused localization string tables from libraries.
- Prefer **Vector Drawables** (`<vector>`) over raster images where possible.

### C. ProGuard / R8 Rules
- Maintain keep rules in `app/proguard-rules.pro` whenever adding custom UI libraries, charts, or reflection-based dependencies.
- Verify release builds with `./gradlew app:assembleRelease` and `./gradlew app:bundleRelease`.

### D. Verification Commands
Always verify changes using Gradle tasks:
- **Build App Bundle**: `./gradlew app:bundleRelease`
- **Build Debug APK**: `./gradlew app:assembleDebug`
- **Run Unit Tests**: `./gradlew test`
- **Sync Project**: `./gradlew --refresh-dependencies`
