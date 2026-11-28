# فتح آناء من نتلو على تبويب محدد

المطلوب في آناء حتى يستطيع تطبيق نتلو (مصحف القارئ، `com.mushaf.reader`) أن يفتحه مباشرة على تبويب
يختاره المستخدم من قائمة «المزيد» في نتلو.

لا تبادل للبيانات بين التطبيقين: نتلو يطلب وجهة فقط، وآناء ينتقل إليها.

## ١. العقد الثابت (لا يتغير بعد النشر)

نتلو سيرسل هذا الطلب بالضبط:

| العنصر | القيمة |
|---|---|
| الحزمة | `com.sakinah.tasbih` |
| الإجراء (action) | `com.sakinah.tasbih.action.OPEN` |
| المعامل (extra، نص) | `com.sakinah.tasbih.extra.DESTINATION` |

قيم الوجهة:

| القيمة | الشاشة في آناء |
|---|---|
| `today` | تبويب اليوم (`AppDestination.Home`) |
| `prayer` | تبويب الصلاة (`AppDestination.Prayer`) |
| `calendar` | تبويب التقويم على الشهر الحالي (`AppDestination.Calendar(null)`) |
| `adhkar` | تبويب الأذكار (`AppDestination.Library`) |
| `tasbih` | تبويب التسبيح (`AppDestination.Tasbih`) |
| `qibla` | القبلة (`AppDestination.Qibla`) |

القواعد:

- وجهة ناقصة أو غير معروفة: يفتح آناء على تبويب اليوم، ولا يتعطل ولا يعرض خطأ.
- أسماء الإجراء والمعامل والقيم أعلاه نهائية. يجوز إضافة قيم جديدة لاحقاً، ولا يجوز تغيير أو حذف قيمة منشورة.
- الطريقة الداخلية الحالية (المعامل `open` في `MainActivity`، للأدوات والإشعارات) تبقى كما هي دون تغيير. العقد الجديد
  طريق منفصل للتطبيقات الأخرى، حتى تبقى الطريقة الداخلية حرّة في التغيّر.

## ٢. التعديلات في الكود

### `AndroidManifest.xml`

أضف إلى `MainActivity` (وهي `exported="true"` أصلاً) فلتراً ثانياً بجانب فلتر `LAUNCHER`:

```xml
<intent-filter>
    <action android:name="com.sakinah.tasbih.action.OPEN" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

### `MainActivity.kt`

في `requestFrom(intent)` وقبل فحص المعامل `open`:

- إذا كان `intent.action == ACTION_OPEN` فاقرأ `EXTRA_DESTINATION` وحوّله إلى `AppOpenRequest`:
  - `prayer` ← `AppOpenRequest.Prayer` (موجود)
  - `qibla` ← `AppOpenRequest.Qibla` (موجود)
  - `calendar` ← `AppOpenRequest.Calendar(LocalDate.now())` (موجود)
  - `today` ← طلب جديد `AppOpenRequest.Today`
  - `adhkar` ← طلب جديد `AppOpenRequest.Adhkar`
  - `tasbih` ← طلب جديد `AppOpenRequest.Tasbih`
  - أي قيمة أخرى أو غيابها ← `AppOpenRequest.Today`
- عرّف الثوابت `ACTION_OPEN` و`EXTRA_DESTINATION` وقيم الوجهات في `companion object` وهي عامة (`const val`)، مع تعليق
  يقول إنها عقد مع نتلو لا يُغيَّر.
- اجعل التحويل دالة منفصلة نقية (نص ← طلب) ليسهل اختبارها.

`onNewIntent` يمر على `requestFrom`، لكن هذا وحده لا يكفي حين يكون آناء مفتوحاً. ظهر في التجربة على المحاكي أن طلباً
بلا رايات يجلب آناء إلى الأمام دون أن يسلّمه الطلب، فيبقى على شاشته السابقة، لأن أندرويد يقارن الطلبات دون معاملاتها.
لذلك صارت `MainActivity` بـ `android:launchMode="singleTask"`: نسخة واحدة من آناء في مهمة خاصة بها، وكل طلب يصلها
عبر `onNewIntent` أياً كانت رايات المرسل.

### `ui/SakinahApp.kt`

- أضف إلى `AppOpenRequest` ثلاثة طلبات: `Today` و`Adhkar` و`Tasbih`.
- في `LaunchedEffect(openRequest)` عالجها مثل `Prayer`: إطفاء `isTasbihFocusMode` و`isReaderFocusMode`، ثم
  `backStack.clear()`، ثم `backStack.add(AppDestination.Home)`، ثم إضافة الوجهة (`Library` أو `Tasbih`؛ و`Today`
  يكتفي بـ `Home`)، ثم `onOpenRequestHandled()`.
- المهم أن تكون النتيجة نفس ما يحدث حين يلمس المستخدم التبويب في الشريط السفلي، وأن يعيده زر الرجوع إلى اليوم ثم
  يخرج كالمعتاد.

## ٣. الاختبار

اختبار وحدة لدالة التحويل: كل قيمة من الست، وقيمة غير معروفة، وغياب المعامل، وإجراء آخر غير `OPEN`.

ثم يدوياً على المحاكي بنسخة release أو debug. انتبه إلى أن نسخة debug حزمتها `com.sakinah.tasbih.preview`، فغيّر
الحزمة في الأمر:

```bash
adb shell am start -a com.sakinah.tasbih.action.OPEN -p com.sakinah.tasbih --es com.sakinah.tasbih.extra.DESTINATION tasbih
adb shell am start -a com.sakinah.tasbih.action.OPEN -p com.sakinah.tasbih --es com.sakinah.tasbih.extra.DESTINATION adhkar
adb shell am start -a com.sakinah.tasbih.action.OPEN -p com.sakinah.tasbih --es com.sakinah.tasbih.extra.DESTINATION wrong
```

جرّب كل أمر في حالتين: آناء مغلق تماماً، وآناء مفتوح على شاشة أخرى (مثل وضع التركيز في المسبحة أو داخل قارئ الأذكار).
والمتوقع في الثالث تبويب اليوم دون خطأ. وتأكد أن أدوات الشاشة الرئيسية وإشعارات الصلاة ما زالت تفتح على شاشاتها.

## ٤. النشر

- سطر في `CHANGELOG.md` تحت «أُضيف»: يمكن لتطبيقات أخرى، مثل نتلو، فتح آناء على تبويب محدد.
- لا شيء يظهر للمستخدم داخل آناء نفسه، فلا حاجة لبطاقة «ما الجديد» ولا لمقطع شرح.
- أخبرني برقم الإصدار الذي يحمل هذا العقد بعد نشره. نتلو سيعمل مع النسخ الأقدم أيضاً، لكنها تفتح على شاشتها
  الرئيسية بدل التبويب، لأنها تتجاهل الإجراء الجديد.

## ٥. اختياري (ليس مطلوباً الآن)

في صفحة «حول» في آناء رابط لنتلو في المتجر. يمكن أن يفتح نتلو مباشرة إذا كان مثبتاً، ويرجع إلى المتجر إن لم يكن.
يحتاج ذلك في `AndroidManifest.xml`:

```xml
<queries>
    <package android:name="com.mushaf.reader" />
</queries>
```

## ما سيفعله نتلو (للعلم)

- في الإعدادات، تبويب «القائمة»، قسم «آناء» يختار فيه المستخدم أي الاختصارات تظهر في قائمة «المزيد».
- كل اختصار يرسل الطلب أعلاه مع `setPackage("com.sakinah.tasbih")`.
- يُستحسن أن يضيف نتلو الرايات `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`.
  آناء لا يحتاجها بعد هذا الإصدار، لكنها تجعل النسخ الأقدم تنتقل إلى شاشتها الرئيسية بدل أن تُفتح نسخة ثانية منها داخل نتلو.
- إن لم يكن آناء مثبتاً يظهر بند واحد «ثبّت آناء» يفتح صفحته في المتجر.
