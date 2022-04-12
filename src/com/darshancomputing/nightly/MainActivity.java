/*
    Copyright (c) 2017-2022 Darshan Computing, LLC
*/

package com.darshancomputing.nightly;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private static final String PREFS_FILE = "ma_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        if (Build.VERSION.SDK_INT >= 21) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.setStatusBarColor(0xff000000);
        }

        EditText editText = (EditText ) findViewById(R.id.editor);
        editText.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // @Override
    // public void onBackPressed() {
    //     // Protects against navigation, so before goBack and onBackPressed, but after other things
    //     if (protectedBack) {
    //         // TODO: Show UI about losing changes?  Cancel means return from method; continue means goBack/oBP
    //         if (wv.canGoBack()) {
    //             wv.goBack();
    //         } else {
    //             //Toast.makeText(this, "Unsent doodle!", Toast.LENGTH_SHORT).show();
    //             moveTaskToBack(true);
    //         }
    //     } else {
    //         if (wv.canGoBack())
    //             wv.goBack();
    //         else
    //             finish();
    //         //super.onBackPressed(); // This instead of finish()?
    //     }
    // }
}
