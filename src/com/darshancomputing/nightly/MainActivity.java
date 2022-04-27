/*
    Copyright (c) 2010-2022 Darshan Computing, LLC
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
import android.widget.PopupMenu.OnMenuItemClickListener;
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


public class MainActivity extends Activity implements OnMenuItemClickListener {
    private static final String PREFS = "settings";
    private static final String MEDS = "Meds";
    private static final String NOTES = "Notes";

    private static final String PREF_X_CUR_POS = "_cur_pos";
    private static final String PREF_X_SCROLL = "_scroll";
    private static final String PREF_X_MODIFIED = "_modified";
    private static final String PREF_X_SAVED_TEXT = "_saved_text";
    private static final String PREF_X_TEXT = "_text";
    private static final String PREF_CUR_BUF = "cur_buf";

    private static final int EXPORT_FILE = 1;
    private static final int IMPORT_FILE = 2;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private TextWatcher textWatcher;
    private TextView header;
    private ScrollView sv;
    private PopupMenu popup;
    private Menu puMenu;
    private boolean modified;
    private String bufName;
    private boolean loaded;
    //private boolean loadingBuffer;

    private class Buffer {
        private int curPos, scroll;
        private boolean modified;
        private String text, bufName;
        private EditText editText;

        private Buffer(String name, int resId) {
            bufName = name;

            editText = (EditText) findViewById(resId);
            //editText.addTextChangedListener(textWatcher);

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

        // Doesn't commit, as the expectation is to be used when putting other things as well.
        private void stash() {
            curPos = editText.getSelectionStart();
            text = editText.getText().toString();

            putState();

            if (modified)
                editor.putString(bufName + PREF_X_TEXT, text);
        }

        private void putState() {
            editor.putInt(bufName + PREF_X_CUR_POS, curPos);
            editor.putInt(bufName + PREF_X_SCROLL, scroll);
            editor.putBoolean(bufName + PREF_X_MODIFIED, modified);
        }

        private void setSelection() {
            try {
                editText.setSelection(curPos);
            } catch (Exception e) {
                editText.setSelection(text.length());
            }
        }

        private void revertBuffer() {
            if (!modified) return;

            curPos = editText.getSelectionStart();
            scroll = sv.getScrollY();

            long start = System.currentTimeMillis();
            String stext = prefs.getString(bufName + PREF_X_SAVED_TEXT, "");
            long now = System.currentTimeMillis();
            System.out.println("..................... nightly: getting stext " + (now - start) + " ms");
            //loadingBuffer = true;
            editText.removeTextChangedListener(textWatcher);
            start = System.currentTimeMillis();
            editText.setText(stext);
            now = System.currentTimeMillis();
            //editText.setText(cstext);
            //editText.setText(edText);
            editText.addTextChangedListener(textWatcher);
            //loadingBuffer = false;
            // long now = System.currentTimeMillis();
            System.out.println("..................... nightly: reverting took " + (now - start) + " ms");

            modified = false;

            setSelection();
            setHeader();

            editText.requestFocus();
            sv.scrollTo(0, cur.scroll);
        }

        private void load() {
            long start = System.currentTimeMillis();
            //loadingBuffer = true;
            editText.removeTextChangedListener(textWatcher);
            editText.setText(text);
            editText.addTextChangedListener(textWatcher);
            //loadingBuffer = false;
            long now = System.currentTimeMillis();
            System.out.println("..................... nightly: loading took " + (now - start) + " ms");

            setSelection();
        }
    }

    private Buffer cur, oth;

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

        updateMenuItems(); // Lazy, easy approach for now...
    }

    private void updateMenuItems() {
        if (puMenu == null) return;

        puMenu.findItem(R.id.revert).setEnabled(cur.modified);

        // If buffer is modified, does user want to export modified buffer, or saved version?
        // Rather than picking a default or asking the user, I think it's cleanest and simplest to say:
        //   exporting is only available when the buffer is saved.  Feels right and reasonable to me,
        //   at least for right now.
        puMenu.findItem(R.id.export).setEnabled(!cur.modified);

        // Similarly, rather than prompt user if they're sure, just have them indicate sureness by clearing
        //   (select all following by backspace, for example) and saving buffer.  Then clearly safe to load file.
        puMenu.findItem(R.id.m_import).setEnabled(!cur.modified && cur.editText.getText().toString().length() == 0);
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

        header = (TextView) findViewById(R.id.header);

        textWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                setModified(true);
                //if (!loadingBuffer) setModified(true);
            }
            @Override
            public void beforeTextChanged(java.lang.CharSequence s, int start, int count, int after){}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        };

        sv = findViewById(R.id.sv);
        sv.setSmoothScrollingEnabled(false);

        header.setOnTouchListener(new View.OnTouchListener() {
            private float downX;

            @Override
            public boolean onTouch(View view, MotionEvent e) {
                int a = e.getActionMasked();

                if (a == MotionEvent.ACTION_DOWN) {
                    downX = e.getX();
                    return false;
                }

                if (a == MotionEvent.ACTION_UP) {
                    float hDelta = e.getX() - downX;

                    if (hDelta > 70 || hDelta < -70) {
                        swapBuffer();
                        return true;
                    }
                }

                return false;
            }

        });

        tstart = System.currentTimeMillis();
        load();
    }
    long tstart;

    private void load() {
        long start = System.currentTimeMillis();
        String curBuf = prefs.getString(PREF_CUR_BUF, MEDS);

        Buffer meds = new Buffer(MEDS, R.id.et1);
        Buffer notes = new Buffer(NOTES, R.id.et2);

        if (prefs.getString(PREF_CUR_BUF, MEDS).equals(MEDS)) {
            cur = meds;
            oth = notes;
        } else {
            cur = notes;
            oth = meds;
        }

        oth.load();
        cur.load();

        cur.editText.setVisibility(View.VISIBLE);

        cur.editText.requestFocus();
        sv.scrollTo(0, cur.scroll);
        setHeader();
        long now = System.currentTimeMillis();
        System.out.println("..................... nightly: load() took " + (now - start) + " ms");

    }

    private void swapBuffer() {
        cur.scroll = sv.getScrollY();
        cur.editText.setVisibility(View.GONE);

        // Note: we're just swapping references, not making new objects or copying data, so this is safe and simple.
        Buffer tmp = cur;
        cur = oth;
        oth = tmp;

        cur.editText.setVisibility(View.VISIBLE);
        cur.editText.requestFocus();
        sv.scrollTo(0, cur.scroll);
        setHeader();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) return;
        if (loaded) return;

        cur.setSelection();
        sv.scrollTo(0, cur.scroll);

        loaded = true;
        long now = System.currentTimeMillis();
        System.out.println("..................... nightly: load start to oWFC finish took " + (now - tstart) + " ms");
    }

    @Override
    public void onPause() {
        super.onPause();

        cur.scroll = sv.getScrollY();
        cur.stash();
        oth.stash();

        editor.putString(PREF_CUR_BUF, cur.bufName);
        editor.commit();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void hClick(android.view.View v) {
        cur.save();
    }

    public void menuClick(android.view.View v) {
        if (popup == null) {
            popup = new PopupMenu(this, findViewById(R.id.header_bar), Gravity.BOTTOM|Gravity.RIGHT);
            popup.setOnMenuItemClickListener(this);
            popup.inflate(R.menu.menu);
            puMenu = popup.getMenu();

            updateMenuItems();
        }

        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
        case R.id.new_section_top:
            cur.editText.getText().insert(0, "\n\n\n\n");
            cur.editText.setSelection(0);
            sv.scrollTo(0, 0);
            return true;
        case R.id.revert:
            cur.revertBuffer();
            return true;
        case R.id.top:
            cur.editText.setSelection(0);
            sv.scrollTo(0, 0);
            return true;
        case R.id.bottom:
            cur.editText.setSelection(cur.editText.getText().length());
            sv.scrollTo(0, Integer.MAX_VALUE / 2);
            return true;
        case R.id.export:
            intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, cur.bufName + ".txt");
            intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("file:///mnt/sdcard/"));

            startActivityForResult(intent, EXPORT_FILE);
            return true;
        case R.id.m_import:
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("file:///mnt/sdcard/"));

            startActivityForResult(intent, IMPORT_FILE);
            return true;
        default:
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            // User cancelled / backed out, rather than selecting a location.
            // I think doing nothing is the most user-friendly / expected thing.
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, "Non-okay resultCode!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == EXPORT_FILE) {
            try {
                Uri uri = data.getData();
                OutputStream os = getContentResolver().openOutputStream(uri);

                os.write(cur.editText.getText().toString().getBytes());
                os.flush();
                os.close();
            } catch (Exception e) {
                Toast.makeText(this, "Error reading file!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == IMPORT_FILE) {
            try {
                Uri uri = data.getData();
                InputStream is = getContentResolver().openInputStream(uri);

                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;

                while ((len = is.read(buf)) != -1) {
                    result.write(buf, 0, len);
                }

                String s = result.toString("UTF-8");
                cur.text = s;
                cur.load();
                // I like it this way, so user can save or revert, rather than saving immediately here
                setModified(true);

                is.close();
            } catch (Exception e) {
                Toast.makeText(this, "Error writing file!", Toast.LENGTH_SHORT).show();
            }
        }
    }

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
