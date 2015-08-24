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

# For ObjectAnimator
-keepclassmembers public class * extends com.hippo.scene.Scene {
    void setBackgroundColor(int);
    int getBackgroundColor();
}
-keepclassmembers class com.hippo.drawable.DrawerArrowDrawable {
    void setProgress(float);
    float getProgress();
}
-keepclassmembers class com.hippo.drawable.AddDeleteDrawable {
    void setProgress(float);
    float getProgress();
}
-keepclassmembers class com.hippo.ehviewer.widget.SearchBar {
    void setProgress(float);
    float getProgress();
}

# Scene
-keepclassmembers class * extends com.hippo.scene.preference.PreferenceBase {
    public static android.support.v7.widget.RecyclerView$ViewHolder createViewHolder(android.content.Context, android.view.ViewGroup);
}

# API 23 remove most stuff in org.apache.http
-dontwarn org.apache.http.**
