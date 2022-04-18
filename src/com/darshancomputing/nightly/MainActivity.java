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
import android.view.Gravity;
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
import java.lang.CharSequence;
import java.lang.Runnable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends Activity {
    private static final String PREFS = "settings";
    private static final String MEDS = "Meds";
    private static final String NOTES = "Notes";

    // private static final String PREF_M_CUR_POS = "m_cur_pos";
    // private static final String PREF_M_SCROLL = "m_scroll";
    // private static final String PREF_M_MODIFIED = "m_modified";
    // private static final String PREF_M_STASH = "m_stash";
    // private static final String PREF_N_CUR_POS = "n_cur_pos";
    // private static final String PREF_N_SCROLL = "n_scroll";
    // private static final String PREF_N_MODIFIED = "n_modified";
    // private static final String PREF_N_STASH = "n_stash";
    private static final String PREF_X_CUR_POS = "_cur_pos";
    private static final String PREF_X_SCROLL = "_scroll";
    private static final String PREF_X_MODIFIED = "_modified";
    private static final String PREF_X_SAVED_TEXT = "_saved_text";
    private static final String PREF_X_TEXT = "_text";
    private static final String PREF_CUR_BUF = "cur_buf";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private EditText editText;
    private TextView header;
    private ScrollView sv;
    private boolean modified;
    private String bufName;
    private boolean loaded;
    private boolean loadingBuffer;

    private class Buffer {
        private int curPos, scroll;
        private boolean modified;
        private String text, bufName;

        private Buffer(String name) {
            bufName = name;

            curPos = prefs.getInt(bufName + PREF_X_CUR_POS, 0);
            scroll = prefs.getInt(bufName + PREF_X_SCROLL, 0);
            modified = prefs.getBoolean(bufName + PREF_X_MODIFIED, false);
            if (modified)
                text = prefs.getString(bufName + PREF_X_TEXT, "");
            else
                text = prefs.getString(bufName + PREF_X_SAVED_TEXT, "");
        }

        private void save() {
            if (!modified) return;

            modified = false;
            putState();
            editor.putString(bufName + PREF_X_SAVED_TEXT, editText.getText().toString());

            editor.commit();

            setHeader();
        }

        private void stash() {
            putState();

            if (modified)
                editor.putString(bufName + PREF_X_TEXT, editText.getText().toString());

            editor.commit();
        }

        private void putState() {
            editor.putInt(bufName + PREF_X_CUR_POS, curPos);
            editor.putInt(bufName + PREF_X_SCROLL, scroll);
            editor.putBoolean(bufName + PREF_X_MODIFIED, modified);
        }

        // private Buffer(Buffer b) {
        //     //b.sync(); // What if it's not the current buffer?

        //     curPos = b.curPos;
        //     scroll = b.scroll;
        //     modified = b.modified;
        //     text = b.text;
        //     bufName = b.bufName;
        // }

        // private void sync() {
        //     curPos = editText.getSelectionStart();
        //     scroll = sv.getScrollY();
        //     text = editText.getText().toString();
        // }

        // private void makeActive() {
        //     editText.setText(text);
        //     editText.setSelection(curPos);
        //     sv.scrollTo(0, scroll);
        //     setHeader();
        // }

        //private Buffer dup()
    }

    private Buffer cur, oth;

    // private int oCurPos, oScroll;
    // private boolean oModified;
    // private String oText, oBufName;;

    private void loadBuffer(Buffer b) {
        loadingBuffer = true;
        editText.setText(b.text);
        loadingBuffer = false;

        try {
            editText.setSelection(b.curPos);
        } catch (Exception e) {
            editText.setSelection(b.text.length());
        }
        editText.requestFocus();
        sv.scrollTo(0, b.scroll);

        setHeader();
    }

    private void syncToCur() {
        cur.curPos = editText.getSelectionStart();
        cur.scroll = sv.getScrollY();
        cur.text = editText.getText().toString();
    }

    private void setModified(boolean m) {
        cur.modified = m;
        setHeader();
    }

    // * ⁕ ∗ ⋇ ☀ ☸ ☼ ⚐ ⚑ ⚠ ⚹ ✳ ✻ ❊ ❋ † ‡ Δ ※ ‼ ⁑
    private void setHeader() {
        String s = "";

        if (cur.modified) s += "✳ ";
        if (oth.modified) s += "(✳) ";

        header.setText(s + cur.bufName);
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

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.setStatusBarColor(0xff000000);

        editText = (EditText) findViewById(R.id.editor);
        //editText.requestFocus();

        header = (TextView) findViewById(R.id.header);

        editText.addTextChangedListener(new TextWatcher () {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!loadingBuffer) setModified(true);
            }
            @Override
            public void beforeTextChanged(java.lang.CharSequence s, int start, int count, int after){}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });

        sv = findViewById(R.id.sv);
        sv.setSmoothScrollingEnabled(false);

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
                    float hDelta = e.getX() - lastX;

                    if (hDelta > 70 || hDelta < -70) {
                        swapBuffer();
                        return true;
                    }
                }

                return false;
            }

        });

        load();
        // if (prefs.getBoolean(PREF_MODIFIED, false))
        //     restoreStashedBuffer();
        //else
        //    loadFiles();
    }

    private void load() {
        String curBuf = prefs.getString(PREF_CUR_BUF, MEDS);

        Buffer meds = new Buffer(MEDS);
        Buffer notes = new Buffer(NOTES);

        if (prefs.getString(PREF_CUR_BUF, MEDS).equals(MEDS)) {
            cur = meds;
            oth = notes;
        } else {
            cur = notes;
            oth = meds;
        }

        loadBuffer(cur);
    }

    private void swapBuffer() {
        // //cur.sync();
        // syncToBuffer(cur);
        // Buffer tmp = new Buffer(cur);
        // cur = oth;
        // oth = tmp;

        // cur.makeActive();

        //syncToBuffer(cur); // Grab state from editText and sv
        syncToCur();
        cur.stash();

        // We're just swapping references, not making new objects or copying data.
        Buffer tmp = cur;
        cur = oth;
        oth = tmp;

        loadBuffer(cur); // Update state from new cur
    }

    // private void swapBuffer() {
    //     int tCurPos, tScroll;
    //     boolean tModified;
    //     String tText, tBufName;

    //     // TODO: Have a data structure (inner class?) for this?
    //     tCurPos = editText.getSelectionStart();
    //     tScroll = sv.getScrollY();
    //     tModified = modified;
    //     tText = editText.getText().toString();
    //     tBufName = bufName;

    //     editText.setText(oText);
    //     editText.setSelection(oCurPos);
    //     sv.scrollTo(0, oScroll);
    //     bufName = oBufName;
    //     modified = oModified;

    //     oCurPos = tCurPos;
    //     oScroll = tScroll;
    //     oModified = tModified;
    //     oText = tText;
    //     oBufName = tBufName;

    //     setHeader();
    // }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) return;
        if (loaded) return;

        //loadBuffer(cur);
        // try {
        //     editText.setSelection(prefs.getInt(PREF_CUR_POS, 0));
        // } catch (Exception e) {}
        // sv.scrollTo(0, prefs.getInt(PREF_SCROLL_Y, 0));

        try {editText.setSelection(cur.curPos);} catch (Exception e) {}
        sv.scrollTo(0, cur.scroll);

        loaded = true;
    }

    @Override
    public void onPause() {
        super.onPause();

        //syncToBuffer(cur);
        syncToCur();
        cur.stash();

        editor.putString(PREF_CUR_BUF, cur.bufName);
        editor.commit();

        // editor.putBoolean(PREF_MODIFIED, modified);
        // editor.putInt(PREF_CUR_POS, editText.getSelectionStart());
        // editor.putInt(PREF_SCROLL_Y, sv.getScrollY());

        // editor.apply();

        // if (modified)
        //     stashBuffer();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void hClick(android.view.View v) {
        //if (modified) saveFile();
        cur.save();
    }

    public void menuClick(android.view.View v) {
        //PopupMenu popup = new PopupMenu(this, v);
        //PopupMenu popup = new PopupMenu(this, findViewById(R.id.menu), android.view.Gravity.TOP|android.view.Gravity.RIGHT);
        // PopupMenu popup = new PopupMenu(this, findViewById(R.id.header_bar));
        // popup.setGravity(android.view.Gravity.BOTTOM|android.view.Gravity.RIGHT);
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.header_bar), Gravity.BOTTOM|Gravity.RIGHT);
        popup.inflate(R.menu.menu);
        popup.show();
    }

    // private void saveFile() {
    //     //saveFile(bufName);
    //     //setModified(false);
    // }

    // private void loadFiles() {
    //     // Load secondary first, so we're done after swapping and loading primary (TODO: keep track of which is open)
    //     loadFile(NOTES);
    //     swapBuffer();
    //     loadFile(MEDS);
    //     setHeader();
    // }

    // private void stashBuffer() {
    //     saveFile(bufName + "~");
    // }

    // private void restoreStashedBuffer() {
    //     if (loadFile(STASH)) {
    //         removeStashFile();
    //         setModified(true);
    //     } else {
    //         loadFile();
    //     }
    // }

    // private void removeStashFile() {
    //     File f = new File(getFilesDir(), STASH);
    //     f.delete();
    // }

    // private void saveFile(String fname) {
    //     try {
    //         FileOutputStream fos = openFileOutput(fname, 0);
    //         fos.write(editText.getText().toString().getBytes());
    //     } catch (FileNotFoundException e) {
    //         Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
    //     } catch (Exception e) {
    //         Toast.makeText(this, "Error writing file!", Toast.LENGTH_SHORT).show();
    //     }
    // }

    // private boolean loadFile(String fname) {
    //     try {
    //         FileInputStream fis = openFileInput(fname);
    //         ByteArrayOutputStream result = new ByteArrayOutputStream();
    //         byte[] buf = new byte[1024];
    //         int len;

    //         while ((len = fis.read(buf)) != -1) {
    //             result.write(buf, 0, len);
    //         }
    //         String s = result.toString("UTF-8");
    //         editText.setText(s);
    //         editText.setSelection(s.length());
    //     } catch (FileNotFoundException e) {
    //         //Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
    //         return false;
    //     } catch (Exception e) {
    //         Toast.makeText(this, "Error reading file!", Toast.LENGTH_SHORT).show();
    //         return false;
    //     } finally {
    //         editText.requestFocus();
    //         bufName = fname;
    //         modified = false;
    //     }

    //     return true;
    // }
}
