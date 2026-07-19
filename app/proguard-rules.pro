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

# EncryptedSharedPreferences / Tink (يحمي كلاسات التشفير من الحذف الخاطئ)
-keep class com.google.crypto.tink.** { *; }
-keep class androidx.security.crypto.** { *; }