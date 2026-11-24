# ApkNamer: خطوات التنفيذ (زيارة واحدة)

الحزمتان:
- `ApkNamer-v1.11.zip`: المشروع كاملاً، **لا تفكّه**، يُرفع كما هو.
- هذه الحزمة (`ApkNamer-manual-upload.zip`): ثلاثة ملفات لا يستطيع إسقاط الـ zip تحديثها، لأن الـ workflow يتجاهل `.github/workflows/` ومجلد `incoming/` عمداً:
  `.github/workflows/ci.yml` و`.github/workflows/release.yml` و`incoming/README.md`.

## الخطوات (بهذا الترتيب)

### 1) الملفات اليدوية
1. فك ضغط هذه الحزمة. (في macOS اضغط Cmd+Shift+. لإظهار المجلد المخفي `.github`.)
2. في المستودع: Add file ← Upload files، واسحب المجلدين `.github` و`incoming`. إن لم يقبل متصفحك سحب المجلدات فارفع كل ملف داخل مجلده الصحيح.
3. في خانة رسالة الـ commit اكتب: `Update workflows and incoming docs [skip ci]` ثم Commit directly to main.
4. احذف الملف `incoming/test.txt` (افتحه ← أيقونة سلة المهملات ← Commit برسالة تنتهي بـ `[skip ci]` أيضاً).

سبب `[skip ci]`: فحص النظافة الجديد يتحقق من النسخة النهائية، والمستودع ما زال بنسخته القديمة (فيها الرابط الوهمي `OWNER`)، فلو شُغّل الآن لفشل عمداً.

### 2) رفع المشروع
1. ارفع `ApkNamer-v1.11.zip` إلى مجلد `incoming/` (Add file ← Upload files).
2. افتح تبويب Actions وراقب «Release from incoming zip» (نحو 10 إلى 15 دقيقة).
3. عند النجاح تجد: commit جديداً من `github-actions[bot]`، واختفاء الـ zip من `incoming/`، وإصداراً `v1.11` في Releases ملاحظاته من CHANGELOG وفيها SHA-256.
4. إن فشل: لا يتغير المستودع ويبقى الـ zip في مكانه. انسخ سطر الخطأ الأحمر من سجل التشغيل.

### 3) التحقق
Actions ← CI ← Run workflow (فرع `main`). يجب أن ينجح الـ jobs (`Repository hygiene` و`build`)، وتتحول شارة CI في README إلى الأخضر.

### 4) إعدادات من واجهة GitHub (لا يمكن وضعها في ملفات)
- **About** (الترس بجانب About في الصفحة الرئيسية):
  - الوصف: `Batch-rename APK, XAPK, APKM and APKS files by their real contents (app name, package, version). Android, Kotlin, Jetpack Compose.`
  - Topics: `android` `apk` `xapk` `apkm` `apks` `file-renamer` `kotlin` `jetpack-compose`
- **Settings ← Code security**:
  - فعّل **Private vulnerability reporting** (ملف SECURITY.md يعتمد عليه).
  - فعّل Dependency graph وDependabot alerts وDependabot security updates.
  - فعّل Secret scanning مع Push protection.
  - في Code scanning فعّل CodeQL بالإعداد الافتراضي (Default setup).
- **حماية الفرع**: لا تفعّل «Require a pull request» على `main`، لأن workflow الإصدار يدفع مباشرة إلى `main`. يمكنك فقط منع force-push وحذف الفرع.
- **اختياري**:
  - صورة المعاينة الاجتماعية (Settings ← General ← Social preview، بقياس 1280×640).
  - لقطات شاشة للتطبيق في README. هذا أكبر تحسين بصري ممكن، ولا أستطيع إنشاءه لك.
  - إعداد توقيع الإصدارات حتى تُثبَّت التحديثات فوق النسخ السابقة: الخطوات في `incoming/README.md`.

## ما الذي تغيّر في هذا المستودع
- **README**: الروابط والشارات كانت تشير إلى `OWNER` الوهمي فصارت إلى `zigh2/ApkNamer`، وأُضيفت شارة الإصدار الأخير وقسم تنزيل وتثبيت واضح، ودُمج بندا التكرارات (وصفهما القديم «نفس الحزمة والإصدار» لم يعد صحيحاً منذ 1.9)، وأُضيفت رموز القوالب الجديدة والقالب الافتراضي، وحُذف من خارطة الطريق ما هو موجود فعلاً.
- **incoming/README.md** (كان مرجعاً مكسوراً في README وفي رسالة الـ workflow): شرح النشر وإعداد التوقيع.
- **CI**: مهل زمنية، وإلغاء التشغيلات المتكررة لنفس الـ PR، وفحص نظافة جديد (ملفات مطلوبة، لا `OWNER`، لا keystore مرفوع، روابط Markdown النسبية سليمة)، و`upload-artifact` من v5 إلى v6 (Node 24).
- **Release**: ملاحظات الإصدار تؤخذ من CHANGELOG مع SHA-256، ومهلة 45 دقيقة.
- **ملفات المجتمع**: رابط مباشر للإبلاغ الخاص عن الثغرات في SECURITY وCODE_OF_CONDUCT ومنتقي الـ Issues، وقسم «What CI checks» في CONTRIBUTING، و`.gitattributes`.
- **الإصدار**: 1.11 (11100)، مع بند في CHANGELOG.

## ما لم يُغيَّر عمداً
- كود التطبيق ومكتباته (AGP وKotlin وAndroidX وغيرها): لا أستطيع بناء Android هنا، ولا طريقة لاختبار ترقية كبيرة قبل نشرها. يبقى Dependabot الشهري يقترح التحديثات وCI يختبرها قبل الدمج.
- إصدارات Actions الأخرى (`checkout` و`setup-java` على v5، و`setup-gradle` على v6): تعمل كما هي.
