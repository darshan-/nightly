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
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ScrollView;
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
import java.lang.Runnable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends Activity {
    private static final String PREFS = "settings";
    private static final String FILE = "nightly.txt";
    private static final String STASH = FILE + "~" ;

    private static final String PREF_CUR_POS = "cur_pos";
    private static final String PREF_SCROLL_Y = "scroll_y";
    private static final String PREF_MODIFIED = "modified";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private EditText editText;
    private TextView header;
    private ScrollView sv;
    private boolean modified;
    private boolean loaded;

    private void setModified() {
        modified = true;
        header.setText(android.text.Html.fromHtml("<font face=\"monospace\">*</font> Nightly"));
    }

    private void setUnmodified() {
        modified = false;
        header.setText("Nightly");
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        getMenuInflater().inflate(R.menu.menu, menu);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, 0);
        editor = prefs.edit();

        setContentView(R.layout.main_activity);

        //setHasOptionsMenu(true);
        //registerForContextMenu(findViewById(R.id.menu));

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.setStatusBarColor(0xff000000);

        editText = (EditText) findViewById(R.id.editor);
        editText.requestFocus();

        header = (TextView) findViewById(R.id.header);

        editText.addTextChangedListener(new TextWatcher () {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                setModified();
            }
            @Override
            public void beforeTextChanged(java.lang.CharSequence s, int start, int count, int after){}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });

        sv = findViewById(R.id.sv);
        // sv.setOnTouchListener(new View.OnTouchListener() {
        //     private boolean lastMEWasDown = false;
        //     private float lastX, lastY;

        //     @Override
        //     public boolean onTouch(View view, MotionEvent e) {
        //         int a = e.getActionMasked();

        //         if (a == MotionEvent.ACTION_DOWN) {
        //             lastMEWasDown = true;
        //             lastX = e.getX();
        //             lastY = e.getY();
        //             return false;
        //         }

        //         if (a == MotionEvent.ACTION_UP && lastMEWasDown) {
        //             InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //             imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        //             editText.setSelection(editText.getText().length());
        //         }

        //         if (a == MotionEvent.ACTION_MOVE && lastMEWasDown) {
        //             if (lastX - e.getX() < 6 &&
        //                 lastY - e.getY() < 6 &&
        //                 e.getX() - lastX < 6 &&
        //                 e.getY() - lastY < 6) return false;
        //         }

        //         lastMEWasDown = false;
        //         return false;
        //     }

        // });

        header.setOnTouchListener(new View.OnTouchListener() {
            private float lastX;

            @Override
            public boolean onTouch(View view, MotionEvent e) {
                int a = e.getActionMasked();

                if (a == MotionEvent.ACTION_DOWN) {
                    lastX = e.getX();
                    return false;
                }

                if (a == MotionEvent.ACTION_UP) {
                    if (e.getX() - lastX > 70) {
                        header.append("R");
                        return true;
                    } else if (lastX - e.getX() > 70) {
                        header.append("L");
                        return true;
                    }
                }

                return false;
            }

        });

        if (prefs.getBoolean(PREF_MODIFIED, false))
            restoreStashedBuffer();
        else
            loadFile();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) return;
        if (loaded) return;

        editText.setSelection(prefs.getInt(PREF_CUR_POS, 0));
        sv.scrollTo(0, prefs.getInt(PREF_SCROLL_Y, 0));

        loaded = true;
    }

    @Override
    public void onPause() {
        super.onPause();

        editor.putBoolean(PREF_MODIFIED, modified);
        editor.putInt(PREF_CUR_POS, editText.getSelectionStart());
        editor.putInt(PREF_SCROLL_Y, sv.getScrollY());

        editor.apply();

        if (modified)
            stashBuffer();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void hClick(android.view.View v) {
        if (modified) saveFile();
    }

    public void menuClick(android.view.View v) {
        //openOptionsMenu();
        //openContextMenu(findViewById(R.id.menu));

        //PopupMenu popup = new PopupMenu(this, v);
        //PopupMenu popup = new PopupMenu(this, findViewById(R.id.sv));
        //MenuInflater inflater = popup.getMenuInflater();
        //inflater.inflate(R.menu.menu, popup.getMenu());
        //PopupMenu popup = new PopupMenu(this, findViewById(R.id.sv), android.view.Gravity.TOP, 0, R.style.popup);
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.menu), android.view.Gravity.TOP|android.view.Gravity.RIGHT);
        popup.inflate(R.menu.menu);
        popup.show();
    }

    private void saveFile() {
        saveFile(FILE);
        setUnmodified();
    }

    private void loadFile() {
        loadFile(FILE);
        setUnmodified();
    }

    private void stashBuffer() {
        saveFile(STASH);
    }

    private void restoreStashedBuffer() {
        if (loadFile(STASH)) {
            removeStashFile();
            setModified();
        } else {
            loadFile();
        }
    }

    private void removeStashFile() {
        File f = new File(getFilesDir(), STASH);
        f.delete();
    }

    private void saveFile(String fname) {
        try {
            FileOutputStream fos = openFileOutput(fname, 0);
            fos.write(editText.getText().toString().getBytes());
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error writing file!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean loadFile(String fname) {
        try {
            FileInputStream fis = openFileInput(fname);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;

            while ((len = fis.read(buf)) != -1) {
                result.write(buf, 0, len);
            }
            String s = result.toString("UTF-8");
            editText.setText(s);
            editText.setSelection(s.length());
            editText.requestFocus();
        } catch (FileNotFoundException e) {
            //Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
            return false;
        } catch (Exception e) {
            Toast.makeText(this, "Error reading file!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}
