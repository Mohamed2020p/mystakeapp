package com.mystake.app

/**
 * Single place for every URL / override used by the app.
 *
 * GAME-PAGE TEST OVERRIDE
 * -----------------------
 * When the user navigates to any MyStake casino game page
 * (e.g. https://mystake.com/casino/gamepage/29076) the app loads
 * [TEST_URL_HTTPS] instead, so you can test your own page.
 * If https fails (DNS / TLS / HTTP error), the app automatically
 * retries with [TEST_URL_HTTP].
 *
 * While the test page is shown, a small "TEST MODE" chip is displayed
 * with a button to open the original MyStake game — so nobody mistakes
 * the test content for the real game.
 */
object AppConfig {

    /** Home page loaded on cold start. No welcome / onboarding screens. */
    const val HOME_URL = "https://mystake.com/"

    /** Any mystake.com URL containing this marker is treated as a game page. */
    private const val GAME_PAGE_MARKER = "/casino/gamepage/"

    /** Your test page — tried first (https). */
    const val TEST_URL_HTTPS = "https://chciken2website.rf.gd/test.html"

    /** Fallback if https cannot be loaded. */
    const val TEST_URL_HTTP = "http://chciken2website.rf.gd/test.html"

    fun isGamePageUrl(url: String?): Boolean {
        if (url == null) return false
        return url.contains("mystake.com") && url.contains(GAME_PAGE_MARKER)
    }

    fun isTestUrl(url: String?): Boolean {
        if (url == null) return false
        return url.startsWith(TEST_URL_HTTPS) ||
            url.startsWith(TEST_URL_HTTP) ||
            "chciken2website.rf.gd" in url
    }

    fun isTestHttpsUrl(url: String?): Boolean {
        if (url == null) return false
        return url.startsWith(TEST_URL_HTTPS) ||
            url.startsWith("https://chciken2website.rf.gd")
    }

    /**
     * Injected ONLY on your test page so it fits the screen cleanly:
     * adds a mobile viewport if missing and kills horizontal overflow.
     * MyStake pages are left untouched.
     */
    const val TEST_PAGE_FIT_JS = """(function(){
  try{
    var m=document.querySelector('meta[name="viewport"]');
    if(!m){
      m=document.createElement('meta');
      m.name='viewport';
      m.content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover';
      document.head.appendChild(m);
    }
    var s=document.createElement('style');
    s.textContent='html,body{margin:0!important;padding:0!important;width:100%!important;max-width:100vw!important;overflow-x:hidden!important;}img,video,iframe,canvas,embed,object{max-width:100%!important;}';
    document.head.appendChild(s);
  }catch(e){}
})();"""
}
