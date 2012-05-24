/*
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) 2005-2011, Nitobi Software Inc.
 * Copyright (c) 2010-2011, IBM Corporation
 */
package com.phonegap.plugins.slaveBrowser;

import java.io.IOException;
import java.io.InputStream;

import org.apache.cordova.api.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import android.view.Gravity;
import android.util.TypedValue;
import android.graphics.Color;
import android.graphics.Typeface;


import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;

import com.phonegap.api.PhonegapActivity;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;

public class SlaveBrowser extends Plugin {
  public String zeTitle;
  
  protected static final String LOG_TAG = "SlaveBrowser";
  private static int CLOSE_EVENT = 0;
  private static int LOCATION_CHANGED_EVENT = 1;
  private static int PAGE_LOADED = 2;

  private String browserCallbackId = null;
  


  private Dialog dialog;
  private WebView webview;
//  private EditText edittext; 
  private TextView edittext; 
  private boolean showLocationBar = true;

  /**
   * Executes the request and returns PluginResult.
   *
   * @param action    The action to execute.
   * @param args      JSONArry of arguments for the plugin.
   * @param callbackId  The callback id used when calling back into JavaScript.
   * @return        A PluginResult object with a status and message.
   */
  public PluginResult execute(String action, JSONArray args, String callbackId) {
    PluginResult.Status status = PluginResult.Status.OK;
    String result = "";

    try {
      if (action.equals("showWebPage")) {
        this.browserCallbackId = callbackId;
        
        // If the SlaveBrowser is already open then throw an error
        if (dialog != null && dialog.isShowing()) {
          return new PluginResult(PluginResult.Status.ERROR, "SlaveBrowser is already open");
        }
        result = this.showWebPage(args.getString(0), args.optJSONObject(1), args.getString(2));
        if (result.length() > 0) {
          status = PluginResult.Status.ERROR;
          return new PluginResult(status, result);
        } else {
          PluginResult pluginResult = new PluginResult(status, result);
          pluginResult.setKeepCallback(true);
          return pluginResult;
        }
      }
      else if (action.equals("close")) {
        closeDialog();
        
        JSONObject obj = new JSONObject();
        obj.put("type", CLOSE_EVENT);
        
        PluginResult pluginResult = new PluginResult(status, obj);
        pluginResult.setKeepCallback(false);
        return pluginResult;
      }
      else if (action.equals("openExternal")) {
        result = this.openExternal(args.getString(0), args.optBoolean(1));
        if (result.length() > 0) {
          status = PluginResult.Status.ERROR;
        }
      }
      else {
        status = PluginResult.Status.INVALID_ACTION;
      }
      return new PluginResult(status, result);
    } catch (JSONException e) {
      return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
    }
  }

  /**
   * Display a new browser with the specified URL.
   *
   * @param url       The url to load.
   * @param usePhoneGap   Load url in PhoneGap webview
   * @return        "" if ok, or error message.
   */
  public String openExternal(String url, boolean usePhoneGap) {
    try {
      Intent intent = null;
      if (usePhoneGap) {
        intent = new Intent().setClass(this.ctx.getContext(), org.apache.cordova.DroidGap.class);
        intent.setData(Uri.parse(url)); // This line will be removed in future.
        intent.putExtra("url", url);

        // Timeout parameter: 60 sec max - May be less if http device timeout is less.
        intent.putExtra("loadUrlTimeoutValue", 60000);

        // These parameters can be configured if you want to show the loading dialog
        intent.putExtra("loadingDialog", "Wait,Loading web page...");   // show loading dialog
        intent.putExtra("hideLoadingDialogOnPageLoad", true);       // hide it once page has completely loaded
      }
      else {
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
      }
      this.ctx.startActivity(intent);
      return "";
    } catch (android.content.ActivityNotFoundException e) {
      Log.d(LOG_TAG, "Error loading url "+url+":"+ e.toString());
      return e.toString();
    }
  }

  /**
   * Closes the dialog
   */
  private void closeDialog() {
    if (dialog != null) {
      dialog.dismiss();
    }
  }

  /**
   * Checks to see if it is possible to go back one page in history, then does so.
   */
  private void goBack() {
    if (this.webview.canGoBack()) {
      this.webview.goBack();
    }
  }

  /**
   * Checks to see if it is possible to go forward one page in history, then does so.
   */
  private void goForward() {
    if (this.webview.canGoForward()) {
      this.webview.goForward();
    }
  }

  /**
   * Navigate to the new page
   * 
   * @param url to load
   */
  private void navigate(String url) {    
    InputMethodManager imm = (InputMethodManager)this.ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);
    if (!url.startsWith("http")) {
      this.webview.loadUrl("http://" + url);      
    }
    this.webview.loadUrl(url);
    this.webview.requestFocus();
  }

  /**
   * Should we show the location bar?
   * 
   * @return boolean
   */
  private boolean getShowLocationBar() {
    return this.showLocationBar;
  }

  /**
   * Display a new browser with the specified URL.
   *
   * @param url       The url to load.
   * @param jsonObject 
   */
  public String showWebPage(final String url, JSONObject options, String myNewTitle) {
    // Determine if we should hide the location bar.
    if (options != null) {
      showLocationBar = options.optBoolean("showLocationBar", true);
    }
    zeTitle=myNewTitle;
    
    // Create dialog in new thread 
    Runnable runnable = new Runnable() {
      public void run() {
        dialog = new Dialog(ctx.getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
              try {
                JSONObject obj = new JSONObject();
                obj.put("type", CLOSE_EVENT);
                sendUpdate(obj, false);
              } catch (JSONException e) {
                Log.d(LOG_TAG, "Should never happen");
              }
            }
        });

        LinearLayout.LayoutParams backParams =
          new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams forwardParams =
          new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams editParams =
          new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT, 1.0f);
        LinearLayout.LayoutParams closeParams =
          new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams wvParams =
          new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        LinearLayout main =
          new LinearLayout(ctx.getContext());
        main.setOrientation(LinearLayout.VERTICAL);

        LinearLayout toolbar = new LinearLayout(ctx.getContext());
        toolbar.setOrientation(LinearLayout.HORIZONTAL);

        edittext = new TextView(ctx.getContext());
        edittext.setId(3);
        edittext.setSingleLine(true);
        edittext.setText(zeTitle);
        edittext.setTextSize(TypedValue.COMPLEX_UNIT_PX, 24);
        edittext.setGravity(Gravity.CENTER);
        edittext.setTextColor(Color.DKGRAY);
        edittext.setTypeface(Typeface.DEFAULT_BOLD);
        edittext.setLayoutParams(editParams);

        webview = new WebView(ctx.getContext());
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        
        // dda: intercept calls to console.log
        webview.setWebChromeClient(new WebChromeClient() {
          public boolean onConsoleMessage(ConsoleMessage cmsg) {
            // check secret prefix
            if (cmsg.message().startsWith("MAGICHTML")) {
              String msg = cmsg.message().substring(9); // strip off prefix
              /* process HTML */
              try {
                JSONObject obj = new JSONObject();
                obj.put("type", PAGE_LOADED);
                obj.put("html", msg);
                sendUpdate(obj, true);
              } catch (JSONException e) {
                Log.d(LOG_TAG, "This should never happen");
              }
              return true;
            }
            return false;
          }
        });
        
        // dda: inject the JavaScript on page load
        webview.setWebViewClient(new SlaveBrowserClient(edittext) {
          public void onPageFinished(WebView view, String address) {
            // have the page spill its guts, with a secret prefix
            view.loadUrl("javascript:console.log('MAGICHTML'+document.getElementsByTagName('html')[0].innerHTML);");
          }
        });

        webview.loadUrl(url);
        webview.setId(5);
        webview.setInitialScale(0);
        webview.setLayoutParams(wvParams);
        webview.requestFocus();
        webview.requestFocusFromTouch();

        toolbar.addView(edittext);

        if (getShowLocationBar()) {
          main.addView(toolbar);
        }
        main.addView(webview);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.FILL_PARENT;
        lp.height = WindowManager.LayoutParams.FILL_PARENT;
        
        dialog.setContentView(main);
        dialog.show();
        dialog.getWindow().setAttributes(lp);
      }
      
      private Bitmap loadDrawable(String filename) throws java.io.IOException {
        InputStream input = ctx.getAssets().open(filename);  
        return BitmapFactory.decodeStream(input);
      }
    };
    this.ctx.runOnUiThread(runnable);
    return "";
  }
  
  /**
   * Create a new plugin result and send it back to JavaScript
   * 
   * @param obj a JSONObject contain event payload information
   */
  private void sendUpdate(JSONObject obj, boolean keepCallback) {
    if (this.browserCallbackId != null) {
      PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
      result.setKeepCallback(keepCallback);
      this.success(result, this.browserCallbackId);
    }
  }

  /**
   * The webview client receives notifications about appView
   */
  public class SlaveBrowserClient extends WebViewClient {
    TextView edittext;

    /**
     * Constructor.
     * 
     * @param mContext
     * @param edittext 
     */
    public SlaveBrowserClient(TextView mEditText) {

    }     

    /**
     * Notify the host application that a page has started loading.
     * 
     * @param view      The webview initiating the callback.
     * @param url       The url of the page.
     */
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      super.onPageStarted(view, url, favicon);      
      String newloc;
      if (url.startsWith("http:") || url.startsWith("https:")) {
        newloc = url;
      } else {
        newloc = "http://" + url;
      }
      
      try {
        JSONObject obj = new JSONObject();
        obj.put("type", LOCATION_CHANGED_EVENT);
        obj.put("location", url);
        Log.e(LOG_TAG, "LOCATION_CHANGED_EVENT: "+url);
        sendUpdate(obj, true);
      } catch (JSONException e) {
        Log.d(LOG_TAG, "This should never happen");
      }
    }
  }
}
