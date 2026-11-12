package com.smart222tech.pasha18tv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Intent
import android.net.Uri
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable

private const val CURSOR_JS = """
(function(){
 if(window.__pashxxxCursorReady)return;window.__pashxxxCursorReady=true;
 var c=document.createElement('div');c.id='__pashxxx_tv_cursor';
 c.style.cssText='position:fixed;z-index:2147483647;left:50%;top:50%;width:25px;height:25px;margin:-12px 0 0 -12px;border:3px solid #fff;border-radius:50%;background:rgba(255,70,105,.28);box-shadow:0 0 0 2px #ff315b,0 2px 12px #000;pointer-events:none;';
 (document.body||document.documentElement).appendChild(c);
 var x=innerWidth/2,y=innerHeight/2;
 window.__pashxxxMove=function(dx,dy){x=Math.max(1,Math.min(innerWidth-1,x+dx));y=Math.max(1,Math.min(innerHeight-1,y+dy));c.style.left=x+'px';c.style.top=y+'px';};
 window.__pashxxxClick=function(){var e=document.elementFromPoint(x,y);if(!e)return;try{e.dispatchEvent(new MouseEvent('mousemove',{bubbles:true,clientX:x,clientY:y}));e.dispatchEvent(new MouseEvent('mousedown',{bubbles:true,clientX:x,clientY:y}));e.dispatchEvent(new MouseEvent('mouseup',{bubbles:true,clientX:x,clientY:y}));e.click();}catch(err){try{e.click();}catch(ignore){}}};
 window.__pashxxxPlayFullscreen=function(){
  var videos=Array.prototype.slice.call(document.querySelectorAll('video'));
  videos=videos.filter(function(v){var r=v.getBoundingClientRect(),s=getComputedStyle(v);return r.width>0&&r.height>0&&s.visibility!=='hidden'&&s.display!=='none';});
  videos.sort(function(a,b){var ar=a.getBoundingClientRect(),br=b.getBoundingClientRect();return (br.width*br.height)-(ar.width*ar.height);});
  var v=videos[0];if(!v)return false;
  try{var p=v.play();if(p&&p.catch)p.catch(function(){});}catch(ignore){}
  try{if(v.requestFullscreen){var f=v.requestFullscreen();if(f&&f.catch)f.catch(function(){});return true;}if(v.webkitEnterFullscreen){v.webkitEnterFullscreen();return true;}}catch(ignore){}
  return false;
 };
})();
"""

private class PashxxxWebView(context: Context) : WebView(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val script = when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> "window.__pashxxxMove&&window.__pashxxxMove(0,-44)"
                KeyEvent.KEYCODE_DPAD_DOWN -> "window.__pashxxxMove&&window.__pashxxxMove(0,44)"
                KeyEvent.KEYCODE_DPAD_LEFT -> "window.__pashxxxMove&&window.__pashxxxMove(-44,0)"
                KeyEvent.KEYCODE_DPAD_RIGHT -> "window.__pashxxxMove&&window.__pashxxxMove(44,0)"
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> "window.__pashxxxClick&&window.__pashxxxClick()"
                KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "window.__pashxxxPlayFullscreen&&window.__pashxxxPlayFullscreen()"
                else -> null
            }
            if (script != null) { evaluateJavascript(script, null); return true }
        }
        return super.dispatchKeyEvent(event)
    }
}

class TvBrowserActivity : Activity() {
    private lateinit var web: PashxxxWebView
    private lateinit var address: EditText
    private lateinit var shell: LinearLayout
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(8, 11, 18))
            isFocusable = true
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(20, 27, 39))
        }
        fun button(label: String, action: () -> Unit): Button =
            Button(this).apply { text = label; isFocusable = true; setOnClickListener { action() } }
        val back = button("‹") { if (web.canGoBack()) web.goBack() else finish() }
        val home = button("⌂") { address.setText("https://"); address.requestFocus() }
        address = EditText(this).apply {
            hint = "Введите адрес сайта"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setBackgroundColor(Color.rgb(35, 43, 57))
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(16, 4, 16, 4)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) { loadAddress(); true } else false
            }
            layoutParams = LinearLayout.LayoutParams(0, 52, 1f).apply { setMargins(8, 0, 8, 0) }
        }
        val go = button("ОТКРЫТЬ") { loadAddress() }
        bar.addView(back); bar.addView(home); bar.addView(address); bar.addView(go)
        shell.addView(bar, LinearLayout.LayoutParams(-1, 66))

        web = PashxxxWebView(this).apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            settings.mediaPlaybackRequiresUserGesture = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return true
                    return uri.scheme != "https" && uri.scheme != "http"
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    address.setText(url.orEmpty())
                    view?.evaluateJavascript(CURSOR_JS, null)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null) return
                    customView = view
                    customCallback = callback
                    shell.visibility = View.GONE
                    (window.decorView as ViewGroup).addView(view, ViewGroup.LayoutParams(-1, -1))
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                }
                override fun onHideCustomView() {
                    val view = customView ?: return
                    (window.decorView as ViewGroup).removeView(view)
                    customView = null
                    customCallback?.onCustomViewHidden()
                    customCallback = null
                    shell.visibility = View.VISIBLE
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
        shell.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        val hint = TextView(this).apply {
            text = "Пульт: стрелки двигают круглый указатель • OK нажимает • BACK возвращает назад"
            setTextColor(Color.LTGRAY)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(8, 6, 8, 6)
        }
        shell.addView(hint, LinearLayout.LayoutParams(-1, 34))
        setContentView(shell)
        address.setText("https://")
        address.requestFocus()
    }

    private fun loadAddress() {
        var raw = address.text.toString().trim()
        if (raw.isBlank()) return
        if (!raw.startsWith("https://") && !raw.startsWith("http://")) raw = "https://$raw"
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return
        if (uri.scheme != "https" && uri.scheme != "http") return
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(address.windowToken, 0)
        web.loadUrl(uri.toString())
        web.requestFocus()
    }

    override fun onBackPressed() {
        if (customView != null) {
            val view = customView
            if (view != null) (window.decorView as ViewGroup).removeView(view)
            customView = null
            customCallback?.onCustomViewHidden()
            customCallback = null
            shell.visibility = View.VISIBLE
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        } else if (::web.isInitialized && web.canGoBack()) {
            web.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::web.isInitialized) { web.stopLoading(); web.destroy() }
        super.onDestroy()
    }
}
