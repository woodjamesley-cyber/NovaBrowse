# Keep WebView JS interfaces and extension runtime entry points.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.novabrowse.browser.extensions.** { *; }
-keepattributes *Annotation*
-dontwarn org.jetbrains.annotations.**
