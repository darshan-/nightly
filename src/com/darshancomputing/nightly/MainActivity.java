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
import android.view.inputmethod.InputMethodManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
    private boolean lastMEWasDown = false;
    private float lastX, lastY;

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

        EditText editText = (EditText) findViewById(R.id.editor);
        editText.requestFocus();

        findViewById(R.id.sv).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                //EditText editText = (EditText) findViewById(R.id.editor);
                //if (editText != null)
                //editText.setText("YOYO");
                //editText.requestFocusFromTouch();

                /*

                  Okay, let's actually look at the MotionEvent --
                  I guess like swapadoodle, keep track of last event.
                  If last event was down, and this is up, then show keyboard and return true.
                  Otherwise don't do anything and return false.

                  lastMEWasDown

                  (maybe always return false?  Certainly we don't want to open keyboad if scrolling, though,
                    so definitely want to keep track of down/up)

                 */

                int a = e.getActionMasked();

                String s = "";
                //editText.setText("action: " + a);
                if (a == MotionEvent.ACTION_DOWN) s += "D: ";
                if (a == MotionEvent.ACTION_MOVE) s += "M: ";
                if (a == MotionEvent.ACTION_UP) s += "U: ";
                s += e.getX() + ", " + e.getY();
                //editText.setText(s);
                //editText.append(s + "\n");

                if (a == MotionEvent.ACTION_DOWN) {
                    lastMEWasDown = true;
                    lastX = e.getX();
                    lastY = e.getY();
                    return false;
                }

                if (a == MotionEvent.ACTION_UP && lastMEWasDown) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                }

                if (a == MotionEvent.ACTION_MOVE && lastMEWasDown) {
                    if (lastX - e.getX() < 6 &&
                        lastY - e.getY() < 6 &&
                        e.getX() - lastX < 6 &&
                        e.getY() - lastY < 6) return false;
                }

                lastMEWasDown = false;
                return false;
            }

        });
        // findViewById(R.id.sv).setOnClickListener(new View.OnClickListener() {
        //     @Override
        //     public void onClick(View view) {
        //         //EditText editText = (EditText ) findViewById(R.id.editor);
        //         //if (editText != null)
        //         editText.setText("rara");
        //     }
        // });
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

    public void svClick(android.view.View v) {
        //EditText editText = (EditText ) findViewById(R.id.editor);
        //if (editText != null)
            //editText.setText("test");
            //editText.requestFocus();
    }
}
