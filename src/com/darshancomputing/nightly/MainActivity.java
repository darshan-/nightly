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
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



// I guess the "right" way to do it is like Signal, with the storage framework -- have user manually
//   pick a folder.  Then the app can handle the rest?
// Other two options are targeting API level 18, as I've done for now, or requesting root.  I don't think
//   there are any other options, as having the file removed if the app is uninstalled is super revolting to me.
// I just want it to be simply, but the prompt asking for storage permissions and then another complaining about
//   targeting an old API level makes it kinda clunky anyway...


public class MainActivity extends Activity {
    private static final String PREFS_FILE = "settings";
    private static final String FILE = "/mnt/sdcard/Nightly0001";
    // private static final String P_READ = android.Manifest.permission.READ_EXTERNAL_STORAGE;
    // private static final int PR_READ = 1;
    // private static final String P_WRITE = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
    // private static final int PR_WRITE = 2;

    private SharedPreferences prefs;
    private boolean lastMEWasDown = false;
    private float lastX, lastY;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.setStatusBarColor(0xff000000);

        editText = (EditText) findViewById(R.id.editor);
        editText.requestFocus();

        TextView header = (TextView) findViewById(R.id.header);
        header.setText("Nightly");

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

        //if (checkReadPermission() && checkWritePermission())
        //init();
        loadFile();
    }

    void init() {
        //if (checkReadPermission() && checkWritePermission())
        //loadFile();
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

    // public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    //     System.out.println("....................... oRPR");
    //     switch (requestCode) {
    //     case PR_READ:
    //         if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    //             init();
    //         } else {
    //             Toast.makeText(this, "Need storage read permission", Toast.LENGTH_SHORT).show();
    //             checkReadPermission();
    //         }

    //         break;
    //     case PR_WRITE:
    //         if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    //             init();
    //         } else {
    //             Toast.makeText(this, "Need storage write permission", Toast.LENGTH_SHORT).show();
    //             checkWritePermission();
    //         }

    //         break;
    //     }
    // }

    // private boolean checkReadPermission() {
    //     System.out.println("....................... check READ perm");

    //     if (checkSelfPermission(P_READ) != PackageManager.PERMISSION_GRANTED) {
    //         System.out.println("....................... requesting read perm");
    //         requestPermissions(new String[]{P_READ}, PR_READ);
    //         return false;
    //     }
    //     System.out.println("....................... HAVE read perm");

    //     return true;
    // }

    // private boolean checkWritePermission() {
    //     System.out.println("....................... check READ perm");
    //     if (checkSelfPermission(P_WRITE) != PackageManager.PERMISSION_GRANTED) {
    //         System.out.println("....................... requesting write perm");
    //         requestPermissions(new String[]{P_WRITE}, PR_WRITE);
    //         return false;
    //     }
    //     System.out.println("....................... HAVE write perm");

    //     return true;
    // }

    public void svClick(android.view.View v) {
        //EditText editText = (EditText ) findViewById(R.id.editor);
        //if (editText != null)
            //editText.setText("test");
            //editText.requestFocus();
    }

    public void hClick(android.view.View v) {
        saveFile();
    }

    private void saveFile() {
    }

    private void loadFile() {
        StringBuffer result = new StringBuffer();

        try {
            // Uri uri = Uri.fromFile(new File(FILE));
            // File fu = new File(uri.getPath());

            // if (fu.isFile()) {
            //     System.out.println("....................... IS FILE!  YAY!!!");
            // } else {
            //     System.out.println("....................... is NOT file!  BOOOO!!!");
            // }

            // FileReader f = new FileReader(uri.getPath().toString());
            // File file = new File(uri.getPath().toString());

            FileReader f = new FileReader(FILE);
            File file = new File(FILE);

            if (f == null)
                throw(new FileNotFoundException());

            if (file.isDirectory())
                throw(new IOException());

            // if the file has nothing in it there will be an exception here
            // that actually isn't a problem
            if (file.length() != 0 && !file.isDirectory()) {
                char[] buffer;
                buffer = new char[1024];
                int read = 0;

                do {
                    read = f.read(buffer, 0, 1000);

                    if (read >= 0)
                        result.append(buffer, 0, read);
                } while (read >= 0);
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error reading file!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unknown error!", Toast.LENGTH_SHORT).show();
        }

        editText.requestFocus();
        editText.setText(result.toString());
    }
}
