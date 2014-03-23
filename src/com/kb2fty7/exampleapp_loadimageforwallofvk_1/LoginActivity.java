package com.kb2fty7.exampleapp_loadimageforwallofvk_1;

import com.perm.kate.api.Auth;


import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LoginActivity extends Activity {


	    WebView webview;
	    
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_login);
	        
	        webview = (WebView) findViewById(R.id.vkontakte_view);
	        webview.getSettings().setJavaScriptEnabled(true);
	        webview.clearCache(true);
	        
	        
	        webview.setWebViewClient(new VkontakteWebViewClient());
	                
	        CookieSyncManager.createInstance(this);
	        
	        CookieManager cookieManager = CookieManager.getInstance();
	        cookieManager.removeAllCookie();
	        
	        String url=Auth.getUrl(getString(R.string.id_application), Auth.getSettings());
	        webview.loadUrl(url);
	    }
	    
	    class VkontakteWebViewClient extends WebViewClient {
	        @Override
	        public void onPageStarted(WebView view, String url, Bitmap favicon) {
	            super.onPageStarted(view, url, favicon);
	            parseUrl(url);
	        }
	    }
	    
	    private void parseUrl(String url) {
	        try {
	            if(url==null)
	                return;
	            if(url.startsWith(Auth.redirect_url))
	            {
	                if(!url.contains("error=")){
	                    String[] auth=Auth.parseRedirectUrl(url);
	                    Intent intent=new Intent();
	                    intent.putExtra("token", auth[0]);
	                    intent.putExtra("user_id", Long.parseLong(auth[1]));
	                    setResult(Activity.RESULT_OK, intent);
	                }
	                finish();
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}