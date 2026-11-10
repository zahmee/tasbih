# Leading Android Prayer-Times / Adhan Apps (state as of Nov 2026)

Research date: 2026-11-10. Scope: prayer times, adhan alerts, prayer widgets. Hijri and Qibla are mentioned only in passing.

Note on data sources: Google Play pages could not be read directly (the fetch returned only the page title; AppBrain returned 403). Ratings, rating counts, install figures, ads/IAP flags and detected SDKs below come from AppGoblin, which mirrors Play data and adds its own SDK scan and revenue-split estimates. AppGoblin install figures are point estimates (for example "148.5M"), not Play's "100M+" style ranges. Any Play range given below is my inference from those figures. All AppGoblin pages were read on 2026-11-10.

## 1. Ratings, reviews, installs and pricing per app

### Takeaway
The big ad-funded apps are Muslim Pro (about 148M installs, 4.2 stars), Al-Moazin Lite (about 43M), Salaat First (about 40M), Muslim App by Assistant App (about 34M) and Prayer Now (about 29M). The best-rated apps are ad-free or low-ad: Sajda (4.9), Athan by IslamicFinder (4.8, but it has ads), Mawaqit (4.8, free and donation-funded) and Salaat First (4.7). Pillars is small on Android (about 750K installs, 4.3 stars) even though it is famous for privacy.

### Cited Findings

**Muslim Pro (Bitsmedia, `com.bitsmedia.android.muslimpro`)**
- 4.23 stars from 1.9M ratings. 148.5M installs (Play bucket likely "100M+"). Updated Oct 30, 2026. Has both ads and IAP; AppGoblin estimates revenue at about 57% ads and 43% IAP — [AppGoblin](https://appgoblin.info/apps/com.bitsmedia.android.muslimpro)
- 12 SDKs detected. Ad networks: Google, Facebook, AppLovin, Unity. Mediation: AdMob. Analytics: Firebase. Business tools: Alipay, Volcengine, Tencent, Purchasely — [AppGoblin](https://appgoblin.info/apps/com.bitsmedia.android.muslimpro)
- Plans: "Muslim Pro + Qalbox" monthly/yearly (5 devices); Lifetime Premium (5 devices); Premium monthly/yearly without Qalbox (2 devices); "No Ads" only (1 device). Prices are not published on the help page. Premium works on Android and iPhone through one account — [Muslim Pro Help: free vs Premium](https://support.muslimpro.com/hc/en-us/articles/7967476840729-What-is-the-difference-between-the-free-and-Premium-Versions-of-Muslim-Pro)
- Paywalled features: choosing different adhan (muezzin) recordings, choosing Quran reciters, themes, Qibla dials, tasbih beads, offline Quran audio, Qalbox videos, removing ads, and "see all prayer times in the widget" — [Muslim Pro Help](https://support.muslimpro.com/hc/en-us/articles/7967476840729-What-is-the-difference-between-the-free-and-Premium-Versions-of-Muslim-Pro)
- The marketing site claims "Trusted by 190M+ worldwide" and a 4.7 rating. That conflicts with the 4.23 Play average on AppGoblin — [muslimpro.com](https://www.muslimpro.com/prayer-times-and-adhan-app/); [AppGoblin](https://appgoblin.info/apps/com.bitsmedia.android.muslimpro)
- A third party lists 4.07 stars, 50M+ Android installs (about 98M total), and premium that is "among the most expensive". This source sells a competing app (islamtics) — [islamtics, May 2026](https://islamtics.com/?p=21177)

**Athan by IslamicFinder (`com.athan`; Play name "Athan: Prayer Times, Dua-Azkar")**
- 4.83 stars from 333K ratings. 18.7M installs. Updated Nov 5, 2026. Ads plus IAP (about 58% ads / 42% IAP). SDKs: Google, Facebook, ChartBoost, AdMob, Firebase, Google Analytics, Zendesk. Premium is an ad-free "Athan Pro" tier, billed monthly or yearly — [AppGoblin](https://appgoblin.info/apps/com.athan)
- IslamicFinder claims 55M users, 4.7 on Google Play and 4.9 on Huawei AppGallery. Its own page says the app is free — [islamicfinder.org/athan](https://www.islamicfinder.org/athan/)
- The App Store lists monthly and yearly "Athan Premium" plus a separate Remove Ads purchase. Paid tiers remove ads and add offline audio and content — [search summary of App Store / subger listings](https://subger.com/en/service/athan-pro)
- Do not confuse this with "Athan Pro: Prayer Times & Azan" by Quanticapps, which is a separate app (below).

**Athan Pro (Quanticapps FZ LLE, `com.quanticapps.athan`)**
- 4.53 stars from 99K ratings. 4.4M installs. Updated Nov 6, 2026. Ads plus IAP (about 66% IAP). SDKs: Google, Facebook, AppLovin, Vungle, AdMob, Kochava and AppStack (attribution), Firebase, RevenueCat, Keepsafe, Batch — [AppGoblin](https://appgoblin.info/apps/com.quanticapps.athan)
- The listing describes a free version with "essential prayer features" and a Premium tier for "additional features" — [Play listing via search](https://play.google.com/store/apps/details?id=com.quanticapps.athan&hl=en_US)

**Pillars (`com.pillars.pillars`)**
- Android: 4.26 stars from 4.4K ratings. 752K+ installs. Updated Nov 6, 2026. AppGoblin flags "no ads" and "no IAP". However, its SDK scan finds RevenueCat and Superwall (paywall/subscription SDKs), Firebase analytics, Sentry and Flutter — [AppGoblin](https://appgoblin.info/apps/com.pillars.pillars)
- Pillars' own site claims 1.6M–2M users, depending on the page section. It cites 4.7 stars from 23,819 ratings across both stores. It lists a Premium subscription (Apple Watch, PrayFirst, Fajr Smart Alarm, full athan, "much more") but gives no price — [thepillarsapp.com](https://thepillarsapp.com/)
- iOS: 4.8 stars from 3.5K reviews (search snippet), labelled "Data Not Linked to You" — [App Store](https://apps.apple.com/us/app/-/id1559086853)
- Conflict: islamtics says Pillars is "free; community-supported" with "no analytics SDK" — [islamtics](https://islamtics.com/?p=21177). That contradicts the Premium tier on Pillars' own site and the Firebase/RevenueCat/Superwall SDKs AppGoblin detected. Unstar also says Pillars charges a "subscription for full features" — [Unstar, Jun 2026](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)

**Mawaqit (MAWAQIT, France, `com.kanout.mawaqit`)**
- 4.78 stars from 174K ratings. 5.4M+ installs. Updated Sep 24, 2026. No ads, no IAP ("Free and without any Ads"). SDKs: Google, Sentry, Flutter, Firebase, Intercom, Keepsafe, Ably — [AppGoblin](https://appgoblin.info/apps/com.kanout.mawaqit)
- The site says the service is free and ad-free in 160+ countries, with 4M+ people following their mosque in the app — [mawaqit.net via search](https://www.mawaqit.net/)
- Beware look-alikes: `com.mawaqit.app` ("Mawaqit: Islamic Prayer Times") is a different, calculation-based product. `com.mawaqit.androidtv` is the mosque-screen TV app — [Play via search](https://play.google.com/store/apps/details?id=com.mawaqit.app&hl=en_US)

**Salaat First (Hicham Boushaba, `org.hicham.salaat`)**
- 4.68 stars from 800K ratings. 39.8M installs. Updated Oct 11, 2026. AppGoblin flags it as free with no ads and no IAP. Its SDK scan still finds Google and Huawei ad-network SDKs, Huawei Analytics and Firebase — [AppGoblin](https://appgoblin.info/apps/org.hicham.salaat)
- Conflict: islamtics says Salaat First shows ads "on most screens" — [islamtics](https://islamtics.com/?p=21177)
- Clone and name confusion: "Salaat First - Prayer Times" by SOUKAPPS (Morocco, `com.soukapps.salatuk`) is a separate ad-supported app. It has 4.61 stars from 25K ratings and 993K+ installs, with AdMob — [AppGoblin](https://appgoblin.info/apps/com.soukapps.salatuk). There is also another "Salaat Fіrst" listing (`com.trg.salat`) — [Play via search](https://play.google.com/store/apps/details?id=com.trg.salat&hl=en_US)

**Prayer Now (AppRocks, `com.AppRocks.now.prayer`)**
- 4.72 stars from 1.5M ratings. 28.6M installs. Updated Oct 25, 2026. Ads plus IAP (about 51% ads / 49% IAP). SDKs: Google, Facebook, Unity, ByteDance ad networks, Firebase, Huawei Analytics, Volcengine — [AppGoblin](https://appgoblin.info/apps/com.AppRocks.now.prayer)
- On iOS, IAP includes a premium upgrade at $9.99 (App Store page may be old, © 2022) — [App Store via search](https://apps.apple.com/us/app/-/id1081440836)

**Al-Moazin Lite (Parfield, `com.parfield.prayers.lite`)**
- 4.43 stars from 367K ratings. 42.6M installs. Last updated Jun 26, 2026, which is less recent than the others. Mostly ad-funded (about 74% ads / 26% IAP). SDKs: Google and Huawei ads, AdMob, Firebase, Huawei Analytics, Amazon — [AppGoblin](https://appgoblin.info/apps/com.parfield.prayers.lite)
- Paid version only: the Fajr wake-up notification and "full support" for multiple prayer notifications — [AppGoblin](https://appgoblin.info/apps/com.parfield.prayers.lite)

**Gulf / Saudi-made or Gulf-popular apps**
- "My Prayer" by Azure Droid (Saudi Arabia, `com.haz.prayer`): 4.46 stars from 119K ratings. 10.1M+ installs. Updated Apr 2, 2026. **No ads.** Its only IAP is an optional "support the developer" purchase. AppGoblin detected one SDK (Google dev tools) — [AppGoblin](https://appgoblin.info/apps/com.haz.prayer)
- "Muslim App: Azan, Namaz, Qibla" by Assistant App Teknoloji AS (Turkey, `com.hundred.qibla`). This is the app often called "Muslim Assistant" / "Muslim: Prayer Times, Qibla". 4.6 stars from 1M ratings, 34.1M installs, updated Sep 2, 2026. Ads plus a Pro IAP. **20 SDKs**, including 8 ad networks (Google, Facebook, AppLovin, Vungle…), AppsFlyer, Adapty and Volcengine — [AppGoblin](https://appgoblin.info/apps/com.hundred.qibla); [search: publisher identity](https://apps.appfollow.io/android/muslim-ramadan-2025-azan/com.hundred.qibla?country=vn)
- Official "تقويم أم القرى الرسمي" (Umm al-Qura calendar) app by KACST: iOS only in the results, 3.4 stars from only 5 ratings — [App Store](https://apps.apple.com/app/id6752710662)
- "المصلي السعودي" (Saudi prayer times with adhan alert): iOS, 3.2 stars — [App Store](https://apps.apple.com/sa/app/id1566657669)
- Arabic 2025–2026 roundups recommend these:
  - Al-Ain's Ramadan 2026 list: Salatuk (صلاتك), Pillars (described as ad-free and not collecting data) and iPray (works offline) — [Al-Ain](https://al-ain.com/article/ramadan-2026-apps)
  - An Egyptian 2025 list: Athan, المؤذن (Al-Moazin), iPray, and "إسلامنا" and "صلاتك" for Saudi Arabia — [aboutmsr 2025](https://aboutmsr.com/أفضل-تطبيقات-مواقيت-الصلاة-2025-لا-تفوت-صل/) (via search summary)

**Newcomers and design-led apps**
- Sajda (GOOD/APP, `com.raimbekov.android.sajde`): 4.9 stars from 466K ratings. 13.2M installs. Updated Oct 27, 2026. **No ads.** The Sajda+ IAP covers offline Quran and exclusive recitations. SDKs: Google, OneSignal, Firebase, RevenueCat. The description says "Every screen is thoughtfully designed and obsessively refined" — [AppGoblin](https://appgoblin.info/apps/com.raimbekov.android.sajde). Another tracker reportedly shows 60K ratings, a 4.88 average and 11M installs, which conflicts with AppGoblin's counts — [search summary](https://similarweb.com/app/google/com.elevensoft.islamicassistant/vs/com.raimbekov.android.sajde)
- Deen (Let's Flutter, `com.letsflutter.deen`): Salah times, forbidden-prayer periods, and a home-screen widget with the Hijri date. No ratings found — [Apptopia](https://apptopia.com/google-play/app/com.letsflutter.deen/about)
- Al-Azan (open source, F-Droid, `com.github.meypod.al_azan`): ad-free, **no internet permission**, no trackers. v2.4.1 was added Nov 6, 2026 — [F-Droid](https://f-droid.org/en/packages/com.github.meypod.al_azan/)
- Other small ad-free newcomers on Play exist, such as FivePrayer, Waqto Salat and "Minimal Qibla N Prayer Times". Launch dates are unverified — [search summary](https://waqtosalat.com/ad-free-prayer-times-app/)
- No reliable data was found for "Muwazzin" or "Ummah" prayer apps.

### Inferences
- Rating vs. monetization: the highest-rated apps (Sajda 4.9, Mawaqit 4.78, Salaat First 4.68) do not show ads, or show few. Muslim Pro, the heaviest ad and SDK load (Facebook/AppLovin/Unity, Chinese ad/payment SDKs), has the lowest rating among the big apps (4.23). This fits the complaint data in section 5.
- Wide distribution in Arab markets shows in the Huawei SDKs and Huawei AppGallery listings (Salaat First, Al-Moazin, Prayer Now, Athan). Many Gulf/Egypt users run Huawei phones without Google Mobile Services.
- Name squatting is common: there are several "Salaat First", "Mawaqit" and "Athan Pro" look-alikes. A new app should pick a distinct name.

### Gaps
- Exact Play install buckets (for example "10M+") and Play review counts could not be read from Play directly.
- No current prices were found for Muslim Pro, Athan or Pillars subscriptions. Prices vary by region.
- No download ranking for Saudi Arabia or the Gulf was found. Sensor Tower public pages were not reached.

## 2. Prayer-time calculation features

### Takeaway
All the major apps offer the standard methods: MWL, ISNA, Egyptian, Karachi and Umm al-Qura. Salaat First lists 16, My Prayer 8 and Al-Moazin 6 including Iraq. They also offer the Hanafi/Shafi'i Asr choice. Pillars stands out by choosing the method automatically by location and re-choosing it on travel. Mawaqit is the outlier: it shows the mosque's own published timetable and iqama rather than a calculation. Marketing pages rarely say much about high-latitude rules, per-prayer minute offsets or DST. The open-source Adhan library shows the standard set a small app can implement.

### Cited Findings
- **Muslim Pro:** advertises "local calculation settings" as free. It says it "syncs directly with local authorities" and partners with religious authorities, and it updates times automatically on travel and has an auto-detect location toggle. The page names no methods and says nothing about Asr options, high latitude or manual adjustments — [muslimpro.com](https://www.muslimpro.com/prayer-times-and-adhan-app/)
- **Athan (IslamicFinder):** the legacy mobile feature page lists "five famous Calculation Methods and Juristic Method". The Windows version allows custom Fajr angle, Isha angle, Dhuhr time after zawal and Maghrib time after sunset. These pages are older (they refer to Java/Symbian phones) — [islamicfinder.org/athan-mobile](https://www.islamicfinder.org/athan-mobile/); [athan-windows](https://www.islamicfinder.org/athan-windows/)
- islamtics credits Athan with "multiple calculation methods, per-prayer adjustments, adhan works in airplane mode" — [islamtics](https://islamtics.com/?p=21177)
- **Athan Pro (Quanticapps):** MWL, ISNA, Egypt, Umm al-Qura "and more" — [AppGoblin](https://appgoblin.info/apps/com.quanticapps.athan)
- **Pillars:** picks a method automatically by location (Moonsighting Committee, ISNA, MWL…), updates it when travelling, and allows a manual choice. Extra times: Tahajjud, Duha, and a last-third-of-the-night calculator. "Mosque Prayer Times (Beta)" shows a chosen mosque's athan and iqama next to the calculated times (UK/USA/Canada). All calculation happens on the device — [thepillarsapp.com](https://thepillarsapp.com/). The Android description adds "early or late Asr options" — [AppGoblin](https://appgoblin.info/apps/com.pillars.pillars)
- **Mawaqit:** "precise timetables set by your Imam, unlike apps that give approximations". It covers Fajr, Sunrise, Dhuhr, Asr, Maghrib and Isha plus iqama, and Ramadan Imsak/Iftar. Mosque admins manage iqama, Ramadan and Eid schedules from the app — [Play via search](https://play.google.com/store/apps/details?id=com.kanout.mawaqit); [AppGoblin](https://appgoblin.info/apps/com.kanout.mawaqit)
- **Salaat First (org.hicham.salaat):** 16 methods, including Umm Al-Qura, MWL and Turkey Diyanet — [AppGoblin](https://appgoblin.info/apps/org.hicham.salaat)
- **Prayer Now:** methods "from several schools of thought" plus a Hanafi Asr option — [AppGoblin](https://appgoblin.info/apps/com.AppRocks.now.prayer)
- **iPray:** times can be adjusted to match the local time zone, and the app works offline — [Al-Ain](https://al-ain.com/article/ramadan-2026-apps)
- **Al-Moazin Lite:** six methods: Umm al-Qura (Makkah), Egyptian, Karachi, ISNA, MWL, and Iraqi Sunni Endowment (downloadable tables for Iraqi cities). "Follow Me" updates the location automatically while travelling. The Hijri calendar has manual correction — [AppGoblin](https://appgoblin.info/apps/com.parfield.prayers.lite)
- **My Prayer (Saudi):** eight methods: Umm Al-Qura, MWL, Karachi, Egyptian, ISNA, UOIF France, Kuwait Ministry of Awqaf, and "Angle Based" — [AppGoblin](https://appgoblin.info/apps/com.haz.prayer)
- **Muslim App (Assistant App):** madhab and "latitude adjustments", and it updates on time-zone change — [AppGoblin](https://appgoblin.info/apps/com.hundred.qibla)
- **Al-Azan (open source):** many methods (Morocco added in 2.4.x), offline location search or GPS, and a midnight-method setting that also applies to Tahajjud — [F-Droid](https://f-droid.org/en/packages/com.github.meypod.al_azan/)
- **Reference standard, the Adhan library (batoulapps, MIT, Kotlin Multiplatform, Android API 26+):**
  - Methods with Fajr/Isha angles: MWL 18/17; Egyptian 19.5/17.5; Karachi 18/18; **Umm al-Qura 18.5 / Isha 90 min after Maghrib, add +30 min in Ramadan**; Dubai 18.2/18.2; **Qatar 18 / 90 min**; Kuwait 18/17.5; Moonsighting Committee 18/18 with seasonal adjustment; Singapore 20/18; ISNA 15/15; Turkey 18/17
  - Madhab: Shafi (earlier Asr) or Hanafi (later Asr)
  - High-latitude rules: MiddleOfTheNight, SeventhOfTheNight, TwilightAngle
  - Per-prayer minute adjustments
  - Sunnah times: middle of the night and last third of the night
  - [batoulapps/adhan-java](https://github.com/batoulapps/adhan-java)
- **Accuracy:** a March 2026 test by FivePrayer (a competing app) found Muslim Pro, Athan Pro and Pillars within about ±2 min of official sources "when configured correctly". Complaints about times differing from the local mosque are usually traced to a wrong default method or a wrong Hanafi/Shafi'i Asr setting — [FivePrayer blog via search](https://www.fiveprayer.app/blog/best-prayer-time-apps) (vendor source)

### Inferences
- For Saudi Arabia, the decisive settings are Umm al-Qura with Isha at a fixed 90 min after Maghrib, plus the Ramadan rule (120 min). Qatar uses a modified Umm al-Qura; the UAE and Kuwait have their own angles. An app that sets the method automatically from the country, as Pillars does, avoids the most common "wrong times" complaint.
- Mawaqit's model (the mosque publishes, the app follows) is the most accurate locally wherever a mosque is registered. Its Fajr/Isha may differ from any calculation method by design.

### Gaps
- No source documented DST handling, Imsak offset defaults, or the high-latitude options of Muslim Pro, Athan or Prayer Now.
- No verified list of Muslim Pro's or Athan's current method names on Android 2026 was found. Help pages for those settings were not reached.
- No independent (non-vendor) accuracy test for the Gulf was found.

## 3. Adhan and alert features, including battery-optimization handling

### Takeaway
Typical options are a choice of muezzins, full adhan vs. short adhan ("Allahu Akbar") vs. beep vs. silent, set per prayer, plus a pre-prayer reminder. Some apps also offer a distinct Fajr adhan, an iqama reminder and automatic silent mode (Prayer Now, Salaat First). Several apps paywall adhan voices or Fajr features: Muslim Pro (muezzin choice), Al-Moazin (Fajr wake-up) and Pillars (full athan and Fajr Smart Alarm, inconsistently). Missed adhans on Android are a standing complaint. Every app's help center points users to manual per-brand battery whitelisting. Android 14 also made exact-alarm permission denied by default.

### Cited Findings
- **Muslim Pro:** adhan recordings "from around the world" (for example Medina and Indonesia) and a notification a few minutes before each prayer — [muslimpro.com](https://www.muslimpro.com/prayer-times-and-adhan-app/). Choosing a different muezzin is Premium-only — [Muslim Pro Help](https://support.muslimpro.com/hc/en-us/articles/7967476840729-What-is-the-difference-between-the-free-and-Premium-Versions-of-Muslim-Pro)
- **Athan (IslamicFinder), legacy page:** Makkah athan, short athan and beep; per-alert choice of complete athan, small athan ("Allahu Akbar") or beep; volume control. The Windows version offered muezzins from Makkah, Madina, Al-Aqsa, Egypt, Lebanon, Bosnia, Pakistan and Yusuf Islam, with a different reciter per prayer — [islamicfinder.org/athan-mobile](https://www.islamicfinder.org/athan-mobile/); [athan-windows](https://www.islamicfinder.org/athan-windows/)
- **Athan Pro (Quanticapps):** multiple azan voices from well-known muadhins — [AppGoblin](https://appgoblin.info/apps/com.quanticapps.athan)
- **Prayer Now:**
  - Muezzins include Mishary Rashid, Al-Naqshabandi and Saad Al-Ghamdi
  - The phone goes silent automatically during prayer times
  - Turning the phone face-down mutes the athan — [AppGoblin](https://appgoblin.info/apps/com.AppRocks.now.prayer)
  - Reminders for iqama ("stand up for prayer"), daily azkar, Qiyam al-Layl, fasting the White Days and Mondays/Thursdays, Quran werd, and Surat al-Kahf on Fridays — [Cafebazaar listing](https://cafebazaar.ir/app/com.AppRocks.now.prayer?l=en)
- **Salaat First:**
  - Choice from multiple adhans — [AppGoblin](https://appgoblin.info/apps/org.hicham.salaat)
  - The SOUKAPPS clone lets users download their favourite muezzin and switches to silent automatically, with a switch time set per prayer — [AppGoblin](https://appgoblin.info/apps/com.soukapps.salatuk)
- **Pillars:**
  - Per-prayer notification: simple buzz, partial athan or full athan
  - The full athan uses the iOS alarm system, so it sounds even when the phone is muted
  - Fajr Smart Alarm wakes the user a set time after Fajr, recalculated daily
  - Premium lists full athan and Fajr Smart Alarm, while the FAQ says anyone can choose the full athan, so the site is inconsistent
  - [thepillarsapp.com](https://thepillarsapp.com/)
- **Mawaqit:**
  - Multiple muezzin voices; the notification mode is a "dome" icon (adhan) or a "bell" icon (beep)
  - The adhan plays on the **notification** sound channel, so a muted notification volume silences it
  - The latest version prompts the user to allow background running
  - [Mawaqit Help, Sep 9, 2025](https://help.mawaqit.net/en/articles/12230364-troubleshooting-the-adhan-on-android-phones)
- **Al-Moazin Lite:** the Fajr wake-up notification and full multi-notification support are paid-only. Notification audio follows the phone's ringer mode — [AppGoblin](https://appgoblin.info/apps/com.parfield.prayers.lite)
- **My Prayer (Saudi):** no bundled adhan audio; the user picks an adhan MP3 from storage as the notification tone — [AppGoblin](https://appgoblin.info/apps/com.haz.prayer)
- **Muslim App (Assistant App):** a default adhan based on maqam, country-specific adhan sounds, and silent or vibration-only reminders — [AppGoblin](https://appgoblin.info/apps/com.hundred.qibla)
- **Al-Azan (open source):**
  - Custom adhan audio or any device sound, with a separate Fajr adhan
  - Reminders before or after any prayer, repeatable every few days, monthly or on a single date
  - Notifications clear automatically after a set time
  - [F-Droid](https://f-droid.org/en/packages/com.github.meypod.al_azan/)
- **Battery handling, Muslim Pro:**
  - Its help asks users to allow notifications, turn off power saving, and add the app to "Never Sleeping Apps" (Samsung path: Settings > Battery > Background usage limits)
  - It links separate guides for Samsung, Xiaomi, Oppo/Vivo/Meizu, Huawei and Sony
  - [Muslim Pro Help](https://support.muslimpro.com/hc/en-us/articles/115001927207)
- **Battery handling, Mawaqit:** add Mawaqit to Battery > Unmonitored apps, enable priority notifications, keep the app on internal storage (not an SD card), check silent mode, then restart — [Mawaqit Help](https://help.mawaqit.net/en/articles/12230364-troubleshooting-the-adhan-on-android-phones)
- **General vendor guidance:** Doze can silently block adhan notifications, and Xiaomi, Huawei and Oppo also need auto-start enabled — [search summary: Muslim Expert, pakdata, Masjidbox guides](https://www.muslim-expert.app/en/blog/configure-athan-notifications)
- **Android 14 platform rule:**
  - `SCHEDULE_EXACT_ALARM` is **denied by default** for new installs of apps targeting API 33+
  - `setExact`, `setExactAndAllowWhileIdle` and `setAlarmClock` all require it
  - `USE_EXACT_ALARM` (granted on install) is allowed only for calendar or alarm-clock apps under Play policy
  - Apps on the power allowlist (requested via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) are exempt
  - Apps must check `canScheduleExactAlarms()` and listen for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`
  - [Android Developers](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- **Complaint data:**
  - "Adhan notifications failing on Android" is one of the five cross-app complaint themes
  - For Athan specifically: "ads around the adhan" and "adhan unreliable on Android"
  - [Unstar, Jun 2026](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026) (vendor; no numbers)

### Inferences
- Every major app pushes the battery problem onto the user through help-center articles. A small app that walks the user through setup in-app stands out: check `canScheduleExactAlarms()`, request the battery-optimization exemption, detect the OEM (Xiaomi/Huawei/Samsung) and deep-link to auto-start, and test the adhan sound. Mawaqit has started doing this with its background-running prompt.
- Whether a prayer app qualifies for `USE_EXACT_ALARM` is uncertain, because it is not named as a calendar or alarm-clock app. The safe path is `SCHEDULE_EXACT_ALARM` with a clear request, plus `setAlarmClock` for the adhan.
- Mawaqit's notification-channel pitfall shows why the adhan channel and audio stream should be explicit and visible to the user. Al-Moazin's "follows ringer mode" is one documented alternative.

### Gaps
- Muslim Pro's and Athan's exact muezzin counts and per-prayer settings on Android 2026 were not found.
- No source confirmed which apps hold `USE_EXACT_ALARM` vs `SCHEDULE_EXACT_ALARM`. An Exodus report for `com.haz.prayer` exists but was not read: [Exodus](https://reports.exodus-privacy.eu.org/en/reports/551844)
- No documentation was found for an automatic-silent feature in Muslim Pro or Athan.

## 4. Widgets

### Takeaway
Widget sets range from Muslim Pro's ten types, where the prayer-times widget is locked behind an ad or Premium for free users, to single-purpose countdown widgets (Al-Moazin, My Prayer). Common patterns:
- a full day list
- a next-prayer countdown or "time between prayers" bar
- a combined Hijri date (Mawaqit, Deen, Prayer Now)
- lock-screen widgets (Muslim App)
- Wear OS tiles and complications (Prayer Now, Salaat First, My Prayer, Al-Moazin, Sajda, Mawaqit)

Sizes and visuals are poorly documented. Users report stale or frozen widgets.

### Cited Findings
- **Muslim Pro (Android):** ten home-screen widgets: All Prayer Times with Tracker, Daily Prayers Tracker, Qibla, Qalbox Navigation, Quran, Fasting Tracker, Daily Inspiration, Ask AiDeen, Duas and Tasbih. **Free users see a padlock and must watch an ad to unlock the times.** Premium users see them directly. Tick-to-track prayers is Premium-only — [Muslim Pro Help: Android widget](https://support.muslimpro.com/hc/en-us/articles/34591554495641-Homescreen-Updated-Widget-Android); "see all prayer times in the widget" is a Premium item — [Muslim Pro Help](https://support.muslimpro.com/hc/en-us/articles/7967476840729-What-is-the-difference-between-the-free-and-Premium-Versions-of-Muslim-Pro)
- **Athan (IslamicFinder):** a home-screen prayer-times widget and "Dua of the Day" — [AppGoblin](https://appgoblin.info/apps/com.athan)
  - Conflict on whether the widget is paid: islamtics says the Android widget "moved behind a premium paywall in 2025" — [islamtics](https://islamtics.com/?p=21177) (competitor-written). An app-store description says the widget is "completely free for all users" — [search summary](https://adapty.io/paywall-library/athan-prayer-times-al-quran/)
  - Reviews describe the next-prayer widget freezing, showing stale times or disappearing after updates — [Unstar](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)
- **Prayer Now:** customizable home-screen widgets for prayer times and the Islamic calendar, plus five Wear OS tiles — [AppGoblin](https://appgoblin.info/apps/com.AppRocks.now.prayer)
- **Salaat First:** "multiple widgets", plus a Wear OS tile and complication — [AppGoblin](https://appgoblin.info/apps/org.hicham.salaat)
- **My Prayer (Saudi):**
  - Phone: a widget for today's prayer times, and a **horizontal widget showing the time between prayers**
  - Wear OS: watch faces, a tile, and complications for next prayer, time remaining, and Hijri plus Gregorian date. Two watch faces are sold as separate apps
  - [AppGoblin](https://appgoblin.info/apps/com.haz.prayer)
- **Al-Moazin Lite:** a simple widget with the time remaining to the next prayer, and a Wear OS tile with today's times — [AppGoblin](https://appgoblin.info/apps/com.parfield.prayers.lite)
- **Mawaqit:** a home-screen widget with prayer times and the Hijri date, plus Wear OS and Android TV — [AppGoblin](https://appgoblin.info/apps/com.kanout.mawaqit)
- **Muslim App (Assistant App):** prayer-times widgets for the **home and lock screens**, a daily-verse Quran widget, and Wear OS — [AppGoblin](https://appgoblin.info/apps/com.hundred.qibla)
- **Pillars:** home-screen and lock-screen widgets plus iOS Live Activities for the next prayer — [thepillarsapp.com](https://thepillarsapp.com/). The Android description lists a home-screen widget, with "more widgets" planned — [AppGoblin](https://appgoblin.info/apps/com.pillars.pillars). Reviewers praise its widgets — [JustUseApp via search](https://justuseapp.com/en/app/1559086853/pillars-prayer-times-qibla/reviews)
- **Sajda:** widgets plus Wear OS; details not listed — [AppGoblin](https://appgoblin.info/apps/com.raimbekov.android.sajde)
- **Deen (Let's Flutter):** a home-screen widget showing the Hijri date — [Apptopia](https://apptopia.com/google-play/app/com.letsflutter.deen/about)
- **Al-Azan (open source):**
  - Home-screen widgets and a notification (status-bar) widget
  - After Isha, the Fajr widget highlights the *next* day's Fajr
  - Custom date format
  - [F-Droid](https://f-droid.org/en/packages/com.github.meypod.al_azan/)
- **iOS idea worth noting:** Deen Tracker (iOS) advertises 15+ widgets with no ads, sold as a lifetime unlock — [App Store via search](https://apps.apple.com/app/id6756148134)

### Inferences
- Muslim Pro's "watch an ad to see the times in the widget" is the clearest anti-pattern on the market. A free, always-current widget showing the full day list, next prayer and countdown would be a real differentiator.
- Widget ideas with low cost and high value, documented in competitors:
  - a "time between prayers" bar (My Prayer)
  - rolling to tomorrow's Fajr after Isha (Al-Azan)
  - Hijri date on the same widget (Mawaqit, Deen, Prayer Now)
  - a lock-screen or notification widget (Muslim App, Al-Azan)
- Stale or frozen widgets (Athan) suggest widget updates tied to background work that gets killed. A widget driven by a scheduled time, which updates exactly at prayer boundaries, avoids this.

### Gaps
- No source gave widget sizes (2x1, 4x2…) or screenshots for any app. This needs a hands-on check on a device.
- The current status of Athan's widget paywall (2025 claim vs "free for all users") is unresolved.

## 5. Features users love, common complaints, and the privacy story

### Takeaway
The most common complaints are ads in a religious app (especially around the adhan), paywalled sacred content, prayer times that don't match the local mosque, failed adhans on Android, feature bloat, and privacy fears. Privacy became a selling point after two scandals. In November 2020 Muslim Pro was found sending location data to X-Mode, which sold to US military contractors. In January 2021 Salaat First was found sending precise location to Predicio. Pillars, launched in that climate, markets "calculated on your phone, no ads, no account".

### Cited Findings
- **Muslim Pro / X-Mode (2020):**
  - Motherboard/Vice found Muslim Pro's Android and iOS apps sending granular location data to X-Mode, a broker that sold to US defense contractors
  - X-Mode was not named in the app's privacy policy
  - Muslim Pro called the report "incorrect and untrue" but ended all its data partnerships, including X-Mode, which had started four weeks earlier
  - Apple and Google banned X-Mode from their stores in December 2020
  - [Vice](https://www.vice.com/en/article/muslim-pro-location-data-military-xmode); [Vice follow-up](https://www.vice.com/en/article/muslim-apps-location-data-military-xmode); [SoyaCincau, Nov 18, 2020](https://soyacincau.com/2020/11/18/did-muslim-pro-sell-user-data-to-the-us-military-heres-what-we-know-so-far/)
  - Sources differ on the publication date: Nov 16 per Christian Century, Nov 18 per Gigazine — [Gigazine](https://gigazine.net/gsc_news/en/20210129-musulim-app-lacation-data-x-mode)
  - Five more Muslim prayer apps were later reported selling data to X-Mode — [The New Arab](https://www.newarab.com/news/more-muslim-apps-selling-data-us-military-linked-x-mode)
- **Salaat First / Predicio (Vice, Jan 11, 2021):**
  - Data sent: precise latitude and longitude (roughly every 2 minutes), phone model, OS, IP address, timestamp and advertising ID
  - Collection applied only to users who installed in the UK, Germany, France or Italy; the app had 10M+ Android downloads at the time
  - Predicio was linked to Gravy Analytics and Venntel, a US government contractor selling to ICE and CBP. It is unclear whether Salaat First data reached them
  - The developer suspended collection in Oct 2020 and ended the deal on Dec 6, 2020. He says app revenue goes to charity
  - [Vice](https://www.vice.com/en/article/muslim-app-location-data-salaat-first/)
- **Comparitech study** (175 Muslim Android apps; page last updated Oct 20, 2023, so older):
  - **Pillars** was the top recommendation: no ads, limited permissions, no serious leaks, and data stays on the phone
  - Muslim Pro had 4 leaks: a plain-text password, a Firebase URL, a generic API key and a Google API key. The company said these were by design or from an ad SDK
  - Salaat First's privacy policy was hosted on a server without a valid SSL certificate, and an opt-out exists in settings
  - [Comparitech](https://comparitech.com/blog/vpn-privacy/muslim-prayer-app-study)
- **Pillars' origin and claims:** the developers removed all ads as inappropriate for a prayer app — [The Muslim Vibe via search](https://themuslimvibe.com/western-muslim-culture/meet-the-founders-of-the-pillars-app-the-young-muslim-duo-behind-an-innovative-new-islamic-app). The app was covered as "a privacy-focused Muslim prayer app" created by students — [BuzzFeed News (title only)](https://www.buzzfeednews.com/article/ikrd/pillars-app). It claims no ads, no tracking, no data selling, no account, and on-device calculation — [thepillarsapp.com](https://thepillarsapp.com/). Caveat: its Android build includes Firebase analytics, Sentry and subscription SDKs — [AppGoblin](https://appgoblin.info/apps/com.pillars.pillars)
- **Arab-media angle:**
  - iPhoneIslam (Sept 2025) warned that some prayer apps link location to identity, using a screenshot of "Muslim: Prayer Times & Qibla"
  - It advised avoiding apps that require login and checking privacy labels, and recommended ElaSalaty (i4islam) as keeping data on the device
  - Reader comments also named Muslim Pro and Al-Musalli, unverified
  - [iPhoneIslam](https://iphoneislam.com/en/2025/09/safe_prayer_time_app_for_muslims/160957)
- **Complaint themes 2026** (Unstar, a vendor; no counts):
  - Muslim Pro: ad volume (dominant), the 2020 data legacy, upgrade prompts, bloat and slowdowns
  - Athan: ads around the adhan, accuracy after travel, unreliable adhan on Android, widget freezes
  - Pillars: smaller library, subscription, edge cases in prayer times
  - Cross-app: ads in a religious app, privacy fears, times not matching mosques, sacred content paywalled, adhan failing on Android
  - User quotes: "ads here, of all places" and "charging for the Quran"
  - [Unstar](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)
- Most-cited Athan complaint: ads loading at the moment users open the app to check or hear the adhan — [search summary of review roundup](https://www.fiveprayer.app/blog/athan-app-alternative) (vendor)
- Long-time Muslim Pro users say the app feels heavier and slower after it added halal restaurants, a community feed and commerce — [search summary](https://www.fiveprayer.app/blog/muslim-pro-alternative) (vendor). Muslim Pro now also bundles Qalbox video, AiDeen AI Q&A and Umrah booking — [AppGoblin](https://appgoblin.info/apps/com.bitsmedia.android.muslimpro); [Muslim Pro Help widget](https://support.muslimpro.com/hc/en-us/articles/34591554495641-Homescreen-Updated-Widget-Android)
- **Features users love:**
  - Pillars: prayer tracker with streaks, menstruation mode that keeps the streak, missed-fast log, PrayFirst app-blocker until the prayer is logged, Fajr Smart Alarm — [thepillarsapp.com](https://thepillarsapp.com/). Reviewers call it clean, minimal and calming, and praise the widgets — [JustUseApp via search](https://justuseapp.com/en/app/1559086853/pillars-prayer-times-qibla/reviews)
  - Prayer Now: auto-silent during prayer and flip-to-mute — [AppGoblin](https://appgoblin.info/apps/com.AppRocks.now.prayer)
  - Mawaqit: the mosque's real iqama times — [AppGoblin](https://appgoblin.info/apps/com.kanout.mawaqit)
  - iPray: colour-coded countdowns and offline use — [Al-Ain](https://al-ain.com/article/ramadan-2026-apps)
  - Athan: adhan works in airplane mode — [islamtics](https://islamtics.com/?p=21177)

### Inferences
- Users treat "no ads + no account + on-device calculation + small SDK footprint" as a trust signal. AppGoblin's SDK lists are a cheap public way to check that claim. Mawaqit, My Prayer and Sajda pass largely; Muslim Pro, Muslim App and Athan Pro carry many ad and attribution SDKs.
- Even privacy-branded apps ship analytics and paywall SDKs (Pillars: Firebase, Sentry, RevenueCat, Superwall). A truly zero-tracker app, like Al-Azan with no internet permission at all, is a stronger and verifiable claim.

### Gaps
- No quantified share of complaint types (for example the percentage of reviews about missed adhan) from an independent source.
- The current state of Muslim Pro's data sharing after 2020 is known only from its own statements and competitor blogs.

## 6. Which app is "best": overall, privacy, Saudi/Gulf accuracy, design

### Takeaway
No single independent authority ranks these apps. Most 2026 "best of" lists are written by competing vendors (islamtics, FivePrayer, Unstar). From the evidence:
- **Overall (feature-complete):** Muslim Pro by reach; Athan by IslamicFinder by rating among the big ad-supported apps
- **Privacy:** Pillars by reputation (Comparitech's pick); Al-Azan (open source, no internet permission) by verifiable design
- **Accuracy in Saudi Arabia and the Gulf:** any app set to Umm al-Qura (or Qatar/Kuwait/Dubai), or Mawaqit where local mosques publish times; Saudi-made My Prayer is ad-free and Umm al-Qura-first
- **Design:** Sajda (4.9) and Pillars are the apps most praised for clean design

### Cited Findings
- islamtics ranks its own app #1, Pillars #3, Athan #4 (held back by "banner ads, widget now paywalled") and Muslim Pro #9 ("lowest rating, heavy ads, expensive premium, documented 2020 data-sharing incident"). It is a conflicted source: written by the founder of islamtics — [islamtics, May 22, 2026](https://islamtics.com/?p=21177)
- Unstar gives use-case verdicts rather than a ranked order and has a commercial conflict (sponsored placements) — [Unstar, Jun 21, 2026](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)
- Comparitech's privacy recommendation is Pillars, and it also names Daily Islam, 1Muslim, Deen Islamic App and iPray as respecting privacy. The page dates from 2023 — [Comparitech](https://comparitech.com/blog/vpn-privacy/muslim-prayer-app-study)
- Al-Azan: no internet permission, no trackers, open source — [F-Droid](https://f-droid.org/en/packages/com.github.meypod.al_azan/)
- Highest Play ratings with large bases: Sajda 4.9 (466K), Athan 4.83 (333K), Mawaqit 4.78 (174K), Prayer Now 4.72 (1.5M), Salaat First 4.68 (800K) — [AppGoblin pages cited above](https://appgoblin.info/apps/com.raimbekov.android.sajde)
- Gulf method details: Umm al-Qura Isha is 90 min (plus 30 in Ramadan); Qatar is a modified Umm al-Qura; Dubai and Kuwait have their own angles — [adhan-java](https://github.com/batoulapps/adhan-java)
- Apps listing Umm al-Qura explicitly: My Prayer (first in its list), Al-Moazin ("Umm al-Qura, Makkah"), Salaat First (first of 16), Athan Pro — [AppGoblin My Prayer](https://appgoblin.info/apps/com.haz.prayer); [AppGoblin Al-Moazin](https://appgoblin.info/apps/com.parfield.prayers.lite); [AppGoblin Salaat First](https://appgoblin.info/apps/org.hicham.salaat); [AppGoblin Athan Pro](https://appgoblin.info/apps/com.quanticapps.athan). My Prayer also includes the Kuwait Awqaf method — [AppGoblin](https://appgoblin.info/apps/com.haz.prayer)
- The official KACST Umm al-Qura app (iOS) has almost no ratings (5), so it is not a popular choice — [App Store](https://apps.apple.com/app/id6752710662)

### Inferences
- "Best for the Gulf" is about defaults, not algorithms. The math is the same in every app. The app that sets Umm al-Qura (Saudi), Qatar, Kuwait or Dubai automatically from the location, and applies the Ramadan Isha rule, will look most accurate to Gulf users.
- Ideas a small native Android app could combine, each proven by one competitor:
  - Salaat First's breadth of methods with an automatic method per country (Pillars)
  - per-prayer full adhan / short / beep / silent options (IslamicFinder, Pillars)
  - a separate Fajr adhan (Al-Azan)
  - auto-silent and flip-to-mute (Prayer Now)
  - an iqama offset (Prayer Now, Mawaqit)
  - a free full-day widget with countdown and Hijri date
  - an in-app battery and exact-alarm setup wizard
  - no ads and no trackers (Mawaqit, My Prayer, Al-Azan)

### Gaps
- No independent, non-vendor 2025–2026 comparison or Reddit consensus was found; the Reddit search returned no threads.
- No Gulf-specific accuracy benchmark against official Saudi Umm al-Qura published times was found.
- Arab tech sites (AITNews, Arageek, Tech-wd) did not surface relevant 2025–2026 comparisons in the searches run.
