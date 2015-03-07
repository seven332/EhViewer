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
-keepattributes SourceFile,LineNumberTable

-keep class com.hippo.ehviewer.widget.SearchView {*;}

-keep class com.hippo.ehviewer.gallery.image.Image {*;}

-keep class com.hippo.ehviewer.gallery.image.GifImage {*;}

-keepclassmembers class com.hippo.ehviewer.drawable.MaterialProgressDrawable$Ring {
    void set*(***);
    *** get*();
}

-keepclassmembers class * extends com.hippo.ehviewer.windowsanimate.Sprite {
    void set*(***);
    *** get*();
}

-keepclassmembers class * extends android.graphics.drawable.Drawable {
    void set*(***);
    *** get*();
}

-keepclassmembers class com.hippo.ehviewer.effect.ripple.Ripple {
    void set*(***);
    *** get*();
}

-keepclassmembers class com.hippo.ehviewer.effect.ripple.RippleBackground {
    void set*(***);
    *** get*();
}
