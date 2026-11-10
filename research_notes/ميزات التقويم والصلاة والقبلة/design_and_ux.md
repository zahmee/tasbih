# Design, Look and User Experience of the Best Islamic Utility Apps (Prayer Times, Hijri, Qibla), as of late 2026

Scope note: Android focus, iOS only for notable ideas. Research done 2026-11-10. Much of the available material is store listings, vendor help pages, and review aggregators, and some "best app" roundups are written by competing app makers who rank themselves first. Each such source is flagged. Information from before 2024 is marked [OLDER].

## 1. Which apps win design praise in 2024-2026, and what exactly is praised

### Takeaway
No prayer app could be confirmed as a Google Play "Best of" winner, Apple Design Award winner, or Play Editor's Choice in 2024-2026. Pillars gets the most consistent design praise ("calm", "modern", "clean", ad-free). The praised qualities are calm color, generous whitespace, modern typography, small micro-interactions, a single clear next-prayer countdown, and no ads. Feature-heavy apps (Muslim Pro, Athan, Quran Majeed) are praised for breadth, not looks.

### Cited Findings
- Pillars: the App Store listing says "adverts don't belong on a Prayer app" and calls the app elegant with a "Beautiful Design". It has 4.8 stars from 5.6K ratings (US store, viewed Nov 2026). Reviewers call it "simple and gorgeous" and "clean and aesthetic". No editorial award or feature is shown, only a #186 chart position in Reference. — [App Store: Pillars](https://apps.apple.com/us/app/-/id1559086853)
- Pillars' interface is described as "modern typography, generous whitespace, satisfying micro-interactions". Caveat: the publisher, islamtics, is a competing app that ranks itself first. — [islamtics, updated 22 May 2026](https://islamtics.com/best-islamic-apps-android/)
- An analysis of 1-3 star reviews (June 2026) found users praise Pillars for a "modern and calm" design, speed and an ad-free experience, and Athan for "a clear next-prayer countdown". Muslim Pro is praised mainly for breadth of features and its ad-free Premium, not for visual design. — [Unstar, 21 Jun 2026](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)
- Pillars' own site says reviewers call its colors "calming" and its design "sublime". It is on both the App Store and Google Play. — [thepillarsapp.com](https://thepillarsapp.com/)
- The Pillars founders say they aimed for a "clean, modern, and minimalist design" that is extremely simple to use. — [The Muslim Vibe](https://themuslimvibe.com/western-muslim-culture/meet-the-founders-of-the-pillars-app-the-young-muslim-duo-behind-an-innovative-new-islamic-app)
- One search-engine summary of Pillars reviews included a reviewer calling its widget "one of the best looking iOS widgets". This was not verified on the page itself. — [justuseapp Pillars reviews](https://justuseapp.com/en/app/1559086853/pillars-prayer-times-qibla/reviews)
- Muslim Pro redesign (2025): the App Store changelog lists "New Home Page: Track prayers, Quran, & check-ins with interactive cards", "Widget Upgrades: Smarter logic & fresh look", "Prayer Times Glow-Up: Stunning new design with improved accuracy", and "Performance Boost". These are vendor claims. — [App Store: Muslim Pro](https://apps.apple.com/us/app/muslim-pro-quran-prayer-times/id388389451)
- Muslim Pro's help center has a "Guide to the Updated and Enhanced App" describing a new "Today page, with the prayer time and tracker displayed prominently at the top". This quote comes from a search snippet, because the fetched page showed only an index. — [Muslim Pro Help Center](https://support.muslimpro.com/hc/en-us/articles/49726715851545-Guide-to-the-Updated-and-Enhanced-App)
- A Google Play event page for a Ramadan "major update" to Muslim Pro promises a cleaner layout with less clutter. This is promotional wording. — [Google Play event](https://play.google.com/store/apps/eventdetails/4829063291664526729)
- Sajda is listed as an App Store "Editor's Choice" in Lifestyle by an aggregator. This is an iOS claim and was not verified on Apple's side. — [appshunter.io](https://appshunter.io/ios/topics/sajda)
- iPray is described as a minimalist, prayer-only app: "Some users prefer it; others find it sparse." — [islamtics](https://islamtics.com/best-islamic-apps-android/). An Android stats page calls it "beautifully designed", and one user review praises it as the "Best Adhan app with NO Advertisements". — [android-stats iPray](https://android-stats.com/d/com.guidedways.ipray)
- Bilal (Arabic market, iOS) markets itself as free, with no ads at all, and an "elegant and simple" design with a home-screen widget. — [App Store: Bilal](https://apps.apple.com/us/app/-bilal/id1461237208)
- Al-Azan (open source, Android, F-Droid) offers light and dark themes with Material You dynamic colors. It is ad-free and tracker-free and has no internet permission. Latest version 2.4.1 is dated 6 Nov 2026. — [F-Droid: Al-Azan](https://f-droid.org/en/packages/com.github.meypod.al_azan)
- Google Play's Best of 2025 page was found, but no Islamic or prayer app could be confirmed on it. — [Google Play Best of 2025](https://play.google.com/best-of-2025); [TechRadar list](https://www.techradar.com/phones/these-are-the-10-best-android-apps-of-the-year-according-to-google)
- iOS trend (2025): MasjidBox's September 2025 changelog says its iOS widgets "embrace modern design aesthetics with liquid glass compatibility". — [MasjidBox changelog](https://masjidbox.com/changelog/enhanced-ios-widgets-and-user-experience-improvements)

### Inferences
- The category's design leader is the "calm, single-purpose, ad-free" app (Pillars, iPray, Bilal, Al-Azan), not the super-app. The praise vocabulary is "calm", "clean", "simple", "no ads" much more than "Islamic art" or "ornament".
- Removing ads appears to be counted as part of "good design" by users in this category.
- Visual polish is now expected on both platforms: Material You on Android and Liquid Glass on iOS 26.

### Gaps
- No confirmed Apple Design Award, Google Play Best of 2024/2025, or Play Editor's Choice for any prayer app was found. Absence was not proven either, because the Best of 2025 page did not list its apps in the fetch.
- No independent design critique was found (for example from a design publication) of Pillars, Sajda, Mawaqit, Athan Pro or Salaat First. Most praise is from reviews and vendor or competitor pages.
- No source found discussing praise for Islamic geometric art or calligraphy specifically in 2024-2026 apps.
- One search summary linked "mandatory, non-skippable onboarding and aggressive paywalls" and "4.6/5 from 9.1K reviews, polarized" to Pillars. The underlying URL is about a different package (com.muslimplus.muslimplus), so the attribution is unverified and left out. — [marlvel.ai](https://marlvel.ai/intel-report/reference/com-muslimplus-muslimplus)

## 2. Home screen layout patterns (next prayer, countdown, five prayers list, dates, sunrise, day progress)

### Takeaway
The dominant layout puts the next prayer first: next prayer name and time with a live countdown as the hero, then today's prayer list. Common additions are elapsed time since the current prayer, a sun-arc or sundial showing progress through the day, and a prayer tracker (check-offs and streaks) near the top. Heavy feeds below the fold are criticized.

### Cited Findings
- The common "Muslim Pro-style" pattern is a today view with the five prayers and "a countdown to the next one as the hero". — [vp0.com SwiftUI clone write-up](https://vp0.com/blogs/muslim-pro-prayer-times-ui-clone-swiftui/)
- Muslim Pro's 2025 Today page puts "the prayer time and tracker displayed prominently at the top", with interactive cards for prayers, Quran and check-ins. — [Muslim Pro Help Center](https://support.muslimpro.com/hc/en-us/articles/49726715851545-Guide-to-the-Updated-and-Enhanced-App); [App Store: Muslim Pro](https://apps.apple.com/us/app/muslim-pro-quran-prayer-times/id388389451)
- Muslim Pro's dashboard shows "the next prayer time, daily progress, and quick access to features like the Quran and Duas". A design-flow analysis critiques it: "The main dashboard is dense with information and feature icons", which may overwhelm new users. — [ScreensDesign: Muslim Pro](https://screensdesign.com/showcase/muslim-pro-quran-athan-prayer)
- Salaty's clock shows "the remaining time till upcoming prayers and the elapsed time of the current prayer". — [App Store: Salaty](https://apps.apple.com/app/id1494640818)
- Glassify's listing mentions a "time elapsed" indicator since the last prayer. — [App Store: Glassify](https://apps.apple.com/app/id6741079325)
- Sun-arc pattern: one app (Siraj, per the search summary) describes "a sun arc that tracks your progress from Fajr to Isha". The URL is the one the search returned for that listing. — [App Store listing id6759178047](https://apps.apple.com/app/id6759178047)
- Awqāt uses a "digital sundial" to visualize sunrise, zenith, sunset and twilight sky positions. — [App Store: Awqāt](https://apps.apple.com/us/app/awq%C4%81t/id6461305834)
- Pillars' home centers on tracking: "Mark each prayer as you go and see your days build", with on-time or "caught up with later" states. — [thepillarsapp.com](https://thepillarsapp.com/)
- Mawaqit's home page holds a per-prayer notification icon: the dome means full adhan and the bell means a beep. — [Mawaqit help, 9 Sep 2025](https://help.mawaqit.net/en/articles/12230364-troubleshooting-the-adhan-on-android-phones)
- Al-Azan lets users show or hide optional times (Sunrise, Sunset, Midnight, Tahajjud) next to the five prayers. In "maghrib lunar mode" it highlights the next Fajr after Isha. — [F-Droid: Al-Azan](https://f-droid.org/en/packages/com.github.meypod.al_azan)
- Complaints about clutter: Muslim Pro users cite "clutter and slowdown" from added restaurants, a community feed and commerce. — [Unstar](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026). A March 2025 Android review says: "After the last update, the app has become very complicated, the amount of ads has increased terribly." This is from a search snippet of the aggregator page. — [unitQ Muslim Pro](https://unitq.com/unitq-scorecards/muslimproramadan2023)

### Inferences
- Reusable layout: (1) header with Hijri and Gregorian dates and location; (2) hero card with the next prayer name, its time, and a live countdown, optionally "X min since [current prayer]"; (3) a vertical list of the five prayers, with the current or next one highlighted and each row holding its own alert-type icon (Mawaqit's dome or bell); (4) sunrise shown as a secondary or muted row because it is not a prayer; (5) an optional sun-arc that visualizes the day.
- A per-row alert icon on the home screen, as in Mawaqit, lets users change alerts without going into settings.
- Keep content feeds and commerce off the prayer home. Clutter is a named reason for complaints.
- The Al-Azan "after Isha, point to tomorrow's Fajr" behavior handles the end-of-day edge case that a naive "next prayer" display gets wrong.

### Gaps
- No source showed how the top apps visually distinguish sunrise from prayers; the inference above is mine.
- No source showed the exact visual way the current prayer is marked (color, bar, bold). Screenshots were not accessible as text.
- No Android-specific home-layout study found.

## 3. Widget designs users love (countdown, one-row five prayers, Hijri and Gregorian, theming, sizes, Arabic readability)

### Takeaway
The most-valued widgets are a next-prayer widget with time remaining, a full-day prayer list, and a Hijri date widget. Users are angry when widgets go behind a paywall or show stale times. On Android, the modern expectation is Material You dynamic color. On iOS it is Lock Screen widgets, Live Activities and Liquid Glass.

### Cited Findings
- Nimaz (open-source Android app, Kotlin, Jetpack Compose, Material 3, widgets built with Jetpack Glance) has:
  - a Hijri date widget showing today's Hijri date with a Gregorian reference
  - a prayer-times widget with the Hijri date in its header
  - a next-prayer widget showing time remaining
  - a minimal variant that shows only the next prayer "with a colored badge showing time remaining"
  - widget colors that follow Material You dynamic theming on Android 12+

  These details come from search snippets, because the docs pages now return HTTP 410. — [Nimaz docs: widgets overview](https://www.mintlify.com/arshad-shah/Nimaz/widgets/overview); [next-prayer widget](https://www.mintlify.com/arshad-shah/Nimaz/widgets/next-prayer-widget); [prayer-times widget](https://www.mintlify.com/arshad-shah/Nimaz/widgets/prayer-times-widget)
- Al-Azan offers home-screen and notification widgets with Material You colors. Version 2.4.1 (Nov 2026) adds a custom widget date format. — [F-Droid: Al-Azan](https://f-droid.org/en/packages/com.github.meypod.al_azan)
- Hijri Today (Dec 2025) is a Hijri-only Android widget built with Compose and Glance and MIT-licensed. It refreshes at 6 PM and at midnight using exact alarms, a compromise between timeliness and battery. A language change triggers an immediate refresh. The author notes that "battery efficiency remains difficult, even with modern Android APIs". — [DEV Community: Hijri Today](https://dev.to/cas8398/building-hijri-today-a-widget-my-mom-actually-uses-32c8)
- IslamicWidget offers an adjustable +/- day offset for the Hijri date. — [IslamicWidget repo mirror](https://gh.r-mj.com/cyberzilla/IslamicWidget)
- Pillars: "Home Screen widgets, the Lock Screen and Live Activities keep the next prayer in view without opening the app." The FAQ lists widgets among the free core features. — [thepillarsapp.com](https://thepillarsapp.com/). This contradicts an App Store review saying the widget "is now locked behind a paywall", calling it "arguably the most useful feature". — [App Store: Pillars](https://apps.apple.com/us/app/-/id1559086853)
- Widget paywall backlash, with conflicting sources on which app: islamtics says Athan moved its Android prayer-times widget behind a paywall in 2025, which drew criticism. — [islamtics](https://islamtics.com/best-islamic-apps-android/). A search-engine summary attributes a 2025 widget paywall to Quran Majeed instead. — [islamtics search result](https://islamtics.com/?p=21177). Both come from a competitor and are unverified.
- Widget reliability complaints: Athan's next-prayer widget "freezes, shows stale times, or disappears after updates". — [Unstar](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026). An Athan Pro user said the widget showed incorrect times and did not update after edits, and that the widget was the reason they bought Pro. — [justuseapp Athan Pro reviews](https://justuseapp.com/en/app/1098618656/athan-pro/reviews). A Sajda user reported "Widget funktioniert nicht" (widget does not work) on an iPhone 13. — [App Store DE: Sajda](https://apps.apple.com/de/app/sajda/id517254891)
- iOS ideas:
  - MasjidBox added a Lock Screen widget that shows all prayer times for the full day, with Liquid Glass styling (Sep 2025). — [MasjidBox changelog](https://masjidbox.com/changelog/enhanced-ios-widgets-and-user-experience-improvements)
  - Sajda added a Live Activity for morning and evening adhkar ("track your progress in real time"), a Ramadan Live Activity with a Lock Screen countdown, and an Apple Watch widget showing "all prayer times at a glance". — [App Store: Sajda](https://apps.apple.com/us/app/-/id517254891)
- Muslim Pro has "Homescreen Updated Widget - Android" and "- iOS" help articles (2025 refresh) and advertises "smarter logic" in widgets. It also uses a contextual modal to suggest adding a widget after onboarding. — [Muslim Pro Help Center](https://support.muslimpro.com/hc/en-us/articles/49726715851545-Guide-to-the-Updated-and-Enhanced-App); [ScreensDesign](https://screensdesign.com/showcase/muslim-pro-quran-athan-prayer)
- Dribbble: the "GrabDaily - Prayer Time Widget" shot has 110 likes and 24.8k views (date not shown). — [Dribbble: prayerapp tag](https://dribbble.com/Creative-Box/tags/prayerapp)

### Inferences
- Widget set worth offering: next prayer with countdown (small, 2x1 or 2x2); full day list in one row (4x1); full day list plus Hijri and Gregorian header (4x2); a Hijri-only date widget.
- Keep the widget free. Paywalling a widget that was free is one of the most resented moves in this category.
- Staleness is the top widget complaint. The widget must update at each prayer boundary and after setting changes or location changes. "Freezes after update" suggests apps must re-register widget updates after an app update.
- The Hijri day begins at maghrib, which is why Hijri Today refreshes at 6 PM. A better design ties the refresh to the computed Maghrib time and offers a +/- day offset for local moon sighting.
- Android has no Live Activity, but an ongoing notification (Al-Azan's "notification widget") gives a similar glanceable countdown.

### Gaps
- No user-sentiment data found on transparency, specific sizes (1x1, 2x1, 4x1, 4x2), or Arabic readability inside widgets.
- No source on how widgets render Arabic-Indic digits or right-to-left prayer rows.

## 4. Onboarding: location, notifications, exact alarms, battery optimization

### Takeaway
The best apps keep setup short, use location on the device only, offer manual city search, ask for notification permission on Android 13+, and treat battery optimization as its own explained step, with a warning banner and a "test adhan" button later. Super-apps put a paywall (sometimes with a countdown timer) and a burst of permission pop-ups in onboarding, and reviewers call this friction.

### Cited Findings
- Muslim Pro onboarding shows a paywall with a countdown timer that "creates urgency from the start". Afterwards "the user is immediately met with multiple permission requests and pop-ups (paywall, tracking, widgets)". Later it uses contextual modals: "Instead of asking for everything upfront, the app uses modals to suggest features." — [ScreensDesign: Muslim Pro](https://screensdesign.com/showcase/muslim-pro-quran-athan-prayer)
- "Muslim: Prayer Times, Qibla" uses a hard paywall. After permissions, the user is shown a 3-day free trial for an annual subscription. — [ScreensDesign: Muslim](https://screensdesign.com/showcase/muslim-ramadan-2026-azan). Quran Widgets uses a soft paywall after "a long and involved onboarding process". — [ScreensDesign: Quran Widgets](https://screensdesign.com/showcase/quran-widgets-verses-duas)
- Pillars FAQ, on whether an account is needed: "No. Download it and it just works." Location "is used to calculate the direction on your phone and goes no further". — [thepillarsapp.com](https://thepillarsapp.com/)
- Nimaz onboarding:
  - It explains that Android 13+ needs explicit notification permission, or prayer alerts will not arrive.
  - It explains that battery optimization "may cause missed prayer notifications".
  - Its settings show a warning banner with a button to open battery settings when the app is not exempted.
  - It has "Test Notification" buttons.

  These details come from search snippets. — [Nimaz quickstart](https://www.mintlify.com/arshad-shah/Nimaz/quickstart); [Nimaz notifications](https://www.mintlify.com/arshad-shah/Nimaz/customization/notifications)
- Mawaqit's newest version prompts users to allow background running. Its help article also walks through: the notification channel not muted, priority notifications on, the app on internal storage, and adding it to Samsung's "Unmonitored apps". It does not mention exact alarms. — [Mawaqit help, 9 Sep 2025](https://help.mawaqit.net/en/articles/12230364-troubleshooting-the-adhan-on-android-phones)
- A verification step: schedule a test adhan 2-3 minutes ahead, lock the screen, and wait. — [Muslim Expert guide](https://www.muslim-expert.app/en/blog/configure-athan-notifications)
- Other setup checks: exclude the app from battery optimization, allow exact alarms if the phone asks, and make sure the notification channel is not silent. — [apkcombo article](https://apkcombo.com/articles/prayer-times-quran-apps/)
- PrayerAlarm (open source) asks to be added to Samsung's "Never sleeping apps" list during setup. — [GitHub: PrayerAlarm](https://github.com/FP49/PrayerAlarm)
- Quran Majeed keeps per-manufacturer guides (Huawei, Pixel, OnePlus, Samsung). It notes Huawei's battery optimization makes apps "doze off shortly after the screen goes off". — [Pakdata: Huawei fix](https://pakdata.com/fix_adhan_notification/huawei); [Pakdata generic](https://pakdata.com/fix_adhan_notification/generic)
- Muslim Pro has a dedicated "Android Battery Settings for Adhan Feature" help article. — [Muslim Pro Help](https://support.muslimpro.com/hc/en-us/articles/360040773471-Android-Battery-Settings-for-Adhan-Feature)
- iOS contrast: Muslim Pro's help says iOS adhan notifications stop after a few days unless the app is opened at least once a week. — [Muslim Pro Help (iOS)](https://support.muslimpro.com/hc/en-us/articles/200664340-The-Adhan-notifications-have-stopped-working-after-a-few-days-iOS-only)
- Manual location: Al-Azan lets users search a location offline or take it from GPS, and it has no internet permission at all. — [F-Droid: Al-Azan](https://f-droid.org/en/packages/com.github.meypod.al_azan). Salam's support page advises enabling Auto Location or manually selecting the city after moving. — [Salam support](https://yasoob.me/support/salam/)
- Salaty asks for location and notification permissions on first launch. — [Salaty review, Apr 2026](https://sarhday.blogspot.com/2026/04/salaty-app.html)

### Inferences
- Recommended order: (1) a value screen with no account; (2) location, with a visible "Choose city manually" option and a one-line promise that location stays on the phone; (3) notification permission, explained as "so the adhan can reach you"; (4) exact alarm and battery exemption as one "Make the adhan reliable" step, with a plain reason and a test-adhan button; (5) OEM-specific instructions only when the device is Samsung, Huawei, Xiaomi and so on.
- Keep paywalls and widget prompts out of first-run. The widget prompt works better later, as a contextual card.
- A persistent but dismissible banner in settings ("Adhan may be delayed: battery optimization is on") recovers users who skipped the step.

### Gaps
- No source showed screen-by-screen copy of how top apps word the exact-alarm request (Android 12+ SCHEDULE_EXACT_ALARM or Android 13+ USE_EXACT_ALARM).
- No study on conversion or drop-off for permission-heavy onboarding in prayer apps.
- The Lazyweb "enable prayer notifications" flow library was found but not fetched. — [Lazyweb flow](https://www.lazyweb.com/canvas/flows/find-your-faith/enable-prayer-notifications)

## 5. Settings organization (per-prayer alerts, adhan picker, calculation method)

### Takeaway
The best Android settings give each prayer its own alert type (silent, beep, full adhan), a separate Fajr adhan, before and after reminders, and many calculation methods with per-prayer minute adjustments. Users complain most when the default method is wrong for their region and the app does not explain why times differ from their mosque.

### Cited Findings
- Al-Azan:
  - per-time configuration for the five prayers plus Sunrise, Sunset, Midnight and Tahajjud, with unneeded times hidden
  - a custom adhan file or any device sound, and a separate Fajr adhan
  - reminders before or after a prayer
  - viewing and skipping upcoming alarms
  - clearing notifications after a set time
  - optional Do Not Disturb for a chosen time after the adhan
  - many calculation methods, with Morocco added in 2.4.1

  — [F-Droid: Al-Azan](https://f-droid.org/en/packages/com.github.meypod.al_azan)
- Athan by IslamicFinder is credited for accuracy because of multiple calculation methods and per-prayer adjustments. — [islamtics](https://islamtics.com/best-islamic-apps-android/)
- Mawaqit puts the alert-type toggle (dome for adhan, bell for beep) on the home page and the adhan choice in Settings. — [Mawaqit help](https://help.mawaqit.net/en/articles/12230364-troubleshooting-the-adhan-on-android-phones)
- Complaint pattern: "wrong or drifting times after travel, with calculation-method defaults that aren't explained" (Athan). — [Unstar](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)
- A developer reply to a review explains that some masjids use a different calculation method, which makes app times look "wrong" against the local mosque. This is from a search summary. — [App Store: Taqwa Time](https://apps.apple.com/app/id6503914442)
- Muezzin offers auto-update that recalculates times for the current location. — [App Store: Muezzin](https://apps.apple.com/us/app/muezzin/id566982859)
- Pillars puts the full athan and a "Fajr Smart Alarm" in Premium. — [thepillarsapp.com](https://thepillarsapp.com/)

### Inferences
- Structure: Location and method (auto by country, with "Advanced" for method, Asr school, high-latitude rule, per-prayer minutes); Alerts per prayer (off, beep, adhan, plus pre-reminder minutes); Adhan sound picker with a play preview and a separate Fajr choice; Reliability (battery and exact alarm status plus a test button); Appearance (theme, digits, Hijri offset).
- Showing which method is in use and why ("Umm al-Qura, chosen for Saudi Arabia") on the times screen can prevent "wrong times" complaints.

### Gaps
- No source confirmed which top apps automatically pick the method by country. That pattern is widely assumed but was not sourced in this research.
- No sentiment data on adhan picker preview UX.

## 6. Accessibility and Arabic (RTL, Arabic-Indic digits, large text, contrast, screen readers)

### Takeaway
Evidence is thin. The one accessibility review found (Sajda, iOS) says the prayer schedule works with a screen reader but some buttons are unlabeled. Arabic, Persian and Urdu localization is common in open-source Android apps, but no reviews of RTL quality, digit choice or large-text behavior were found.

### Cited Findings
- AppleVis (iOS 18.2.1, so roughly early 2025): Sajda's "prayer schedule works just fine with VoiceOver", but "Some buttons are not labeled". From search snippets; the page returned 403. — [AppleVis: Sajda](https://www.applevis.com/apps/ios/lifestyle/sajda)
- Al-Azan is localized in 13 languages, including Arabic, Persian and Urdu. The listing does not describe RTL layout behavior. — [F-Droid: Al-Azan](https://f-droid.org/en/packages/com.github.meypod.al_azan)
- Hijri Today is localized in English, Indonesian and Arabic, and refreshes immediately on a language change. — [DEV Community](https://dev.to/cas8398/building-hijri-today-a-widget-my-mom-actually-uses-32c8)
- Muslim Pro's reader offers font, text size and theme settings. — [ScreensDesign](https://screensdesign.com/showcase/muslim-pro-quran-athan-prayer)

### Inferences
- Unlabeled icon buttons (such as alert-type icons per prayer) are the likely accessibility failure. Each needs a spoken label like "Fajr alert: adhan".
- Screen readers read countdowns poorly unless they get a stable label ("Next prayer Asr in 1 hour 12 minutes").

### Gaps
- No source on Arabic-Indic versus Latin digit preferences in prayer apps, high-contrast modes, or TalkBack testing on Android prayer apps.

## 7. What users hate (ads, subscriptions, clutter, missed adhan, wrong times, privacy)

### Takeaway
The top complaints are:
- ads appearing exactly when users open the app to check or hear the adhan
- previously free features (especially widgets) moved behind subscriptions, sometimes during Ramadan
- super-app clutter
- adhan that does not fire or plays silently on Android
- wrong times after travel or a daylight-saving change
- distrust over location-data sales, which dates from 2020 and still shapes choices

### Cited Findings
- Ads:
  - Muslim Pro's free tier has "heavy banner and interstitial ads". Athan has "Banner ads sit on most screens". Quran Majeed has frequent "banners on the home screen and interstitials between actions". — [islamtics](https://islamtics.com/best-islamic-apps-android/) (competitor source)
  - Athan ads load when users open the app to check or hear the adhan; the objection is "ads here, of all places". — [Unstar](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)
  - "Prayer Time: Athan & Quran" had 2025 complaints about "lots of ads". — [unitQ](https://unitq.com/unitq-scorecards/prayertimeathanquran)
- Subscriptions and paywalls:
  - An Athan Pro reviewer says the developer put "previously free features behind paywall DURING RAMADAN". — [justuseapp Athan Pro](https://justuseapp.com/en/app/743843090/athan-pro-quran-azan-qibla/reviews)
  - A Pillars reviewer complained about the widget paywall. — [App Store: Pillars](https://apps.apple.com/us/app/-/id1559086853)
  - Muslim Pro's onboarding paywall uses a countdown timer, and Muslim Pro plus Qalbox yearly is $34.99. — [ScreensDesign](https://screensdesign.com/showcase/muslim-pro-quran-athan-prayer); [Adapty paywall library](https://adapty.io/paywall-library/muslim-pro/)
  - Muslim Pro users report frequent upgrade prompts. — [Unstar](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)
  - A Pillars reviewer asked the developers not to "bring in ads" as the app grows. — [App Store: Pillars](https://apps.apple.com/us/app/-/id1559086853)
- Clutter: Muslim Pro reviewers cite restaurants, a community feed and commerce. — [Unstar](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026). The March 2025 review quoted under section 2 adds "very complicated". — [unitQ Muslim Pro](https://unitq.com/unitq-scorecards/muslimproramadan2023)
- Missed or silent adhan:
  - Athan's adhan "doesn't fire, plays silently, or is delayed on Android". — [Unstar](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)
  - A Mawaqit review aggregator notes complaints that the adhan sound disappeared after a 2025 update, plus complaints about wrong Fajr times. These are from search snippets; the fetched page showed only a score of 92 from 1,159 reports. — [unitQ Mawaqit](https://unitq.com/unitq-scorecards/mawaqitprayertimesmosques)
  - A Pillars user says the adhan "doesn't play aloud" despite enabled settings. — [App Store: Pillars](https://apps.apple.com/us/app/-/id1559086853)
  - A Gosolat user reported notifications firing minutes early while in-app times were correct. — [Gosolat feedback board](https://gosolat.userjot.com/board/p/notification-waktu-solat-awal-dari-sepatutnya)
- Wrong times:
  - An Athan Pro reviewer saw wrong times after the daylight-saving switch. — [justuseapp Athan Pro](https://justuseapp.com/en/app/1098618656/athan-pro/reviews)
  - An iPray user asked for automatic location for accurate times while travelling. — [App Store: iPray](https://apps.apple.com/ie/app/ipray/id288054534)
  - Pillars has occasional time errors in specific regions. — [Unstar](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)
- Privacy [OLDER, Nov 2020, still cited in 2025-2026]:
  - Vice/Motherboard reported Muslim Pro sent location data to X-Mode, which sold data onward, including to US military contractors. Muslim Pro called the military claim "untrue" but admitted sharing anonymized data with X-Mode and ended data-broker ties. — [Vice](https://www.vice.com/en/article/muslim-pro-location-data-military-xmode); [Al Jazeera](https://www.aljazeera.com/news/2020/11/18/muslim-pro-app-denies-selling-user-data-to-us-military)
  - A council of 90 New York mosques urged deletion. — [Middle East Eye](https://www.middleeasteye.net/news/us-military-data-muslim-pro-app-denounced)
  - Salaat First's developer confirmed sending location data to Predicio. — [Vice: Salaat First](https://www.vice.com/en/article/muslim-app-location-data-salaat-first/)
  - Pillars was created largely in response to the 2020 Muslim Pro privacy concerns. — [Comparitech](https://comparitech.com/blog/vpn-privacy/muslim-prayer-app-study)
  - 2025 follow-up: iPhoneIslam warns that prayer apps link location to identity. It advises avoiding apps that require login and choosing apps where "data stays on the device". The publisher promotes its own app. — [iPhoneIslam, Sep 2025](https://iphoneislam.com/en/2025/09/safe_prayer_time_app_for_muslims/160957)
  - 2026 roundups still advise skipping Salaat First and Muslim Pro for privacy. — [islamtics](https://islamtics.com/best-islamic-apps-android/)

### Inferences
- "No ads anywhere, especially not on the adhan or prayer screen" and "no account, location stays on the phone" are the two strongest trust signals in the category.
- Reliability bugs (silent adhan, stale widget) are treated as worse than missing features. One missed Fajr can cost an app its user.
- Times that change after travel or a daylight-saving switch must be recalculated automatically, and the app should say so visibly ("Location updated: Riyadh").

### Gaps
- No Reddit thread content was retrievable (r/islam, r/MuslimLounge, r/androidapps). Search did not surface threads, so Reddit sentiment is unrepresented.
- No quantified complaint shares; the Unstar analysis gives only qualitative labels.

## 8. Dribbble and Behance design trends (patterns only)

### Takeaway
Few dated 2024-2026 shots surfaced. Visible patterns are a prayer-time widget concept, Ramadan tracker dashboards, and "least distraction" faith app concepts. Commercial apps show sun-arc or sundial visuals and glass or translucent widgets as current trends.

### Cited Findings
- "GrabDaily - Prayer Time Widget" (110 likes, 24.8k views; date not shown). — [Dribbble prayerapp tag](https://dribbble.com/Creative-Box/tags/prayerapp)
- "Daily Ramadan Tracker App UI" shot. — [Dribbble](https://dribbble.com/shots/23808841-Daily-Ramadan-Tracker-App-UI)
- The Faith app concept, published on Behance, aims for "least distractions and cleanest interface possible". This was summarized from search results. — [Dribbble: Clear Faith App UI](https://dribbble.com/shots/10752012-Clear-Faith-App-UI) [likely OLDER, shot ID suggests about 2020]
- Portfolio projects: Masjid 247 is aimed at helping people "stay connected to prayer times with ease and clarity". — [Contra: Masjid 247](https://contra.com/p/YM3kFlsR-masjid-247-never-miss-a-prayer); [Contra: Ihsan](https://contra.com/p/mbXvcXQP-ihsan-islamic-mobile-app-design); [Contra: Mishkat](https://contra.com/p/rcAFe7Mo-mishkat-brand-and-digital-experience-design-build)
- The Damai case study on Medium covers a prayer and qadha-tracking app with prayer times, qadha management and insights. Undated in the results. — [Medium: Damai](https://medium.com/muazzim/case-study-damai-app-fc1ff1ca5ed2)
- Commercial trends: sun arc from Fajr to Isha (Siraj listing) — [App Store id6759178047](https://apps.apple.com/app/id6759178047); digital sundial — [Awqāt](https://apps.apple.com/us/app/awq%C4%81t/id6461305834); Liquid Glass widgets — [MasjidBox](https://masjidbox.com/changelog/enhanced-ios-widgets-and-user-experience-improvements)

### Inferences
- The concept trend matches the store-praise trend: one hero metric (next prayer), soft color and lots of space, with ornament kept light.

### Gaps
- Dribbble and Behance pages could not be browsed in detail or filtered by date. No reliable 2025-2026 shot set was assembled.
- No verified source on the use of Islamic geometric patterns in current concepts.
