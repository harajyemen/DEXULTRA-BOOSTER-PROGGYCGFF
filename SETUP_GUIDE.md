# دليل الإعداد والبناء 🔧

## الخطوة 1: تنزيل Gradle Wrapper

أول شيء بعد فك الضغط:

```bash
# Linux / Mac
cd DexUltraBoosterPro
gradle wrapper --gradle-version 8.4
chmod +x gradlew

# Windows (CMD)
cd DexUltraBoosterPro
gradle wrapper --gradle-version 8.4
```

> إذا لم يكن Gradle مثبتاً، نزّله من: https://gradle.org/install/

---

## الخطوة 2: البناء المباشر (Android Studio)

1. افتح Android Studio
2. `File → Open` واختر مجلد المشروع
3. انتظر sync المشروع
4. `Build → Build Bundle(s) / APK(s) → Build APK(s)`

---

## الخطوة 3: الرفع على GitHub

```bash
git init
git add .
git commit -m "Initial commit: DexUltra Booster Pro"
git remote add origin https://github.com/YOUR_USERNAME/DexUltraBoosterPro.git
git push -u origin main
```

---

## الخطوة 4: إعداد GitHub Actions

### إنشاء Keystore جديد

```bash
keytool -genkey -v \
  -keystore dexultra-release.jks \
  -alias dexultra \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

أجب على الأسئلة (اسم، مدينة، دولة، إلخ)

### إضافة Secrets في GitHub

1. اذهب إلى `Repository → Settings → Secrets and variables → Actions`
2. أضف:

```
KEYSTORE_BASE64  = (انتج بالأمر التالي)
KEYSTORE_PASSWORD = (كلمة مرور اخترتها)
KEY_ALIAS        = dexultra
KEY_PASSWORD     = (كلمة مرور المفتاح)
```

لتشفير الـ keystore:
```bash
# Linux/Mac
base64 -w 0 dexultra-release.jks | pbcopy  # Mac - ينسخ للحافظة
base64 -w 0 dexultra-release.jks           # Linux - اطبع ثم انسخ

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("dexultra-release.jks"))
```

---

## الخطوة 5: بناء Release

```bash
# بناء APK
git tag v1.0.0
git push origin v1.0.0

# أو تفعيل يدوي من GitHub → Actions → Run workflow
```

---

## ملفات ببجي المحقونة

### UserCustom.ini
الملف الأصلي يحدد إعدادات الجرافيكس والـ FPS.
يتم حقنه في: `/data/data/<pkg>/files/UserCustom.ini`

### Active.sav
يحمل بيانات تعريف الجهاز. بعد الحقن يظهر الجهاز كأنه يدعم 120 FPS.
يتم حقنه في: `/data/data/<pkg>/files/Active.sav`

### Shizuku ضروري لـ Android 12+
- نزّل Shizuku من Google Play
- فعّله عبر ADB أو wireless debugging
- امنح الإذن للتطبيق

---

## مسارات ببجي حسب الإصدار

| الإصدار | Package |
|---------|---------|
| Global | `com.tencent.ig` |
| KR | `com.pubg.krmobile` |
| VN | `com.vng.pubgmobile` |
| BGMI | `com.pubg.imobile` |
| Lite | `com.tencent.iglite` |
