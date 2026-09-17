package com.mystake.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var retryButton: Button
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var testChip: MaterialCardView
    private lateinit var testChipOriginalButton: Button
    private lateinit var testChipClose: Button

    // Fullscreen video / game support
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // Game-page test-override state
    private var lastOriginalGameUrl: String? = null
    private var bypassInterceptionOnce = false
    private var triedHttpFallback = false
    private var testChipDismissed = false

    // File upload (KYC docs, avatars, ...)
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val uris: Array<Uri>? = if (result.resultCode == RESULT_OK && data != null) {
            val clip = data.clipData
            when {
                clip != null -> Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                data.data != null -> arrayOf(data.data!!)
                else -> null
            }
        } else {
            null
        }
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: the WebView takes the FULL screen size.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        errorView = findViewById(R.id.errorView)
        errorText = findViewById(R.id.errorText)
        retryButton = findViewById(R.id.retryButton)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)
        testChip = findViewById(R.id.testChip)
        testChipOriginalButton = findViewById(R.id.testChipOriginalButton)
        testChipClose = findViewById(R.id.testChipClose)

        // Keep the thin progress bar below the status bar, and the test chip
        // above the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(progressBar) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            (v.layoutParams as ViewGroup.MarginLayoutParams).topMargin = top
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(testChip) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            (v.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = bottom + dp(16)
            insets
        }

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            loadsImagesAutomatically = true
            // Fit content to the screen, no zoom clutter / overflow UI.
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            safeBrowsingEnabled = true
        }
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.overScrollMode = View.OVER_SCROLL_NEVER

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()

                // 1) Game-page test override: load YOUR test page instead.
                if (!bypassInterceptionOnce && AppConfig.isGamePageUrl(url)) {
                    lastOriginalGameUrl = url
                    triedHttpFallback = false
                    testChipDismissed = false
                    view.loadUrl(AppConfig.TEST_URL_HTTPS)
                    return true
                }
                // One-time bypass when the user taps "ORIGINAL" on the test chip.
                if (bypassInterceptionOnce && AppConfig.isGamePageUrl(url)) {
                    bypassInterceptionOnce = false
                    return false
                }

                // 2) Keep all http(s) navigation inside the app.
                when (request.url.scheme?.lowercase()) {
                    "http", "https" -> return false
                }

                // 3) External schemes (tel:, mailto:, intent:, ...) -> other apps.
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                } catch (e: ActivityNotFoundException) {
                    true
                }
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                errorView.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                updateTestChip(url)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                if (url != null && AppConfig.isTestUrl(url)) {
                    view.evaluateJavascript(AppConfig.TEST_PAGE_FIT_JS, null)
                }
                updateTestChip(url)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (!request.isForMainFrame) return
                val failingUrl = request.url.toString()
                // https -> http fallback for the test page.
                if (AppConfig.isTestHttpsUrl(failingUrl) && !triedHttpFallback) {
                    triedHttpFallback = true
                    view.loadUrl(AppConfig.TEST_URL_HTTP)
                    return
                }
                showError(getString(R.string.error_no_connection))
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (!request.isForMainFrame) return
                val failingUrl = request.url.toString()
                // e.g. https serves an error page -> retry over http once.
                if (AppConfig.isTestHttpsUrl(failingUrl) && !triedHttpFallback) {
                    triedHttpFallback = true
                    view.loadUrl(AppConfig.TEST_URL_HTTP)
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                // Free hosting often has broken HTTPS: fall back to http for the
                // test page instead of showing an SSL warning. Everything else is
                // blocked as usual (secure default).
                val failingUrl = error.url
                if (failingUrl != null && AppConfig.isTestHttpsUrl(failingUrl) && !triedHttpFallback) {
                    triedHttpFallback = true
                    handler.cancel()
                    view.loadUrl(AppConfig.TEST_URL_HTTP)
                    return
                }
                handler.cancel()
                showError(getString(R.string.error_ssl))
            }
        }

        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    onHideCustomView()
                }
                customView = view
                customViewCallback = callback
                fullscreenContainer.addView(
                    view,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE
            }

            override fun onHideCustomView() {
                fullscreenContainer.visibility = View.GONE
                fullscreenContainer.removeAllViews()
                customView = null
                webView.visibility = View.VISIBLE
                try {
                    customViewCallback?.onCustomViewHidden()
                } catch (_: Exception) {
                }
                customViewCallback = null
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                try {
                    request.grant(request.resources)
                } catch (_: Exception) {
                    request.deny()
                }
            }

            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                return try {
                    fileChooserLauncher.launch(params.createIntent())
                    true
                } catch (e: ActivityNotFoundException) {
                    filePathCallback = null
                    false
                }
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message
            ): Boolean {
                // Open popups (window.open) in the SAME WebView so the user
                // never leaves the app.
                return try {
                    val transport = resultMsg.obj as WebView.WebViewTransport
                    val popup = WebView(this@MainActivity)
                    popup.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                            webView.loadUrl(r.url.toString())
                            return true
                        }
                    }
                    transport.webView = popup
                    resultMsg.sendToTarget()
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }

        retryButton.setOnClickListener {
            errorView.visibility = View.GONE
            if (AppConfig.isTestUrl(webView.url)) {
                // Retry the test page from https again (fallback will re-arm).
                triedHttpFallback = false
                webView.loadUrl(AppConfig.TEST_URL_HTTPS)
            } else {
                webView.reload()
            }
        }

        testChipOriginalButton.setOnClickListener {
            lastOriginalGameUrl?.let { original ->
                bypassInterceptionOnce = true
                testChipDismissed = true
                testChip.visibility = View.GONE
                webView.loadUrl(original)
            }
        }
        testChipClose.setOnClickListener {
            testChipDismissed = true
            testChip.visibility = View.GONE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    customView != null -> webView.webChromeClient?.onHideCustomView()
                    webView.canGoBack() -> webView.goBack()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            // Cold start: straight into mystake.com — no splash / welcome screens.
            val deepLink = intent?.data?.toString()
            if (deepLink != null && (deepLink.startsWith("http://") || deepLink.startsWith("https://"))) {
                webView.loadUrl(deepLink)
            } else {
                webView.loadUrl(AppConfig.HOME_URL)
            }
        }
    }

    private fun updateTestChip(url: String?) {
        if (url != null && AppConfig.isTestUrl(url) && !testChipDismissed) {
            testChip.visibility = View.VISIBLE
        } else {
            testChip.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        errorText.text = message
        errorView.visibility = View.VISIBLE
        testChip.visibility = View.GONE
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }
}
