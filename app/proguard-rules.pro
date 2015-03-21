# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\Android\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# For bug report
-keepattributes SourceFile,LineNumberTable
-keep class com.hippo.**

# For ObjectAnimator
-keepclassmembers class com.hippo.effect.ripple.Ripple {
   void set*(***);
   *** get*();
}
-keepclassmembers class com.hippo.effect.ripple.RippleBackground {
   void set*(***);
   *** get*();
}
-keepclassmembers public class * extends com.hippo.drawable.VectorDrawable.VGroup {
   void set*(***);
   *** get*();
}
