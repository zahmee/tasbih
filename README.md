# سكينة

[العربية](#العربية)

[English](#english)

<a id="العربية"></a>

<div dir="rtl">

> سكينة تطبيق عربي أصلي للأذكار اليومية، وكتاب حصن المسلم، والمسبحة. يعمل محليًا، بلا حساب، وبلا إعلانات أو اشتراكات.

## المشروع متاح للجميع

هذا المستودع هو المصدر الكامل للتطبيق، ومتاح بموجب ترخيص **MIT**. يمكنك قراءته، تشغيله، تعديله، ونشر نسخ مشتقة منه وفق شروط الترخيص.

التطبيق لا يطلب صلاحية الإنترنت لتشغيل محتواه؛ النصوص والإعدادات والتقدم تحفظ محليًا على الجهاز. لا توجد حسابات مستخدمين أو خدمة خلفية أو تتبع إعلاني.

## المزايا

- 133 بابًا و302 ذكرًا ودعاءً من حصن المسلم، مع التكرار والمصادر.
- وصول سريع إلى أذكار الصباح والمساء والنوم وما بعد الصلاة.
- قارئ مخصص يحفظ التقدم، ويحسب التكرارات، ويدعم المفضلة والتشكيل وحجم الخط.
- بحث عربي يتسامح مع اختلاف الهمزات والتشكيل.
- مسبحة حرة بأهداف جاهزة أو هدف يحدده المستخدم.
- إدارة مستقلة لأذكار المسبحة: إضافة وتعديل وحذف، مع توقف التنقل عند أول ذكر وآخره.
- واجهة عربية من اليمين إلى اليسار، وخطوط مناسبة للنصوص العربية، وخيار الألوان الديناميكية والوضع الداكن.

يبقى نطاق سكينة مقصودًا ومركزًا: الذكر والقراءة والتسبيح، من دون أقسام جانبية مشتتة.

## صور من التطبيق

<table>
  <tr>
    <td align="center"><img src="docs/images/home.png" alt="الشاشة الرئيسية" width="220"><br>الشاشة الرئيسية</td>
    <td align="center"><img src="docs/images/reader.png" alt="قارئ الأذكار" width="220"><br>قارئ الأذكار</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/images/tasbih.png" alt="المسبحة" width="220"><br>المسبحة</td>
    <td align="center"><img src="docs/images/tasbih-phrases.png" alt="إدارة أذكار المسبحة" width="220"><br>إدارة أذكار المسبحة</td>
  </tr>
</table>

## التقنية

- Kotlin
- Jetpack Compose وMaterial 3
- Navigation 3
- DataStore لحفظ الإعدادات والمفضلة والتقدم وبيانات المسبحة محليًا
- Room للبيانات المحلية
- محتوى عربي محلي داخل التطبيق عبر ملف `hisn_ar.json`

الحد الأدنى للنظام هو Android 8.0، ومستوى واجهة البرمجة 26.

## التشغيل محليًا

المتطلبات:

- JDK 17
- Android SDK Platform 37
- محاكي أو جهاز يعمل بنظام Android 8.0 أو أحدث

افتح المشروع في بيئة التطوير المناسبة، أو شغّل الأمر التالي من جذر المستودع:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

لا يحتاج المشروع إلى مفتاح API أو متغيرات سرية.

لتشغيل اختبارات الواجهة على محاكي أو جهاز متصل:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## الخصوصية

- لا يتطلب التطبيق إنشاء حساب.
- لا يتضمن إعلانات أو تحليلات أو اشتراكات.
- لا يطلب صلاحية الإنترنت لتشغيل المحتوى.
- تحفظ تفضيلات القراءة والمسبحة والتقدم محليًا على الجهاز.

البيان الكامل:

[سياسة الخصوصية](PRIVACY_POLICY.md)

## المحتوى والتراخيص

الكود الأصلي لهذا المشروع مرخص بموجب [MIT](LICENSE).

تحتفظ بيانات حصن المسلم والخطوط المضمنة بتراخيصها الخاصة. راجع [إشعارات الأطراف الثالثة](THIRD_PARTY_NOTICES.md) للمصادر والتراخيص وسجل التحقق.

ينبغي التعامل مع أي تصحيح ديني أو تعديل للنصوص بعناية، مع إرفاق مصدر يمكن مراجعته.

## المساهمة والدعم

نرحب بالتقارير والتحسينات والمساهمات المدروسة. يرجى قراءة [دليل المساهمة](CONTRIBUTING.md) قبل فتح طلب دمج.

للأسئلة أو الإبلاغ المسؤول عن مشكلة أمنية أو خصوصية:

<a href="mailto:info@cdit.co">info@cdit.co</a>

<a href="https://cdit.co/contact.html">صفحة التواصل</a>

التطوير بواسطة مؤسسة إبداع التطوير والبرمجة لتقنية المعلومات.

## الإصدار

الإصدار الحالي هو 1.3.0، ورقم البناء 4.

</div>

<a id="english"></a>

## English

Sakinah is a native Arabic-first Android app for daily adhkar, Hisn al-Muslim content, and personal tasbih sessions. It works locally with no account, ads, subscriptions, or network permission required for its content.

### Highlights

- 133 chapters and 302 adhkar and du'a entries.
- Focused reader with progress, repetition, favorites, tashkeel, and text-size controls.
- Arabic-aware search.
- A flexible tasbih with editable phrases and bounded previous/next navigation.
- Local-first settings and progress storage.
- RTL-first interface built with Kotlin and Jetpack Compose.

### Build

Requirements: JDK 17, Android SDK Platform 37, and Android 8.0 / API 26 or newer.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Run connected UI tests with:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### License and attribution

The project source code is released under the [MIT License](LICENSE). See [Third-Party Notices](THIRD_PARTY_NOTICES.md) for the bundled Hisn al-Muslim data and font licenses.

### Privacy

Read the app-specific [Privacy Policy](PRIVACY_POLICY.md).

### Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a change. Security or privacy reports can be sent to <a href="mailto:info@cdit.co">info@cdit.co</a>.
