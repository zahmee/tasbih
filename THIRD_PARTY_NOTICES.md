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

## خرائط القبلة

- المكتبة: [Maps Compose](https://github.com/googlemaps/android-maps-compose)، النسخة 9.0.1، بترخيص Apache 2.0، ومعها حزمة خرائط قوقل لأندرويد من خدمات قوقل وهي مغلقة المصدر
- الاستخدام: صور القمر الصناعي تحت خط القبلة، عند اختيار المستخدم فقط، وتخضع لشروط منصة خرائط قوقل
- المفتاح: في ملف `maps.properties` غير المرفوع، بسطر واحد `MAPS_API_KEY=...`. البناء دونه يخلو من عرض القمر الصناعي

## البحث عن الأماكن

- الخدمة: [Open-Meteo Geocoding API](https://open-meteo.com/en/docs/geocoding-api)، تُستدعى عند البحث عن مكان بالاسم فقط
- بيانات الأماكن فيها من [GeoNames](https://www.geonames.org/)، بترخيص [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)

## قائمة الأماكن المضمّنة

- الملف: `app/src/main/assets/places_ar.tsv`، يسمّي موقع الهاتف دون إنترنت حين لا تتوفر في الهاتف خدمة تسمية الأماكن
- المصدر: [GeoNames](https://www.geonames.org/)، بترخيص [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)، نُزّلت ملفاته في ١٩ نوفمبر ٢٠٢٦
- التعديل: أُبقيت الأماكن المأهولة في الدول العربية الاثنتين والعشرين التي لها اسم عربي، مع إحداثياتها ومنطقتها ورمز دولتها، وحُذفت علامات الاتجاه من الأسماء. يبنيه `tools/build_places.py`

## تسجيلات الأذان

هذان من [ويكيميديا كومنز](https://commons.wikimedia.org/). اقتُطع من كل تسجيل ما يلزم، وضُبط مستوى صوته، وحُوّل إلى ملف أحادي بصيغة أوغ. وحُذفت في ٢٣ نوفمبر ٢٠٢٦ أربعة تسجيلات أخرى من ويكيميديا كانت في التطبيق، باختيار صاحبه.

| الملف في التطبيق | المصدر | صاحب التسجيل | الرخصة | التعديل |
|---|---|---|---|---|
| `prayer_tone_takbir_makkah.ogg` | [Adhan, Great Mosque of Mecca - Jan 21, 2013](https://commons.wikimedia.org/wiki/File:Adhan,_Great_Mosque_of_Mecca_-_Jan_21,_2013.webm) | Seyfula Islam | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) | الصوت فقط، التكبيرتان الأوليان من الثانية 7.5 إلى 32 |
| `adhan_recording_2.ogg` | [Beautiful adhan.ogg](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg) | Adam-synagda | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | ضبط الصوت |

### أذانات أضافها صاحب التطبيق

أُضيفت في ٢٣ نوفمبر ٢٠٢٦ من ملفات قدّمها صاحب التطبيق، وليست من ويكيميديا كومنز. **مصدرها ورخصتها لم يُتحقق منهما بعد**، ويجب التحقق منهما أو حذفها قبل النشر في المتجر. حُذف الصمت من طرفي كل تسجيل، وضُبط مستوى صوته، وحُوّل إلى ملف أحادي بصيغة أوغ.

| الملف في التطبيق | المؤذن كما في بيانات الملف | الرخصة |
|---|---|---|
| `adhan_ali_mulla.ogg` | Ali Ibn Ahmed Mala، الحرم المكي، 2006 | لم تُعرف |
| `adhan_ahmed_alamadi.ogg` | Ahmed El Amadi، قطر | لم تُعرف |
| `adhan_nasser_alqatami.ogg` | Nasser Al Qatami | لم تُعرف |
| `adhan_hamad_aldaghriri.ogg` | Hamad Al Daghriri | لم تُعرف |

## نغمات التنبيه

نغمات أندرويد الثلاث التي كانت هنا حُذفت في ٢٣ نوفمبر ٢٠٢٦ باختيار صاحب التطبيق، وحلّت محلها النغمات التالية.

### نغمات من Pixabay

أُضيفت في ٢٣ نوفمبر ٢٠٢٦ من ملفات نزّلها صاحب التطبيق من [Pixabay](https://pixabay.com/)، وتسمح [رخصة محتوى Pixabay](https://pixabay.com/service/license-summary/) باستعمالها داخل تطبيق. حُذف الصمت من طرفيها، وضُبط صوتها، وحُوّلت إلى ملفات أحادية.

| الملف في التطبيق | الأصل | صاحبه |
|---|---|---|
| `prayer_tone_clear.ogg` | `koiroylers-clear-notification-355970.mp3` | koiroylers |
| `prayer_tone_light.ogg` | `universfield-new-notification-032-480570.mp3` | Universfield |
| `prayer_tone_simple.ogg` | `universfield-simple-notification-152054.mp3` | Universfield |
