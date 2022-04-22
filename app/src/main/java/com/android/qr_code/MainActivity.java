package com.android.qr_code;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;



public class MainActivity extends Activity {

    public WebView webView;
    public WebView childWebView = null;

    public String baseUrl = "";
    public final static int SCANQR_PAGE = 49374;
    String Tag = " MainAct ";

    public CookieManager cookieManager;
    public Context mainContext;

    private SharedPreferences pref;
    //SharedPreferences 구분
    private final String sharedName = "userInfo";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainContext = this;

        cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.acceptCookie();

        webInit();

    }

    //웹뷰 셋팅
    public void webInit(){
        //원격디버깅 테스트용 :
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setWebContentsDebuggingEnabled(true);
        }

        webView = (WebView) findViewById(R.id.webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(true);

        //userAgent set
        String userAgent = webView.getSettings().getUserAgentString();
        webView.getSettings().setUserAgentString(userAgent + "QR-Code");

        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.clearHistory();
        webView.clearFormData();
        webView.clearCache(true);


        //javascript Bridge 연결
        webView.addJavascriptInterface(new AndroidBridge(), "android");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                //return super.onJsAlert(view, url, message, result);
                //result.confirm();
                Log.e("asdd", Tag + " 337 === onJsAlert() = " + message);
                new AlertDialog.Builder(MainActivity.this).setTitle("").setMessage(message)
                        .setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // TODO Auto-generated method stub
                                result.confirm();
                            }
                        })
                        .setOnCancelListener(new AlertDialog.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface arg0) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false).create().show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                result.confirm();
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
				Log.e("asdd", Tag + " 362 === onCreateWindow() = " + resultMsg.toString());
//				Log.e("asdd", Tag + " 363 === onCreateWindow() isDialog = " + isDialog);

                view.removeAllViews();
                //webView.setVisibility(View.GONE);
                childWebView = new WebView(MainActivity.this);
                childWebView.getSettings().setJavaScriptEnabled(true);
                childWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                childWebView.setWebChromeClient(this);
                childWebView.setWebViewClient(new WebViewClient());
                childWebView.setLayoutParams(new LinearLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

                webView.addView(childWebView);

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(childWebView);
                resultMsg.sendToTarget();
                Log.e("asdd", Tag + " 391 === onCreateWindow() = " + childWebView.getUrl());

                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                webView.removeView(window);
            }

        });

        baseUrl = "https://www.naver.com";
        webView.loadUrl(baseUrl);
    }


    //자바스크립트 연결(Bridge)
    private class AndroidBridge {

        @JavascriptInterface
        public void externalBrowser(String[] arry) {
            final String[] strArry = new String[10];
            strArry[0] = arry[0];

        }

        //QR Scan javascript call
        @JavascriptInterface
        public void goScanQR() {
            Log.e("asdd", Tag + " 534 === Javascript call goScanQR()");
            new IntentIntegrator(MainActivity.this).initiateScan();
        }

        //setSharedPreferencesString(데이터 저장)
        @JavascriptInterface
        public void setSharedPreferencesString(final String key, final String value) {
            Log.e("asdd", Tag + " 535 === Javascript call setSharedPreferencesString(key, value)");

            pref = getSharedPreferences(sharedName, MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(key, value);
            editor.commit();
        }

        //getSharedPreferencesString(데이터 불러오기)
        @JavascriptInterface
        public String getSharedPreferencesString(final String key) {
            Log.e("asdd", Tag + " 535 === Javascript call getSharedPreferencesString(key)");
            pref = getSharedPreferences(sharedName, MODE_PRIVATE);
            String result = pref.getString(key,"");
            return result;
        }

        //clearSharedPreferencesData(데이터 삭제)
        @JavascriptInterface
        public void clearSharedPreferencesData() {
            Log.e("asdd", Tag + " 535 === Javascript call clearSharedPreferencesData()");
            pref = getSharedPreferences(sharedName, MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.clear();
            editor.commit();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.e("asdd", Tag + " 876 === onPause()");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e("asdd", Tag + " 1124 === onDestroy()");
        super.onDestroy();
        try {
            cookieManager.removeSessionCookie();
        } catch (NullPointerException e) {
            Log.e("asdd", this.getClass() + " 1571 === NullPointerException occured ");
        } catch (Exception e) {
            Log.e("asdd", this.getClass() + " 1573 === Exception occured ");
        }

        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        webView.removeAllViews();
        webView.clearSslPreferences();
        webView.destroy();

        System.exit(0);
        //android.os.Process.killProcess(Process.myPid());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("asdd", Tag + " 1036 === onActivityResult() requestCode = " + requestCode);
        Log.e("asdd", Tag + " 1037 === onActivityResult() resultCode = " + resultCode);


        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                //QR Scanner 인식
                case SCANQR_PAGE:
                    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    if(result != null) {
                        if(result.getContents() == null) {
                            Toast.makeText(this, "스캔인식을 실패하였습니다. 다시 시도 해주세요.", Toast.LENGTH_LONG).show();

                        } else {
                            webView.loadUrl("javascript:SuccessScanQR('"+result.getContents()+"')");
                            //Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                            //Toast.makeText(this, "스캔이 완료 되었습니다.", Toast.LENGTH_LONG).show();

                        }
                    } else {
                        super.onActivityResult(requestCode, resultCode, data);
                    }
                    break;


                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {

        }
    }
}