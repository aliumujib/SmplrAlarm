### Modernize Project: Compose UI, Kotlin DSL, Version Catalogs & Critical Bug Fixes

Hello!

I recently discovered `SmplrAlarm` while looking for a robust alarm library for a personal project and was immediately impressed with its functionality. As I started to integrate it, I ran into a few issues with the older build system and some deprecated practices that could lead to silent failures.

I ended up spending about four hours modernizing the library and the sample app, and I'd love to contribute these changes back to the project. My goal was to make the project easier to maintain, more resilient, and aligned with current Android development best practices.

Here is a summary of the changes in this pull request:

---

#### **Key Changes & Modernization**

##### 1. Complete UI Migration to Jetpack Compose
- The entire sample app UI has been rewritten from XML layouts and Fragments to **Jetpack Compose**.
- This removes a significant amount of boilerplate code, gets rid of `ViewBinding`, and adopts a modern, declarative UI approach.
- The app now uses a single `Activity` with Composable screens, and all legacy XML layout files have been deleted.

##### 2. Gradle Build System Overhaul
- **Kotlin DSL:** All `build.gradle` files have been migrated to the Kotlin DSL (`build.gradle.kts`), providing better type safety and editor support.
- **Version Catalogs:** All dependencies are now managed through a TOML version catalog (`libs.versions.toml`), centralizing version management and making dependency updates much cleaner.
- **Dependency Updates:** All dependencies, including the Android Gradle Plugin, Kotlin, and various AndroidX libraries, have been updated to their latest stable versions.

##### 3. Dependency Modernization & Bug Fixes
- **Moshi to `kotlinx.serialization`:** The project now uses `kotlinx.serialization` for JSON parsing, removing the dependency on Moshi and its associated Kapt plugin. This aligns with modern, multiplatform Kotlin projects.
- **Silent Notification Failures:** Addressed a critical issue where incorrectly configured notification items would **fail silently** during channel or notification creation. The system now provides better validation and error handling to prevent these hard-to-debug issues.
- **Edge-to-Edge UI:** The sample app now correctly handles edge-to-edge displays by using safe drawing insets for a modern look and feel.

---

I believe these changes significantly improve the project's architecture and make it a great example of modern Android development. I hope you find these contributions valuable and would be happy to discuss them further.

Thank you for creating such a great library!
