# DexUltra Booster Pro 🚀

**مُحسّن ببجي موبايل الاحترافي**

[![Build APK](https://github.com/YOUR_USERNAME/DexUltraBoosterPro/actions/workflows/build-apk.yml/badge.svg)](https://github.com/YOUR_USERNAME/DexUltraBoosterPro/actions/workflows/build-apk.yml)
[![Build AAB](https://github.com/YOUR_USERNAME/DexUltraBoosterPro/actions/workflows/build-aab.yml/badge.svg)](https://github.com/YOUR_USERNAME/DexUltraBoosterPro/actions/workflows/build-aab.yml)

---

## الميزات

| الميزة | الوصف |
|--------|-------|
| 🚀 **تعزيز الأداء** | تنظيف RAM، تحسين CPU، إعداد الشبكة |
| 💉 **حقن 120 FPS** | UserCustom.ini + Active.sav لجميع إصدارات ببجي |
| ⚡ **Shizuku** | وصول مباشر لملفات data/data (Android 12+) |
| 🎯 **ماشر عائم** | Crosshair قابل للسحب فوق اللعبة |
| 🌀 **الحساسية** | 8 إعدادات مسبقة (مع/بدون جيرو) |
| 📡 **محسّن البنق** | DNS مخصص + VPN بنق فقط |
| 💡 **النصائح** | 15+ نصيحة احترافية |

---

## البناء

### متطلبات
- Android Studio Hedgehog أو أحدث
- JDK 17
- Android SDK 34

### البناء المحلي
```bash
# APK للتطوير
./gradlew assembleDebug

# APK للإصدار (يحتاج keystore)
./gradlew assembleRelease

# AAB لـ Google Play
./gradlew bundleRelease
```

### البناء عبر GitHub Actions

1. رفع المشروع إلى GitHub
2. إضافة Secrets في `Settings → Secrets → Actions`:

| Secret | الوصف |
|--------|-------|
| `KEYSTORE_BASE64` | الـ keystore مشفر بـ base64 |
| `KEYSTORE_PASSWORD` | كلمة مرور الـ keystore |
| `KEY_ALIAS` | اسم المفتاح |
| `KEY_PASSWORD` | كلمة مرور المفتاح |

3. إنشاء tag لبناء release:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## إنشاء Keystore

```bash
keytool -genkey -v -keystore dexultra-release.jks \
  -alias dexultra -keyalg RSA -keysize 2048 \
  -validity 10000

# تشفير للـ GitHub Secret
base64 -w 0 dexultra-release.jks
```

---

## هيكل المشروع

```
app/src/main/
├── java/com/dex/ultra/booster/pro/
│   ├── SplashActivity.kt         # شاشة البداية
│   ├── PermissionsActivity.kt    # إدارة الأذونات
│   ├── MainActivity.kt           # الصفحة الرئيسية + التعزيز
│   ├── SensitivityActivity.kt    # إعدادات الحساسية
│   ├── TipsActivity.kt           # النصائح
│   ├── PingOptimizerActivity.kt  # محسّن البنق
│   ├── ShizukuHelper.kt          # تكامل Shizuku
│   ├── FileInjector.kt           # حقن ملفات ببجي
│   ├── PerformanceOptimizer.kt   # تحسين الأداء
│   ├── OverlayService.kt         # الماشر العائم
│   └── DnsVpnService.kt          # DNS VPN
└── res/
    ├── layout/                   # ملفات UI
    ├── values/                   # ألوان، نصوص، أنماط
    └── drawable/                 # الأيقونات
```

---

## الأذونات

| الإذن | السبب |
|-------|-------|
| `MANAGE_EXTERNAL_STORAGE` | قراءة/كتابة ملفات ببجي |
| `SYSTEM_ALERT_WINDOW` | الماشر العائم |
| `BIND_VPN_SERVICE` | محسّن DNS/البنق |
| `rikka.shizuku.permission.API_V23` | Shizuku ADB access |

---

## الدعم الفني

- جميع إصدارات PUBG Mobile مدعومة
- Android 6.0+ (minSdk 23)
- مُحسَّن لـ Android 12 و 13 و 14

---

**⚠️ تنبيه:** هذا التطبيق مخصص للتحسين التقني فقط. استخدامه على مسؤوليتك الخاصة.
