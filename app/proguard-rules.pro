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

-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.**

# ✅ يشيل كل استدعاءات Log.d / Log.v / Log.i نهائيًا من نسخة Release —
# حماية إضافية من تسريب أي بيانات حساسة (روابط، أسماء مستخدمين...) بسجلات الجهاز بالغلط،
# حتى لو انضاف سطر Log جديد مستقبلاً وما انتبهنا نشيله يدويًا
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}