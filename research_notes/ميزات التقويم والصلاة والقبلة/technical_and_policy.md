# Technical approach and Google Play policy constraints: Hijri/Gregorian calendar, prayer times with adhan alerts, and Qibla in Speed Contact (state as of November 2026)

Context assumed throughout: native Kotlin app, minSdk 26, targetSdk 36, compileSdk 37, Compose screens, plain RemoteViews widgets, WorkManager, no INTERNET permission, only READ_CONTACTS and CALL_PHONE today, personal developer account in a closed test, owner's phone is Huawei or Honor.

## 1. Offline prayer-time calculation libraries

### Takeaway
The batoulapps Adhan library is still the standard choice. Its Kotlin Multiplatform artifact `com.batoulapps.adhan:adhan2` (latest 0.0.7, published June 2026) is MIT licensed, has the 12 standard methods (Umm al-Qura included), 3 high-latitude rules, Shafi/Hanafi Asr, per-prayer minute adjustments and a Qibla helper. The pure-Java `adhan` artifact (1.2.1, March 2021) is effectively frozen. PrayTimes.org is the main alternative and has been MIT licensed since v3.0.0 (March 2025), but it is JavaScript.

### Cited Findings
- The Adhan GitHub repo (batoulapps/adhan-kotlin; the adhan-java URL serves the same README) is MIT licensed. It says "All primary development is for the Kotlin Multiplatform version" and keeps "a branch for a pure Java version". — [GitHub adhan-kotlin](https://github.com/batoulapps/adhan-kotlin); [GitHub adhan-java](https://github.com/batoulapps/adhan-java)
- Gradle coordinates in the README are `implementation("com.batoulapps.adhan:adhan2:0.0.7")`. — [GitHub adhan-kotlin](https://github.com/batoulapps/adhan-kotlin)
- On Maven Central, adhan2 has versions 0.0.1 to 0.0.7. Latest and release are both 0.0.7, and lastUpdated is 20260613193315 (2026-06-13). — [Maven Central metadata adhan2](https://repo1.maven.org/maven2/com/batoulapps/adhan/adhan2/maven-metadata.xml)
- The legacy Java artifact `com.batoulapps.adhan:adhan` has only 1.2.0 and 1.2.1, last updated 2021-03-06. Mark it as **old/frozen**. — [Maven Central metadata adhan](https://repo1.maven.org/maven2/com/batoulapps/adhan/adhan/maven-metadata.xml)
- Maven Central publishes adhan2 for many targets: adhan2-jvm, -js, -wasm-js, iOS, macOS, watchOS, linux, mingw. No separate "adhan2-android" artifact is listed. — [Maven Central directory](https://repo1.maven.org/maven2/com/batoulapps/adhan/)
- Android requirement, quoted from the README: "on Android, kotlinx.datetime uses `java.time`, which needs either a minimum api level of 26, or enabling of `coreLibraryDesugaring`". Speed Contact's minSdk 26 meets this without desugaring. — [adhan-kotlin README](https://raw.githubusercontent.com/batoulapps/adhan-kotlin/main/README.md)
- Calculation methods (Fajr/Isha):
  - MUSLIM_WORLD_LEAGUE 18/17
  - EGYPTIAN 19.5/17.5
  - KARACHI 18/18
  - UMM_AL_QURA: Fajr 18.5, Isha a fixed 90-minute interval
  - DUBAI 18.2/18.2
  - QATAR: Fajr 18, Isha 90 minutes
  - KUWAIT 18/17.5
  - MOON_SIGHTING_COMMITTEE 18/18 plus seasonal adjustments
  - SINGAPORE 20/18
  - NORTH_AMERICA (ISNA) 15/15, which the README marks "not recommended"
  - TURKEY 18/17
  - OTHER 0/0

  — [GitHub adhan-kotlin](https://github.com/batoulapps/adhan-kotlin)
- Umm al-Qura in Ramadan, quoted from the README: "Note: you should add a +30 minute custom adjustment for Isha during Ramadan". 90 + 30 gives the 120-minute rule. The library does **not** apply this automatically. — [adhan-kotlin README](https://raw.githubusercontent.com/batoulapps/adhan-kotlin/main/README.md)
- Secondary prayer-time sites describe Umm al-Qura as "Isha 90 minutes after Maghrib, 120 minutes during Ramadan". No official Saudi publication of the 120-minute rule was found. The Saudi Ministry of Finance FAQ only points users to the Umm al-Qura calendar site for prescribed prayer times. — [eSalah](https://esalah.com/prayer-times/saudi-arabia/?lang=en); [MoF FAQ](https://mof.gov.sa/en/help/faq/Pages/faq_008.aspx)
- High-latitude rules:
  - MIDDLE_OF_THE_NIGHT: "Fajr will never be earlier than the middle of the night and Isha will never be later than the middle of the night"
  - SEVENTH_OF_THE_NIGHT
  - TWILIGHT_ANGLE: "the fraction of the night used is fajrAngle/60 and ishaAngle/60"

  — [adhan-kotlin README](https://raw.githubusercontent.com/batoulapps/adhan-kotlin/main/README.md)
- Madhab: SHAFI gives the earlier Asr, HANAFI the later Asr. — [GitHub adhan-kotlin](https://github.com/batoulapps/adhan-kotlin)
- Other features:
  - Custom per-prayer minute adjustments, e.g. `PrayerAdjustments(fajr = 2)`
  - `SunnahTimes`: middle of the night and last third of the night
  - `Qibla(coordinates).direction`

  — [adhan-kotlin README](https://raw.githubusercontent.com/batoulapps/adhan-kotlin/main/README.md)
- On accuracy, the README says: "All astronomical calculations are high precision equations directly from the book 'Astronomical Algorithms' by Jean Meeus". It does not say what the results were tested against. — [adhan-kotlin README](https://raw.githubusercontent.com/batoulapps/adhan-kotlin/main/README.md)
- Repo popularity at fetch time: 227 stars, 59 forks, 316 commits, 9 open issues. The GitHub Releases section is empty. — [GitHub adhan-kotlin](https://github.com/batoulapps/adhan-kotlin)
- Real apps use Adhan-style stacks. For example, a prayer-times app on Play (com.reworewo.prayertimes 3.7.2) declares USE_EXACT_ALARM, SCHEDULE_EXACT_ALARM and boot permissions. That report covers permissions only and does not show which library the app uses. — [Exodus Privacy report](https://reports.exodus-privacy.eu.org/en/reports/452430)
- PrayTimes.org (alternative):
  - v3.0.0 (25 Mar 2025) says "Change license from LGPL v3.0 to MIT".
  - v3.2.0 (18 Jul 2025) says "Add three new calculation methods" and "Add a method for setting timezone in standard IANA format".
  - Earlier v2.x code was LGPL v3.0 (GPL before 2.1). High-latitude adjustments date from v1.0 (2007).

  — [PrayTimes changelog](https://praytimes.org/docs/changelog)
- Copies of PrayTimes found in the wild still carry the old LGPL/GPL headers, for example a v2.3 gist. — [gist PrayTimes 2.3](https://gist.github.com/rveciana/d5a398bdb55a6caec3e3931f347e4b70)

### Inferences
- For a minSdk-26 Kotlin app, `adhan2` is a direct fit: no desugaring, MIT (compatible with a public GitHub repo), and offline. The Ramadan +30-minute Isha rule for Umm al-Qura has to be implemented in app code, for example by detecting Ramadan from the Hijri calendar in section 2 and adding `PrayerAdjustments(isha = 30)` (or the equivalent method adjustment).
- The 0.0.x version numbers mean pre-1.0 API stability. Pin the exact version.
- Rough location precision for prayer times (arithmetic, not a sourced figure): the sun crosses 1° of longitude in 4 minutes, so about 27 km east-west shift (at the equator) moves times by about 1 minute. A city-level or approximate fix is precise enough.
- PrayTimes v3 is JavaScript. Using it would mean a manual port to Kotlin, so it is a weaker fit than adhan2.

### Gaps
- I could not confirm adhan2's transitive dependency footprint. The README says "no external dependencies" but also relies on kotlinx-datetime, so kotlinx-datetime is presumably a dependency. Check `./gradlew :app:dependencies` and the APK size delta.
- No published comparison was found between adhan2 output and official Umm al-Qura prayer timetables.
- No official Saudi source confirms the Ramadan 120-minute Isha rule. Only secondary prayer-time sites state it.
- PrayTimes.org's current list of methods and its v3 high-latitude handling were not retrieved.

## 2. Offline Hijri dates (ICU IslamicCalendar vs java.time HijrahDate)

### Takeaway
Two built-in options exist:
- `android.icu.util.IslamicCalendar` (API 24) with `CalculationType.ISLAMIC_UMALQURA`, `ISLAMIC_CIVIL`, `ISLAMIC_TBLA` or `ISLAMIC`.
- `java.time.chrono.HijrahChronology`/`HijrahDate` (API 26), whose `INSTANCE` is the Umm al-Qura calendar.

Both are offline and need no desugaring at minSdk 26. Umm al-Qura is a computed civil calendar. Saudi authorities often adjust religious dates after crescent sightings, so a user ±1/±2-day offset is needed. The "~1500 AH / 2077" range limit in the brief was not confirmed; the one source found puts the Umm al-Qura data range at 1300–1600 AH.

### Cited Findings
- `IslamicCalendar` was "Added in API level 24". It "implements the Islamic civil and religious calendars". It "usually should be instantiated using Calendar.getInstance(ULocale) passing in a ULocale with the tag "@calendar=islamic" or "@calendar=islamic-civil" or "@calendar=islamic-umalqura"". — [Android IslamicCalendar](https://developer.android.com/reference/android/icu/util/IslamicCalendar)
- `IslamicCalendar.CalculationType` enum, all API 24:
  - ISLAMIC: "Religious calendar (astronomical simulation)"
  - ISLAMIC_CIVIL: "Tabular (intercalary years [2,5,7,10,13,16,18,21,24,26,29]) algorithm with civil (Friday) epoch"
  - ISLAMIC_TBLA: same tabular algorithm "with astronomical (Thursday) epoch"
  - ISLAMIC_UMALQURA: "Umm al-Qura calendar"

  — [Android IslamicCalendar.CalculationType](https://developer.android.com/reference/android/icu/util/IslamicCalendar.CalculationType)
- On Umm al-Qura, the IslamicCalendar page says it "is also based on the sighting method of the crescent moon but is standardized by Saudi Arabia". — [Android IslamicCalendar](https://developer.android.com/reference/android/icu/util/IslamicCalendar)
- `HijrahChronology` was added in API 26. Its `INSTANCE` is the "Singleton instance of the Islamic Umm Al-Qura calendar of Saudi Arabia". The chronology ID is `Hijrah-umalqura`, the calendar type `islamic-umalqura`, and the locale extension `ca-islamic-umalqura`. "Ordinary years have 354 days; leap years have 355 days." The Android reference page does **not** state the supported year range. — [Android HijrahChronology](https://developer.android.com/reference/java/time/chrono/HijrahChronology)
- Range: the ICU4X source says the Umm al-Qura rules use KACST-derived month lengths from 1300 AH (1882-11-12 ISO) to the end of 1600 AH (2174-11-25 ISO), and fall back to other rules outside that range. This is ICU4X (Rust), not Android's ICU4J or libcore, so Android's exact bounds are unconfirmed. — [ICU4X hijri.rs](https://kdab.searchfox.org/firefox-main/source/third_party/rust/icu_calendar/src/cal/hijri.rs)
- Official vs religious dates: a table of principal Islamic days by the Umm al-Qura calendar (1420–1450 AH) notes these dates are "often adjusted by the religious authorities of Saudi Arabia after reported sightings of the lunar crescent". — [R.H. van Gent, Umm al-Qura principal days](https://webspace.science.uu.nl/~gent0113/islam/ummalqura_principal.htm)
- Ramadan 1448 AH is expected to start around 8 Feb 2027 per Umm al-Qura, subject to the Saudi Supreme Court's moon-sighting announcement. This comes from secondary news coverage. — [Wego Ramadan 2027](https://blog.wego.com/saudi-arabia-ramadan-calendar/)

### Inferences
- **Recommended default:** `HijrahDate.from(LocalDate)` (java.time, API 26) or ICU `IslamicCalendar` with ISLAMIC_UMALQURA. Offer ISLAMIC_CIVIL/TBLA as an alternative "tabular" option for users outside Saudi Arabia.
- **User ±1/±2-day offset:** apply it on the Gregorian side before converting, e.g. `HijrahDate.from(localDate.plusDays(offset))`. The month grid then shifts consistently, and Ramadan detection for the Isha +30 rule follows the user's chosen offset.
- **Arabic month names and Arabic-Indic digits:** use ICU formatting (`android.icu.text.SimpleDateFormat`/`DateFormat` with a `ULocale` such as `ar@calendar=islamic-umalqura`). A numbering-system keyword like `numbers=arab` (or `latn`) can force the digit style. These keyword details come from general ICU knowledge and were not verified in this research. Per the project memory note on Arabic digit separators, use a dash rather than a middle dot between Arabic digits.
- **Desugaring:** not needed at minSdk 26, since java.time and ICU are native. Desugaring only matters if minSdk is ever lowered below 26.
- **Month calendar screen:** compute the Gregorian month, map each day to Hijri (or the reverse), and render in Compose. There is no network dependency.

### Gaps
- Android's actual Umm al-Qura data range for HijrahChronology and ICU4J IslamicCalendar is not stated on developer.android.com. The "1500 AH / 2077" figure in the brief was not supported by any source found. The only figure found is 1300–1600 AH (ICU4X). Verify on a device: convert 2077-12-31 and 2174-11-25 and check for exceptions or fallback.
- No quantitative source was found on how often Saudi religious dates differ from the computed Umm al-Qura dates.
- How the Umm al-Qura rules changed over time (the 1420/1423 AH rule changes) was not retrieved; the van Gent rules page did not load.

## 3. Location without internet, permissions, Data safety and privacy policy

### Takeaway
ACCESS_COARSE_LOCATION alone is enough for prayer times and Qibla. Play policy explicitly tells apps to request the minimum scope "coarse instead of fine". Use the platform `LocationManager` through `LocationManagerCompat.getCurrentLocation` (androidx.core 1.13+), not Google Play services, so the app works on Huawei phones without GMS and stays dependency-free. Offer an offline city picker built from GeoNames (CC BY 4.0, attribution required), which carries the IANA timezone per city.

Location processed only on-device is not "collected" for Data safety. The privacy policy must still be updated, because it has to disclose how the app *accesses* data.

### Cited Findings
- Location policy (excerpts):
  - "Apps should request the minimum scope necessary (for example, coarse instead of fine"
  - "Background location may only be used to provide features beneficial to the user"
  - "You should never request location permissions from users for the sole purpose of advertising or analytics."

  — [Play Policy: Permissions and APIs that Access Sensitive Information](https://support.google.com/googleplay/android-developer/answer/9888170)
- Permissions may only be requested for features "that are promoted in your Google Play listing". Quoted from Play policy: "only request permissions and APIs that access sensitive information that are necessary to implement current features or services in your app that are promoted in your Google Play listing". — [Play policy page cited by search](https://support.google.com/googleplay/android-developer/answer/16558241?hl=en)
- `LocationManagerCompat.getCurrentLocation(...)` (androidx.core 1.13.0) "Asynchronously returns a single current location fix from the given provider". The callback "will be invoked once and only once, either with a valid location or with a null location". It "will never return older locations (for example, several minutes old or older)". Background callers may fail more often: "Background applications may be throttled". — [LocationManagerCompat](https://developer.android.com/reference/androidx/core/location/LocationManagerCompat)
- `LocationManagerCompat.hasProvider()` and `isLocationEnabled()` exist. On Android Q and below, `hasProvider` for FUSED_PROVIDER "may return incorrect results if the client does not hold at least the ACCESS_COARSE_LOCATION permission". — [LocationManagerCompat](https://developer.android.com/reference/androidx/core/location/LocationManagerCompat)
- Huawei Mobile Services has its own Location Kit `FusedLocationProviderClient`. Developers who support both GMS and HMS write dual providers with swapped packages. No official source describing a LocationManager fallback was found. — [Huawei Location Kit reference](https://developer.huawei.com/consumer/cn/doc/HMSCore-References/fusedlocationproviderclient-0000001050746169); [Medium: Huawei Location Kit single class](https://medium.com/huawei-developers/huawei-location-kit-manage-location-from-single-class-3512785702bc)
- Data safety definitions:
  - "'Collect' means transmitting data from your app off a user's device."
  - "User data accessed by your app that is only processed locally on the user's device and not sent off device" does not need to be disclosed as collected.
  - "Approximate location": an area "greater than or equal to 3 square kilometers". "Precise location": "less than 3 square kilometers".

  — [Play Console Help: Data safety](https://support.google.com/googleplay/android-developer/answer/10787469)
- Privacy policy, from the User Data policy:
  - "All apps must post a privacy policy link in the designated field within Play Console". A link or text must also appear inside the app.
  - It must "comprehensively disclose how your app accesses, collects, uses, and shares user data".
  - Prominent disclosure applies "In cases where your app's access, collection, use, or sharing of personal and sensitive user data may not be within the reasonable expectation of the user".

  — [Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)
- GeoNames dump:
  - License: "This work is licensed under a Creative Commons Attribution 4.0 License". The data is provided "as is" without warranty.
  - cities15000: "all cities with a population > 15000 or capitals (ca 25.000)"
  - cities5000: "> 5000 or PPLA (ca 50.000)"
  - cities1000: "ca 130.000"
  - cities500: "ca 185.000"
  - Columns include lat/lon (WGS84), ISO country code, `alternatenames` and "the iana timezone id". Localized names are in alternateNamesV2.zip, with ISO language codes.

  — [GeoNames readme.txt](https://download.geonames.org/export/dump/readme.txt)
- A Play-listed Quran/prayer app's own Data safety notes advise marking location as "collected for app functionality (prayer times)". That advice describes the app's own setup, which sends data off-device, so it does not apply to an on-device-only design. — [Quran Oasis privacy notes](https://sites.google.com/view/quran-oasis-app/home)

### Inferences
- **Permission choice:** ACCESS_COARSE_LOCATION only. Do not request FINE. A Qibla bearing barely changes over a few kilometres, and prayer times change about 1 minute per ~27 km east-west (see section 1), so coarse precision is enough. Do not request background location: ask for a one-shot fix while the user is on the prayer screen, then store the coordinates.
- **Provider choice:** use the platform LocationManager (`LocationManager.NETWORK_PROVIDER`/`GPS_PROVIDER`, or `FUSED_PROVIDER` on API 31+) through LocationManagerCompat. This needs no Google Play services dependency, which keeps the app GMS-free for Huawei and avoids adding a Play services library. The app itself needs no INTERNET permission. GPS works fully offline. Network location is resolved by the system provider, not by the app.
- **City picker as the primary or fallback path:** bundle a trimmed cities15000 subset (name, localized names for the 8 app languages, lat, lon, IANA tz) as a compact asset or SQLite. It lets users who never grant location still get prayer times. Show the GeoNames attribution (CC BY 4.0) in the About screen.
- **Timezone:** for "current location" mode, use the device's `ZoneId.systemDefault()`. For a manually picked city, use the city's IANA tz from GeoNames so times stay correct for a city in another zone.
- **Data safety:** if location never leaves the device, the form can stay "no data collected". The privacy policy text and the in-app privacy statement must still be updated to say the app accesses approximate location on-device to compute prayer times and Qibla, and never transmits it. This is consistent with the existing "nothing leaves the phone" promise.
- **Store listing:** prayer times and Qibla must be described in the listing, because sensitive permissions must back features "promoted in your Google Play listing".
- **Prominent disclosure:** a formal prominent-disclosure screen is probably not mandatory for foreground coarse location used for an obvious, user-started feature. An in-context rationale before the system dialog is still good practice and low-cost.

### Gaps
- Size of cities15000.txt (uncompressed, zipped, and after trimming) was not retrieved. Measure it after building the subset.
- Whether NETWORK_PROVIDER works on Huawei devices without GMS (HMS-only) was not confirmed. Test on the owner's device.
- Whether the owner's specific Honor model ships with GMS was not checked.
- Offline reverse geocoding: no specific library was researched. Nearest-city lookup over the bundled GeoNames subset is the obvious offline approach (inference).

## 4. Adhan alert reliability: exact alarms, notifications, foreground service, Android 17 audio hardening, OEM battery killers

### Takeaway
1. **USE_EXACT_ALARM:** Play limits it to alarm/timer apps and calendar apps that show event notifications. A prayer feature inside a contacts app does not clearly qualify, so the safer path is **SCHEDULE_EXACT_ALARM**. On Android 14+ it is denied by default and the user grants it via the "Alarms & reminders" settings screen. Check `canScheduleExactAlarms()` and fall back to inexact alarms.
2. **Android 17 background audio hardening:** this is the biggest new constraint. It applies to **all apps** on Android 17, regardless of target SDK. Background audio needs a visible activity or a foreground service **that is not shortService**. When targeting API 37, the service must also have while-in-use capability, unless the app holds the exact-alarm permission and plays on a `USAGE_ALARM` stream.
3. **Design that follows:**
   - Short adhan: a notification channel sound with USAGE_ALARM attributes.
   - Full adhan: a `mediaPlayback` foreground service started from the exact alarm, playing with USAGE_ALARM attributes. This needs a Play Console FGS declaration with a demo video.
4. **Rescheduling:** BOOT_COMPLETED, TIME_SET and TIMEZONE_CHANGED receivers.
5. **Huawei/Honor:** users need manual "App launch" whitelisting.

### Cited Findings
**Exact alarms: Play policy text**
- Quoted from the Exact Alarm policy:
  - "USE_EXACT_ALARM is a restricted permission and apps must only declare this permission"
  - "if their core functionality supports the need for an exact alarm."
  - "Your app must use the USE_EXACT_ALARM functionality only when your app's core, user facing functionality"
- "Acceptable use cases for using the Exact Alarm Permission":
  - "The app is an alarm or timer app."
  - "The app is a calendar app that shows event notifications."

  — [Play Policy: Permissions and APIs that Access Sensitive Information](https://support.google.com/googleplay/android-developer/answer/9888170)
- Android docs on USE_EXACT_ALARM: "Granted automatically", "Cannot be revoked by the user", "Subject to an upcoming Google Play policy", "Limited use cases". Versus SCHEDULE_EXACT_ALARM: "Granted by the user", "Broader set of use cases". Also: "Apps will not be able to publish a version of their app with this permission in the manifest unless they qualify based on the policy language." — [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms); [Android 14 exact alarms](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- The July 2022 policy announcement says apps declaring the restricted exact alarm permission that don't meet the criteria "will not be permitted on Google Play". — [Policy announcement July 27, 2022](https://support.google.com/googleplay/android-developer/answer/14554661)
- Some Play prayer apps declare USE_EXACT_ALARM anyway, e.g. com.reworewo.prayertimes. Others use only SCHEDULE_EXACT_ALARM "For exact prayer time notifications". No documented case of Play rejecting a prayer app over USE_EXACT_ALARM was found. — [Exodus report](https://reports.exodus-privacy.eu.org/en/reports/452430); [prayer app privacy policy](https://sites.google.com/view/prayer-times-qibla-finder/home)

**Exact alarms: Android 14+ behaviour**
- SCHEDULE_EXACT_ALARM is "no longer being pre-granted to most newly installed apps targeting Android 13 and higher (will be set to denied by default)". It is needed for `setExact()`, `setExactAndAllowWhileIdle()` and `setAlarmClock()`, otherwise a SecurityException is thrown. An `OnAlarmListener`-based exact alarm doesn't need it.
- The migration flow:
  1. Check `canScheduleExactAlarms()`.
  2. If missing, launch `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
  3. Re-check in `onResume()`.
  4. Listen for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
  5. "Gracefully degrade" if denied, e.g. use `setWindow()`.

  — [Android 14 exact alarms](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- If the permission is revoked, "your app stops, and all future exact alarms are canceled". On grant, the app should reschedule "similar to what your app does when it receives the ACTION_BOOT_COMPLETED broadcast". — [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms)
- Apps on the power allowlist are always allowed to call `setExact`/`setExactAndAllowWhileIdle`. — [Android 14 exact alarms](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- Play policy prohibits requesting a direct Doze exemption (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) "unless the core function of the app is adversely affected". Prayer apps are not in the acceptable-use table; it lists messaging/VoIP that can't use FCM, safety apps, task automation and peripheral companions. Most apps can use `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` instead. — [Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)

**Alarm APIs and Doze**
- `setAlarmClock()`: "the system never adjusts their delivery time", and "leaves low-power modes if necessary". `setExactAndAllowWhileIdle()` fires "even if battery-saving measures are in effect". — [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms)
- In Doze, "Alarms set with setAlarmClock() continue to fire normally. The system exits Doze shortly before those alarms fire." Also: "Neither setAndAllowWhileIdle() nor setExactAndAllowWhileIdle() can fire alarms more than once per nine minutes, per app." WorkManager jobs don't run in Doze. — [Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)
- Inexact alarm limits: on Android 12+, `set()`/`setAndAllowWhileIdle()` fire "within one hour of the supplied trigger time" (absent battery restrictions). The minimum `setWindow` length is 10 minutes. WorkManager periodic work has a "15 minutes minimum". — [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms)

**Rescheduling after boot and time changes**
- "By default, all alarms are canceled when a device shuts down." Declare RECEIVE_BOOT_COMPLETED and a receiver for ACTION_BOOT_COMPLETED. This "only works if the app has already been launched by the user at least once". — [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms)
- Receiving ACTION_TIMEZONE_CHANGED, ACTION_TIME_CHANGED or ACTION_LOCALE_CHANGED (and BOOT_COMPLETED) is an exemption that allows starting an FGS from the background. "Android considers exact alarms to be critical, time-sensitive interruptions … exact alarms aren't affected by foreground service launch restrictions". "Your app invokes an exact alarm to complete an action that the user requests" is also an FGS background-start exemption. — [FGS background start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start); [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms)

**Notification permission**
- POST_NOTIFICATIONS (Android 13+) is a runtime permission. On new installs "notifications are off by default". If denied, "All notification channels are blocked". FGS notices still show in Task Manager but not in the drawer. The docs recommend asking in context, after a user action. — [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)

**Android 17 background audio hardening (new in 2026)**
- For all apps on Android 17, regardless of target: "All apps running on Android 17 that have these background audio interactions must have a visible activity or must be running a foreground service that is not of type SHORT_SERVICE. This applies whether or not the app targets API level 37."
- For apps targeting 37: the FGS must have while-in-use (WIU) capability. "However, the requirement for WIU capabilities is waived if the app has been granted the exact alarm permission, and it is making changes to audio streams that have the USAGE_ALARM attribute."
- Failure mode: "audio playback and volume change APIs fail silently without throwing an exception". Audio focus returns AUDIOFOCUS_REQUEST_FAILED. Example given: an FGS started from BOOT_COMPLETED that plays audio "will be suppressed".
- Test with `adb shell cmd audio set-enable-hardening <enable|disable|throw>`.

  — [Android 17 background audio hardening](https://developer.android.com/about/versions/17/changes/bg-audio); [Android 17 behavior changes, all apps](https://developer.android.com/about/versions/17/behavior-changes-all); [Android 17 behavior changes, targeting 37](https://developer.android.com/about/versions/17/behavior-changes-17)
- WIU comes from standard FGS starts while visible, or from background starts tied to explicit user intent: "notification clicks, widget interactions, or media key events". Exact alarms are **not** listed as granting WIU. — [Android 17 background audio hardening](https://developer.android.com/about/versions/17/changes/bg-audio); [FGS background start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)

**Foreground service types**
- `mediaPlayback`: needs FOREGROUND_SERVICE_MEDIA_PLAYBACK and no runtime prerequisites. "Continue audio or video playback from the background." Apps targeting Android 15+ cannot launch it from a BOOT_COMPLETED receiver.
- `shortService`: needs no type permission and runs "about 3 minutes". It has no sticky support and "Cannot start other foreground services". If it times out, the app gets an ANR.
- `systemExempted` is allowed for "Apps holding SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM permission" (among others).
- Apps targeting 14+ must declare FGS types in Play Console App content.

  — [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- The Play FGS declaration asks for:
  - "a description of the app functionality that is using each foreground service type"
  - the user impact if the task is "deferred" or "interrupted"
  - "a link to a video demonstrating each foreground service feature"

  For mediaPlayback: "Playback must be user-initiated; any automatic soundtracks that play when you open an app do not qualify".
- Full-screen intent: "only apps that have calling or alarm functionalities will have this permission enabled by default". Others "must get user permission to use the USE_FULL_SCREEN_INTENT permission."

  — [Play Console Help: FGS and full-screen intent requirements](https://support.google.com/googleplay/android-developer/answer/13392821)

**Notification sound length**
- No official Android maximum for notification-channel sound length was found. OneSignal recommends under 30 seconds for Android sound files. One OpsGenie report describes an Android alert sound cut off after about 20 seconds. A third-party blog claims USAGE_NOTIFICATION playback is time-limited (unsourced). — [OneSignal notification sounds](https://documentation.onesignal.com/docs/notification-sounds); [OpsGenie-1105](https://jira.atlassian.com/browse/OPSGENIE-1105); [technetexperts blog](https://www.technetexperts.com/expo-30-second-notification-sound/)
- Muslim Pro's help center: "Android notifications will automatically stop as soon as" another app posts a notification or the user pulls down the status bar. On iOS: "Notifications are limited to 30 seconds on Apple devices." — [Muslim Pro: app not playing the full Adhan](https://support.muslimpro.com/hc/en-us/articles/200588785-The-app-is-not-playing-the-full-Adhan-iOS-Android)

**Huawei/Honor battery killers**
- dontkillmyapp's Huawei page:
  - Fix: set the app to "Manage manually" under Battery > App launch, with all toggles on. Disable Smart Tune-up.
  - PowerGenie (EMUI 9+) "kills all apps that are not on its whitelist", and "You cannot add custom apps on their pre-defined whitelist". On some EMUI versions "no user accessible settings can prevent the system to break background processing longer than 60 minutes".
  - The page covers EMUI 4–10 only and calls EMUI 9 the latest. It says nothing about MagicOS or HarmonyOS, so treat it as **dated**.

  — [dontkillmyapp Huawei](https://dontkillmyapp.com/huawei)
- The Quran Majeed app (Pakdata) gives users Huawei steps: Settings > Battery > Battery usage > app > turn off the power-intensive prompt, then Launch: manually enable "Auto-launch, Secondary-launch, and Run in background". — [Pakdata Huawei fix](https://pakdata.com/fix_adhan_notification/huawei)
- Muslim Pro tells users to add the app to "Never Sleeping Apps" and to allow notifications with sound. A third-party guide adds "Task Manager > Lock the app" for Huawei/Honor. — [Muslim Pro troubleshooting](https://support.muslimpro.com/hc/en-us/articles/360029844451-How-to-troubleshoot-adhan-issue-on-Android-device); [Muslim Expert guide](https://www.muslim-expert.app/en/blog/configure-athan-notifications)

### Inferences
**Permissions and Play declarations**
- **Exact alarms:** declare SCHEDULE_EXACT_ALARM, not USE_EXACT_ALARM. Speed Contact's core purpose is contacts/speed dial, so the policy's "core, user facing functionality … alarm or timer app / calendar app" test is unlikely to be met. A USE_EXACT_ALARM declaration could block the release. When the user enables adhan, explain why and deep-link to the "Alarms & reminders" settings screen. If denied, fall back to `setWindow`/`setAndAllowWhileIdle` (about 10 minutes of slack) and say so in the UI.
- **Scheduling API:** with the permission, use `setExactAndAllowWhileIdle()` per prayer. The 9-minute Doze limit is fine for 5 prayers a day. `setAlarmClock()` is the most reliable through Doze and is shown in the status bar as an alarm, which may be unwanted for 5 daily prayers. Schedule only the *next* prayer each time, then chain to the following one when it fires. This keeps state simple and survives day and timezone changes.
- **Receivers:** BOOT_COMPLETED, LOCKED_BOOT_COMPLETED (optional), MY_PACKAGE_REPLACED, TIME_SET, TIMEZONE_CHANGED and ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED all just recompute and reschedule. Under Android 15+ rules, do not start a mediaPlayback FGS from the boot receiver.
- **New manifest items:** RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, ACCESS_COARSE_LOCATION, plus FOREGROUND_SERVICE and FOREGROUND_SERVICE_MEDIA_PLAYBACK only if full adhan is offered. Each needs Play Console App content updates: the FGS declaration with a video, and Data safety reviewed.

**Audio design**
- **Option A, short adhan (recommended first):** a high-importance notification channel per prayer type. Set the sound via `setSound(uri, AudioAttributes.USAGE_ALARM or USAGE_NOTIFICATION_EVENT)` with a short clip (under about 30 s, e.g. the takbir). The system plays it, not the app process, so it should not depend on the app being alive or on the Android 17 app-side audio rules. This is an inference to verify on Android 17.
  - Limits: channel sounds are fixed once the channel is created, so changing the sound means creating a new channel id. Playback stops when another notification arrives or the shade opens (Muslim Pro).
- **Option B, full adhan:** the exact alarm fires a receiver, which starts a `mediaPlayback` FGS (allowed by the exact-alarm background-start exemption). It plays with `USAGE_ALARM` attributes, posts a notification with a Stop action, and stops itself at the end.
  - On Android 17 at targetSdk 36, this meets "FGS not shortService".
  - Once targetSdk reaches 37, the WIU waiver needs both the exact-alarm permission and USAGE_ALARM. Without SCHEDULE_EXACT_ALARM granted, a full adhan from the background is silenced on Android 17 at target 37.
  - `shortService` must **not** be used for audio on Android 17.
  - Play's mediaPlayback rule ("Playback must be user-initiated") is open to interpretation. Argue that the user explicitly enabled the adhan for that prayer, and show that in the declaration video.
  - `systemExempted` is technically allowed for exact-alarm holders, but Play's declaration page does not discuss it, so it is riskier.
- **Do Not Disturb:** USAGE_ALARM audio follows the user's DND "Alarms" setting. Bypassing DND for a channel requires user action (notification policy access). Neither point was confirmed in this research.

**Huawei/Honor**
- These features cannot be made fully reliable by code alone on the owner's phone. Add a "Battery & launch settings" help screen with the App launch > Manage manually steps. Optionally deep-link to `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, which is allowed, unlike the direct-request intent.
- Exact alarms through AlarmManager are generally the most robust mechanism on these devices. Test on the owner's phone with the screen off overnight. Per project memory, test the release build.

### Gaps
- No official number for maximum notification-channel sound duration on Android 14–17.
- No official Google statement on whether prayer-time apps qualify for USE_EXACT_ALARM, and no confirmed rejection case.
- Whether a system-played notification channel sound counts as "background audio interaction" under Android 17 hardening is not stated in the docs. It is inferred not to, but must be tested with `adb shell cmd audio set-enable-hardening enable`.
- DND behaviour for USAGE_ALARM channel sounds and the bypass-DND requirements were not retrieved.
- dontkillmyapp has no MagicOS or HarmonyOS 4/5 data. Current Honor and Huawei behaviour (2025–2026) is undocumented in the sources found.
- When Play will require targetSdk 37 (which would trigger the stricter audio rule) was not checked.

## 5. Qibla direction

### Takeaway
Compute the initial great-circle bearing from the user to the Kaaba (21.4225 N, 39.8262 E); adhan2 already offers `Qibla(coordinates).direction`. Get device heading from TYPE_ROTATION_VECTOR, falling back to accelerometer + magnetometer with `getRotationMatrix`/`getOrientation`. Add magnetic declination from `GeomagneticField` to convert magnetic to true north. Watch `onAccuracyChanged` to prompt a figure-8 calibration. Degrade gracefully on devices without a magnetometer. No Play permission is needed for these sensors.

### Cited Findings
- adhan2 provides `val qibla = Qibla(coordinates)`, and "qibla.direction is the qibla direction". — [adhan-kotlin README](https://raw.githubusercontent.com/batoulapps/adhan-kotlin/main/README.md)
- `GeomagneticField(lat, lon, altitudeMeters, timeMillis)` "Estimates magnetic field at a given point on Earth, and in particular, to compute the magnetic declination from true north". `getDeclination()` returns degrees, where "positive means the magnetic field is rotated east". The reference page (fetched Nov 2026) still says: "This class currently uses WMM-2020 which is valid until 2025, but should produce acceptable results for several years after that. Future versions of Android may use a newer version of the model." — [GeomagneticField](https://developer.android.com/reference/android/hardware/GeomagneticField)
- The orientation-sensor type is deprecated. The recommended route is `SensorManager.getRotationMatrix()` with `getOrientation()`, plus `remapCoordinateSystem()` for the app's frame. Azimuth is "the angle between the device's current compass direction and magnetic north". — [Position sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_position)
- `TYPE_GEOMAGNETIC_ROTATION_VECTOR` doesn't use the gyroscope ("accuracy of this sensor is lower … but the power consumption is reduced"). `TYPE_GAME_ROTATION_VECTOR` doesn't use the magnetic field, so "the Y axis does not point north", which makes it unsuitable for Qibla. — [Position sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_position)
- Calibration: "you can improve the sensor's accuracy by waving the device in a figure-8 pattern". For apps targeting 12+, the magnetic field sensor "is rate-limited". — [Position sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_position)

### Inferences
- **Bearing formula** (standard spherical trigonometry, not sourced in this research): θ = atan2(sin Δλ · cos φ2, cos φ1 · sin φ2 − sin φ1 · cos φ2 · cos Δλ), normalized to 0–360°. Using adhan2's `Qibla` avoids hand-coding it.
- **Heading pipeline:** prefer TYPE_ROTATION_VECTOR (accelerometer + gyroscope + magnetometer fusion); fall back to accelerometer + magnetic field. Then compute true heading = magnetic azimuth + declination, and arrow angle = qibla − true heading. Apply a low-pass or circular moving average on sin/cos (not on raw degrees) to avoid the 359→0 wrap jump. Register at SENSOR_DELAY_UI only while the screen is visible, which is well under the rate limit.
- **Calibration UI:** if `onAccuracyChanged` reports SENSOR_STATUS_ACCURACY_LOW or UNRELIABLE for the magnetometer or rotation vector, show a figure-8 hint.
- **No magnetometer:** if `getDefaultSensor(TYPE_MAGNETIC_FIELD)` is null, show the Qibla bearing as degrees from true north plus a static diagram, with no live compass. Declare `<uses-feature android:name="android.hardware.sensor.compass" android:required="false"/>` (and the same for the accelerometer) so Play does not filter devices out. This is standard uses-feature behaviour and was not re-verified here.
- **Declination model:** Android's GeomagneticField documentation still cites WMM-2020. On recent Android versions, the 2026 declination error is expected to be small relative to phone-compass noise. This is unverified; NOAA WMM2025 coefficients could be bundled if needed, but that is likely overkill.
- **Play:** no permission or declaration is needed for motion and position sensors at normal rates. The HIGH_SAMPLING_RATE_SENSORS permission is unnecessary.

### Gaps
- Whether Android 16/17 updated GeomagneticField to WMM2025 internally despite the stale doc text was not verified.
- No source was found on typical smartphone magnetometer accuracy for Qibla use.
- The uses-feature filtering behaviour for sensor.compass was not re-verified in this session.

## 6. Widgets with RemoteViews: countdown, update scheduling, Android 16/17 limits

### Takeaway
A live countdown without per-second app updates is possible with a RemoteViews `Chronometer`: `setChronometer(base, format, true)` plus `setChronometerCountDown(true)` (API 24). The launcher ticks it, so the app only needs to push a new RemoteViews at each prayer boundary and at midnight, via AlarmManager. WorkManager's 15-minute floor and Doze deferral make it unsuitable for boundary updates. Android 17 adds a strict bitmap-memory ceiling for RemoteViews when targeting API 37. Generated widget previews (Android 15+) are rate-limited to about 2 calls per hour.

### Cited Findings
- `RemoteViews.setChronometer(viewId, base, format, started)`: base is "The time at which the timer would have read 0:00. This time should be based off of SystemClock#elapsedRealtime()". `setChronometerCountDown` was "Added in API level 24" and makes the chronometer "count down to base instead of counting up". — [RemoteViews](https://developer.android.com/reference/android/widget/RemoteViews)
- Supported RemoteViews widgets include Chronometer, TextClock, AnalogClock, TextView, ImageView and ProgressBar. Layouts include ListView, GridView, StackView and AdapterViewFlipper. "Descendants of these classes are not supported." — [RemoteViews](https://developer.android.com/reference/android/widget/RemoteViews)
- `setRemoteAdapter(int, Intent)` "was deprecated in API level 35. use setRemoteAdapter(int, RemoteCollectionItems) instead". — [RemoteViews](https://developer.android.com/reference/android/widget/RemoteViews)
- **Android 17 (targeting API 37) bitmap memory limit:**
  - "the system enforces a strict memory limit on RemoteViews updates for apps targeting Android 17". If exceeded, "the system rejects the update and throws an IllegalArgumentException".
  - Budget = "6 * width * height bytes" of the active display. It counts Bitmap and bitmap-backed Icons across all size buckets.
  - Advice: use full `updateAppWidget` rather than `partiallyUpdateAppWidget` when swapping images. At API 36 or lower, a violation is only logged.

  — [Android 17 behavior changes, targeting 37](https://developer.android.com/about/versions/17/behavior-changes-17)
- Android 17 also adds `setViewPadding` with complex units (dp/sp) and `OPTION_APPWIDGET_DISPLAY_ID` for widgets on external displays. These come from Android 17 beta coverage. — [Android Developers Blog, Android 17 Beta 3](https://android-developers.googleblog.com/2026/03/the-third-beta-of-android-17.html?m=0)
- Generated previews: `setWidgetPreview` is "a rate-limited API". The default is about two calls per hour, per the localized docs. There is no system callback, and `previewImage` remains the fallback. — [Add generated previews to your widget picker](https://developer.android.com/develop/ui/compose/glance/generated-previews); [Widget previews (views)](https://developer.android.com/develop/ui/views/appwidgets/previews)
- WorkManager periodic work has a "15 minutes minimum", and WorkManager jobs don't run during Doze. Inexact alarms can be delayed up to 1 hour, or by at least 10 minutes for windows. — [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms); [Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)

### Inferences
- **Next-prayer widget:**
  - A static TextView for the prayer name and time ("Asr 15:32").
  - Optionally a Chronometer for the countdown, with base = `SystemClock.elapsedRealtime() + (nextPrayerMillis − System.currentTimeMillis())`, countdown on.
  - A TextClock if the current time is wanted.

  None of these need app wake-ups between boundaries; the launcher process ticks them.
- **Conflict with the owner's "widgets must stay still" preference** (project memory): a Chronometer ticks seconds (format MM:SS or H:MM:SS) and cannot hide seconds by format. A plain "next: Asr 15:32" line, or minute-level text refreshed by a boundary alarm, may suit the owner better. If a Chronometer fires late, it counts past zero into negative values. Recommended: show the static prayer time as the primary element and make the countdown optional.
- **Update scheduling:** reuse the same exact (or fallback inexact) alarm chain that drives adhan alerts. On each boundary, call `updateAppWidget` with a full RemoteViews, and also update at local midnight for the Hijri/Gregorian date line. WorkManager can stay as a periodic safety net (every few hours) to repair drift, but not as the boundary trigger.
- **Month calendar widget:** if one is added, use ListView rows (per the project rule that GridView swipes are stolen by Huawei/Honor launchers) with `RemoteCollectionItems`, since the Intent-based adapter is deprecated in API 35. Avoid bitmaps (text-only cells) to stay far below the Android 17 memory budget.
- **Battery:** a handful of exact alarms per day (5 prayers plus midnight) is negligible compared with periodic polling. The Chronometer and TextClock cost nothing in the app process.

### Gaps
- No Android 16-specific RemoteViews or widget behaviour change was found beyond generated previews (introduced in Android 15) and the Android 17 memory ceiling.
- How Huawei/Honor launchers render Chronometer countdown in widgets, and whether they throttle it, is untested and undocumented.
- Whether `setChronometer` values survive launcher restarts on Huawei/Honor without an app update is unknown. Test it.

## 7. Architecture fit, Play policy on combined features, and the closed-test implications

### Takeaway
No Play policy forbids combining contacts and prayer features in one app. The binding rules are:
- Sensitive permissions must back features "promoted in your Google Play listing".
- The app must deliver what its listing says (Deception; Spam, Functionality and UX).

Adding prayer, Qibla and calendar features therefore means updating the listing, the privacy policy, Data safety and the FGS declaration. The Play Console page does not say that a new release during a closed test resets the 14-day clock; the 14 days are counted per tester opt-in. A separate app would need its own 12-tester, 14-day closed test.

### Cited Findings
- Closed-test rule for personal accounts: "must run a closed test for their app with a minimum of 12 testers who have been opted in continuously for at least 14 days". If "a tester opts out and opts back in later, the 14 days must be consecutive". The page encourages updating during the test: "Continue running closed tests while resolving user-reported issues and bugs."
- The production-access form asks:
  - whether testers used all available features
  - whether "tester usage matched expected production user behavior"
  - target audience and value proposition
  - "Describe any changes made to your app or game based on what you learned from your closed test."

  — [Play Console Help: App testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
- Conflict note: one search-result summary claimed Google's page says 20 testers. The direct fetch of the same page (Nov 2026) says 12. The 20 figure is presumably an older cached version; trust the direct fetch. — [Play Console Help 14151465](https://support.google.com/googleplay/android-developer/answer/14151465)
- Permission and listing linkage: request sensitive permissions only for "current features or services in your app that are promoted in your Google Play listing". — [Play policy page (search excerpt)](https://support.google.com/googleplay/android-developer/answer/16558241?hl=en)
- The Spam, Functionality, and User Experience policy (formerly Minimum Functionality) disallows apps with "only limited functionality and content". The Deception policy: "Apps should provide all communicated functionality. Apps should not confuse users." — [Play: Functionality, Content, and User Experience](https://support.google.com/googleplay/android-developer/answer/14983486?hl=en); [Play policy page](https://support.google.com/googleplay/android-developer/answer/16909972)
- Apps targeting 14+ must declare FGS types in Play Console App content, with a description, user impact and a demo video. — [Play Console Help: FGS requirements](https://support.google.com/googleplay/android-developer/answer/13392821)
- USE_EXACT_ALARM: apps "will not be able to publish a version of their app with this permission in the manifest unless they qualify". — [Android 14 exact alarms](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- Android 17 Play Console declarations exist for other restricted permissions, e.g. "Apps targeting Android 17 or higher (API 37+) must submit a Play Console declaration to use" the Local Network permission. This shows the declaration mechanism is expanding. It is not relevant to these features unless the network is used. — [Play Console Help 12253906 (partial)](https://support.google.com/googleplay/android-developer/answer/12253906)

### Inferences
**Module in the same app**
- Pros: one listing, the existing testers, shared widget infrastructure, and no new 14-day test.
- Cons:
  - Permissions grow from 2 to about 6–8 (coarse location, notifications, exact alarm, boot, and FGS media if full adhan is offered).
  - The listing's purpose shifts from "speed contact" to "contacts + Islamic tools", which weakens the clear single-purpose story and the privacy pitch.
  - The production-access questionnaire's "target audience/value proposition" answers must cover both.

  Size impact is small: adhan2 is pure Kotlin, and a trimmed cities list is likely a few MB at most (unmeasured). Build it as a Gradle feature module (`:feature:prayer`) inside the same APK. Optionally, a build flag can drop it from a variant.
- **Separate app:** a clean single purpose and a focused listing, but by the per-app rule ("a closed test for their app") it needs a fresh closed test with 12 testers for 14 days, plus a new listing, privacy policy and Data safety form. This is inferred from the per-app wording; confirm in Play Console.

**Timing and the current closed test**
- The 1.4.1 closed test was sent for review on 2026-11-09. Do not swap the build under review. Adding the features in a later closed-test release is permitted ("Updating your app in closed testing … helps"). No source says a new release resets the 14-day counter, because the counter tracks tester opt-in continuity.
- A release that adds new sensitive permissions and FGS types will need new App content declarations (FGS, Data safety re-confirmation, possibly exact alarm). Each new release is reviewed, so review time could grow.
- Practical sequencing (inference): finish the current 14-day test and production access first with the existing scope, then ship the prayer features as an update. The alternative is to add them early enough that testers can exercise them, since the questionnaire asks whether testers "used all available app features".

**Listing and privacy wording**
- **Listing:** add the new features to the short and full descriptions and screenshots in all 8 languages, because permissions must match "promoted" features.
- **Privacy policy:** update it even though Data safety can stay "no data collected", because it must disclose data the app "accesses" (approximate location on-device) as well as data it collects.

### Gaps
- No Google page states whether publishing a new closed-test release (with new permissions) resets or pauses the 14-day requirement. Third-party guides describe developers "resetting" the clock but don't identify the trigger.
- No source was found on whether a rejected production-access application restarts the closed test. Only unverified third-party claims exist.
- Play's current review-time expectations for closed-track updates that add FGS or exact-alarm declarations were not retrieved.
- No explicit Play policy text on combining "unrelated" features in a single app was found. The constraints come from the permission/listing linkage and the Deception and Functionality policies.
