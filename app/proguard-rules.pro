-keep class com.dazou.iptvplayer.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
  <init>(...);
}
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$ImageType {
  **[] $VALUES;
  public *;
}

# EncryptedSharedPreferences / Tink
-keep class com.google.crypto.tink.** { *; }
-keep class androidx.security.crypto.** { *; }

# ✅ Tink فيها إشارات لميزة اختيارية (تحميل مفاتيح من رابط) غير مستخدمة بمشروعنا إطلاقًا،
# وتعتمد على مكتبات غير موجودة أصلًا بالتبعيات — هذا كود ميت آمن نتجاهله
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.**