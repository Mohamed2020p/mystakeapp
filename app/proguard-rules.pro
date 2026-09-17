# Keep JavascriptInterface methods (harmless default for WebView apps)
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
