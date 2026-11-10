# إشعارات الطرف الثالث

## بيانات حصن المسلم العربية

- المصدر: [asellam/HisnElMuslim](https://github.com/asellam/HisnElMuslim)
- النسخة المثبتة: `872764ca013116c905f7fc9c3dc972c27cd37d4a`
- الملف: `hisn.json`
- SHA-256 للنسخة المضمّنة: `B30A448EF40184B0422C40BD3C372BF2B25BFB5B0539FD3699E6B4FBA9459D80`
- الترخيص: MIT، حقوق النشر © 2021 Abdellah SELLAM
- الكتاب: «حصن المسلم من أذكار الكتاب والسنة» للشيخ سعيد بن علي بن وهف القحطاني

نص ترخيص MIT محفوظ داخل التطبيق في `app/src/main/assets/licenses/hisn_data_mit.txt`.

ينبغي إجراء مراجعة شرعية وتحريرية مستقلة للنصوص والمراجع قبل نشر نسخة إنتاجية في المتجر، مع الحفاظ على إمكانية الإبلاغ عن أخطاء المحتوى.

## الخطوط

الخطوط التالية مأخوذة من مشروع [Google Fonts](https://github.com/google/fonts) عند النسخة `ec626514f79f831f1ab848a82114a0ce7e2d6372`:

- Amiri Regular وAmiri Bold
- IBM Plex Sans Arabic Regular وMedium وSemiBold
- Aref Ruqaa Regular وBold، المستخدم للعناوين الزخرفية والهوية الإسلامية

الخطوط مرخّصة وفق SIL Open Font License 1.1. نسخ التراخيص محفوظة داخل `app/src/main/assets/licenses/`.

### Droid Arabic Kufi

- Droid Arabic Kufi Regular وBold، خيار «كوفي الباقيات» للواجهة والقراءة
- المصدر: [Android Open Source Project](https://android.googlesource.com/platform/frameworks/base/+/e1538f0/data/fonts/)
- حقوق النشر: © 2010 Google Corporation
- الترخيص: Apache License 2.0

نسخة الترخيص محفوظة داخل التطبيق في `app/src/main/assets/licenses/droid_arabic_kufi_apache2.txt`.

## حساب مواقيت الصلاة والقبلة

- المكتبة: [batoulapps/adhan-java](https://github.com/batoulapps/adhan-java)، النسخة 1.2.1
- الترخيص: MIT، حقوق النشر © Batoul Apps

## البحث عن الأماكن

- الخدمة: [Open-Meteo Geocoding API](https://open-meteo.com/en/docs/geocoding-api)، تُستدعى عند البحث عن مكان بالاسم فقط
- بيانات الأماكن فيها من [GeoNames](https://www.geonames.org/)، بترخيص [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)

## تسجيلات الأذان

كلها من [ويكيميديا كومنز](https://commons.wikimedia.org/). اقتُطع من كل تسجيل الأذانُ وحده، وضُبط مستوى صوته، وحُوّل إلى ملف أحادي بصيغة أوغ. وتبقى الملفات المعدّلة من التسجيلات المرخّصة بالمشاركة بالمثل متاحة بالرخصة نفسها.

| الملف في التطبيق | المصدر | صاحب التسجيل | الرخصة | التعديل |
|---|---|---|---|---|
| `adhan_makkah_maghrib.ogg` | [Maghrib Adhan at the Masjid al Haram, Mecca - 25 Feb, 2012](https://commons.wikimedia.org/wiki/File:Maghrib_Adhan_at_the_Masjid_al_Haram,_Mecca_-_25_Feb,_2012.webm) | 3omar Faruq | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) | الصوت فقط، من الثانية 124 إلى 290 |
| `prayer_tone_takbir_makkah.ogg` | [Adhan, Great Mosque of Mecca - Jan 21, 2013](https://commons.wikimedia.org/wiki/File:Adhan,_Great_Mosque_of_Mecca_-_Jan_21,_2013.webm) | Seyfula Islam | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) | الصوت فقط، التكبيرتان الأوليان من الثانية 7.5 إلى 32 |
| `adhan_recording_1.ogg` | [Adhan.ogg](https://commons.wikimedia.org/wiki/File:Adhan.ogg) | Aishatu98 | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | حذف الصمت في أوله |
| `adhan_recording_2.ogg` | [Beautiful adhan.ogg](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg) | Adam-synagda | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | ضبط الصوت |
| `adhan_recording_3.ogg` | [Azan.ogg](https://commons.wikimedia.org/wiki/File:Azan.ogg) | Andrewler | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) | ضبط الصوت |
| `adhan_recording_4.ogg` | [Call to prayer.ogg](https://commons.wikimedia.org/wiki/File:Call_to_prayer.ogg) | Isaacayodele32 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) | ضبط الصوت |

النغمات `prayer_tone_chime` و`prayer_tone_bell` و`prayer_tone_soft` من صنع المشروع، يولّدها `tools/generate_prayer_tones.py`.
