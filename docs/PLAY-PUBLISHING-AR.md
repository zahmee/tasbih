# النشر المباشر إلى متجر قوقل

يُرفع تحديث «اناء» من الطرفية عبر واجهة النشر في متجر قوقل، دون متصفح. يقوم بذلك إضافة البناء `com.github.triplet.play` مع حساب خدمة، بالطريقة نفسها المعتمدة في مشروع «نتلو».

## الإعداد لمرة واحدة

### أ. صلاحية حساب الخدمة على التطبيق

حساب الخدمة المستخدم هو حساب النشر نفسه الذي يرفع «نتلو»:

```
play-publisher-quran@api-7291268109329898663-2910.iam.gserviceaccount.com
```

في لوحة المتجر: **Users and permissions** ← هذا الحساب ← `Add app` ← «اناء»، ثم امنحه:

- `Release to production, exclude devices, and use Play App Signing`
- `Release apps to testing tracks`

ثم `Save changes`. قد يحتاج تفعيل الصلاحية بضع دقائق.

### ب. ملف المفتاح

ضع ملف مفتاح حساب الخدمة في جذر المشروع:

```
play-service-account.json
```

هو نسخة من الملف نفسه في `D:\new project\quran_01\mushaf_app\play-service-account.json`. الملف مستبعد في `.gitignore` مثل `keystore.properties`، فلا يدخل المستودع أبدًا. **لا ترفعه ولا تشاركه.** إن لم يوجد الملف تتعطل إضافة النشر تلقائيًا، ويبقى البناء العادي سليمًا.

### ج. التحقق

```powershell
python tools/check_play_auth.py
```

يفتح معاملة تعديل مؤقتة ويحذفها، دون رفع أو نشر أي شيء. يجب أن تظهر الخطوات الثلاث بحالة `OK`.

## نشر إصدار جديد

١. ارفع `versionCode` و`versionName` و`RELEASE_DATE` في `app/build.gradle.kts`، فالمتجر يرفض رقم بناء مستعملًا.

٢. حدّث `CHANGELOG.md`، وملاحظات المتجر في:

```
app/src/main/play/release-notes/ar/default.txt
```

الحد الأقصى ٥٠٠ حرف، وتُرفع مع الحزمة تلقائيًا.

٣. شغّل الاختبارات وابنِ الحزمة الموقعة، وافحص الترقية من الإصدار السابق على محاكٍ:

```powershell
.\gradlew.bat testDebugUnitTest lintRelease bundleRelease assembleRelease
```

٤. ارفع إلى مسار الإنتاج بنسبة كاملة:

```powershell
.\gradlew.bat :app:publishReleaseBundle --track production --release-status completed --release-name "اناء 1.3.7 — وصف قصير"
```

خيارات سطر الأوامر تتجاوز الإعداد الافتراضي الآمن في `app/build.gradle.kts` (مسار `internal` وحالة مسودة) دون تعديله.

٥. تحقق من النتيجة:

```powershell
python tools/check_play_tracks.py --notes
```

النشر المُدار مطفأ، فيمر الإصدار بمراجعة قوقل ثم يُنشر تلقائيًا بعد الموافقة. حالة `inProgress` أو «قيد المراجعة» لا تعني أن التحديث متاح للجمهور بعد.

## ملاحظات

- لا يطبع أي سكربت المفتاح الخاص أو رمز الدخول.
- ما زال المتصفح الداخلي طريقًا بديلًا: رفع الحزمة من زر `Upload` في صفحة إصدار الإنتاج، ثم «إرسال التغييرات للمراجعة» من صفحة نظرة عامة على النشر.
