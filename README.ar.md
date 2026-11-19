# APK Namer (نسخة عربية مختصرة)

تطبيق أندرويد يعيد تسمية ملفات **APK و XAPK و APKM و APKS** بناءً على **محتواها الحقيقي**:
اسم التطبيق، اسم الحزمة، رقم الإصدار. مثال: `download (3).xapk` ← `Telegram v10.14.5 (5034).xapk`.

## المميزات
- يدعم APK وXAPK وAPKM وAPKS (وامتدادات APKX وASPK).
- **فحص عميق**: يكتشف الحزم حتى لو كان امتدادها `.zip` أو بلا امتداد.
- قالب تسمية قابل للتخصيص مع معاينة فورية، مثل `{name}< v{version}>< ({code})>`.
- لا يستبدل أي ملف موجود، ويحل تعارض الأسماء تلقائياً، ويدعم **التراجع** عن آخر دفعة.
- **لا يستخدم الإنترنت إطلاقاً**، وواجهته عربية وإنجليزية مع دعم كامل لاتجاه RTL.

## النشر التلقائي
ارفع ملف `ApkNamer-vX.Y.zip` إلى مجلد `incoming/` فيستبدل GitHub Actions ملفات المشروع كلها، ويشغّل الاختبارات،
ثم يبني APK وينشره في صفحة Releases باسم يحمل الإصدار، مثل `ApkNamer-v1.10.apk`. آخر نسخة: `releases/latest`.
التفاصيل وإعداد التوقيع في [incoming/README.md](incoming/README.md).

## البناء
```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## الصلاحيات
يطلب التطبيق صلاحية «الوصول إلى جميع الملفات» ليبحث في المجلدات ويعيد التسمية في مكانها.
لذلك يُوزَّع عبر GitHub وF-Droid وليس عبر Google Play.

للتفاصيل التقنية والمساهمة راجع [README.md](README.md) و[CONTRIBUTING.md](CONTRIBUTING.md).
