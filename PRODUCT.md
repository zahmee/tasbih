# Product

## Register

product

## Users

Arabic-speaking Muslims using a phone during short, focused moments of daily dhikr. The core flow should work comfortably one-handed, remain readable across text sizes, and avoid demanding visual attention beyond the current remembrance.

## Product Purpose

Anaa provides offline-first access to daily adhkar, Hisn al-Muslim, and a personal tasbih. Success means the user can begin, count, resume, and complete remembrance with minimal friction while their progress stays private on the device.

On November 10, 2026 the user widened the scope to the day's other anchors: the Hijri and Gregorian dates and month calendars, then prayer times and the Qibla direction. They serve the same daily practice and follow the same rules. Everything is computed on the device, with no account and no tracking, and each permission is asked for only when the feature that needs it is turned on. On November 11, 2026 the user chose the hybrid approach: the internet is used for one thing only, searching a city or village by name, and prayer times are still worked out on the phone so they keep working offline. The same day the remaining phases were built: prayer widgets, alerts and the full adhan, then the plan's remaining items: a monthly and Ramadan timetable, the Qibla by the sun for phones without a compass, a Qibla widget, a travel notice, an option to start the Hijri day at Maghrib, and personal Hijri occasions. Widget background choices were left out because Anaa's widgets already follow day and night mode. Later that day the user chose to put prayer and its services first: the home screen leads with the next prayer and the day's times, then the Qibla, the calendar and the month's table, then the day's wird and the tasbih. The bottom bar became the day, prayer, the calendar, the adhkar and more, and the tasbih opens from the home screen and the adhkar. The adhan recordings are free-licensed ones the user listened to and chose; their licences and changes are in THIRD_PARTY_NOTICES.md. The research and phased plan are in `reports/`. On November 19, 2026 the user asked for the phone's position to carry its real name, as other apps show it: the coordinates go to the phone's own place naming service, which returns the district, town and region, the user may allow the precise position for the district, the place is read again each time the app opens so it follows a journey, and the name can be edited. The user accepted that this sends the coordinates off the phone, and the privacy policy says so. The same day the user asked for the Qibla over satellite imagery, so one can see where one stands and where the direction runs, and chose Google Maps for it: an optional view on the Qibla screen, with a line from the middle of the map to the Kaaba, offered only where a Maps key and Google services are present. On November 23, 2026 the user removed the occasions the calendar could show on request, the Mawlid, the Isra and Mi'raj and the middle of Sha'ban, as marking them is not of the religion; the calendar keeps only occasions with a sound basis, and the setting that showed the others is gone. The same day the user moved each tab's settings into that tab, behind a gear on its page, and the «more» tab went: the bottom bar is the day, prayer, the calendar, the adhkar and the tasbih, and the day's gear opens what belongs to the whole app, its look and about it. Later that day the user asked for a list of the day's tasks with reminders: the morning and evening adhkar, tasks of the user's own, recurring or once, done and not done. It is «وِردي», the wird: tasks fall daily, on days of the week, on days of the Hijri month, or once, at no time, at a clock time, or before or after a prayer, and a reminder is a notification with «done» and «later». A task of adhkar is done when they are read to their end in the app, and its reminder then stays quiet. Ten suggested tasks with a sound basis are a tap away; the morning and evening adhkar are on the list from the start, their reminders off. It has no tab of its own: a line on the day's page, a widget that ticks tasks done, the reminders and the calendar's day all open it. Nothing counts days in a row or marks a missed day in red. The same day the user asked for the widgets' ornament to be a light mihrab in place of the interlaced stars, for the day widget's prayers to stand together in one box with a background of its own (after trying a frame for each prayer, which the user did not want), with a colour of its own for the marked prayer and a choice of marking the next prayer or the current one, for the time bar to have a colour of its own or to turn from blue to red as the adhan nears, and for a choice between the Hindi digits «١٢٣» and the Arabic «123», for the app and its widgets together. The version was then raised to 2.0.0, build 11.

## Product Name

The approved Arabic name is «اناء», without the alif madda, with «أذكار ومسبحة» as its descriptor. Use the short name in the launcher and screen headings, and the full name «اناء — أذكار ومسبحة» in descriptive material. The Google Play listing title has been «اناء» since October 3, 2026. From version 2.0.2 (November 24, 2026), the launcher, the app's text and the drawn wordmark match it: the madda shape was removed from the wordmark. Older documents and release notes keep «آناء» as history. The English transliteration is Anaa.

Keep the existing Android application ID, storage names, and persisted preference values when applying the display-name change, so existing installations retain their data.

## Brand Personality

Calm, respectful, and focused. The interface should feel quietly reassuring and contemporary while treating the religious content with appropriate dignity.

The launcher and splash icon approved on October 3, 2026 is the revised first concept: a clear green alif and noon, with copper prayer beads replacing the noon dot on an ivory background. Native vector layers follow the approved artwork's contours and provide a matching monochrome layer. The store PNG preserves the approved export unchanged. Every in-app emblem, including shared page headers, About, and the wide-screen navigation rail, reuses the same launcher artwork in its original colors; the old flowing emblem resources are removed. The bespoke Arabic wordmark remains the app name, with its smaller dimensions on Home. Reading actions remain pine-green. The home screen's lead card, now the next prayer, has rounded top-right and bottom-left corners, with sharp opposite corners, as requested on October 4, 2026.

## Anti-references

- Cluttered portal-style screens or unrelated feature sections. The home screen holds one lead card, one row of times and three services, then the wird's line, the day's adhkar and the tasbih, and never crowds the reading and tasbih screens.
- Loud gamification, celebratory effects, or competitive mechanics.
- Advertising-led layouts, promotional interruptions, and account pressure.
- Ornamental treatments that compete with Arabic reading or obscure controls.

## Design Principles

- Lead with the day's next prayer, keep remembrance one tap away, and make frequent actions effortless.
- Let the interface recede during reading while keeping progress unmistakable.
- Preserve a consistent, familiar interaction vocabulary across reading and tasbih flows.
- Use Islamic visual identity with restraint and never at the cost of legibility.
- Keep all essential content and progress available locally without an account.

## Accessibility & Inclusion

Design RTL-first with clear Arabic typography, generous touch targets, screen-reader semantics, dark mode, dynamic color compatibility, scalable reading text, and feedback that does not depend on color, sound, or haptics alone.
