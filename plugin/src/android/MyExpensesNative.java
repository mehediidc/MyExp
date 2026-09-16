package com.mhdigital.myexpenses.nativeplugin;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Fixed version of MyExpensesNative
 * - Keeps strong reference to the temporary print WebView so it is not GC'd
 *   before onPageFinished (this was the main reason printing failed)
 * - Cleans up the WebView after printing
 * - Better error handling for PrintManager
 */
public class MyExpensesNative extends CordovaPlugin {
    private static final int REQ_STORAGE = 7211;
    private CallbackContext pendingBackup;
    private String pendingFilename;
    private String pendingJson;

    // Strong reference so the WebView is not garbage-collected before page finishes loading
    private WebView printWebView;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if ("printHtml".equals(action)) {
            final String title = args.optString(0, "MyExpenses");
            final String html = args.optString(1, "");
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printHtmlInternal(title, html, callbackContext);
                }
            });
            return true;
        }
        if ("saveBackup".equals(action)) {
            this.pendingFilename = args.optString(0, "MyExpenses_Backup.json");
            this.pendingJson = args.optString(1, "");
            this.pendingBackup = callbackContext;
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    saveBackupInternal();
                }
            });
            return true;
        }
        return false;
    }

    private void printHtmlInternal(final String title, String html, final CallbackContext cb) {
        final Activity activity = this.cordova.getActivity();

        // Destroy previous print WebView if any
        if (printWebView != null) {
            try {
                printWebView.destroy();
            } catch (Exception ignored) {}
            printWebView = null;
        }

        printWebView = new WebView(activity);
        printWebView.setBackgroundColor(0xFFFFFFFF);
        printWebView.getSettings().setJavaScriptEnabled(false);
        printWebView.getSettings().setLoadWithOverviewMode(true);
        printWebView.getSettings().setUseWideViewPort(true);

        printWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                try {
                    PrintManager pm = (PrintManager) activity.getSystemService(Activity.PRINT_SERVICE);
                    if (pm == null) {
                        cb.error("Print service not available");
                        cleanupPrintWebView();
                        return;
                    }
                    PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(title);
                    PrintAttributes attrs = new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();
                    pm.print(title, adapter, attrs);
                    cb.success("Print dialog opened");
                } catch (Exception e) {
                    cb.error("Printing failed: " + e.getMessage());
                } finally {
                    // Give the print system a moment, then clean up
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            cleanupPrintWebView();
                        }
                    }, 2000);
                }
            }
        });

        String doc = "<!doctype html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "body{font-family:Arial,sans-serif;padding:18px;color:#111;background:#fff}"
                + "h2{text-align:center;margin:0 0 16px}"
                + ".summary-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-bottom:14px}"
                + ".summary{border:1px solid #aaa;padding:10px}"
                + ".summary span{display:block;font-size:11px}"
                + ".summary strong{display:block;font-size:16px;margin-top:4px}"
                + ".report-line{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #ddd}"
                + ".report-name{font-weight:700}"
                + ".report-values{text-align:right}"
                + ".ledger-table{width:100%;border-collapse:collapse;font-size:12px}"
                + ".ledger-table th,.ledger-table td{border:1px solid #999;padding:7px 6px}"
                + ".ledger-table th{background:#eee}"
                + ".ledger-table td.num{text-align:right}"
                + ".ledger-opening{font-weight:700;background:#f5f5f5}"
                + ".print-meta{font-size:11px;color:#555;margin-bottom:10px}"
                + "@page{size:A4;margin:10mm}"
                + "</style></head><body><h2>" + escapeHtml(title) + "</h2>" + html + "</body></html>";

        printWebView.loadDataWithBaseURL(null, doc, "text/html", "UTF-8", null);
    }

    private void cleanupPrintWebView() {
        if (printWebView != null) {
            try {
                printWebView.stopLoading();
                printWebView.setWebViewClient(null);
                printWebView.destroy();
            } catch (Exception ignored) {}
            printWebView = null;
        }
    }

    private void saveBackupInternal() {
        if (this.pendingBackup == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT <= 28
                    && this.cordova.getActivity().checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
                this.cordova.getActivity().requestPermissions(
                        new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, REQ_STORAGE);
            } else {
                writeBackup();
            }
        } catch (Exception e) {
            this.pendingBackup.error("Backup failed: " + e.getMessage());
            this.pendingBackup = null;
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (requestCode == REQ_STORAGE && this.pendingBackup != null) {
            if (grantResults.length <= 0 || grantResults[0] != 0) {
                this.pendingBackup.error("Storage permission denied");
                this.pendingBackup = null;
            } else {
                writeBackup();
            }
        }
    }

    private void writeBackup() {
        OutputStream out = null;
        String location;
        try {
            String safe = this.pendingFilename.replaceAll("[^A-Za-z0-9._-]", "_");
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, safe);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/MyExpenses");
                Uri uri = this.cordova.getActivity().getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    throw new Exception("Could not create Downloads file");
                }
                out = this.cordova.getActivity().getContentResolver().openOutputStream(uri);
                location = "Downloads/MyExpenses/" + safe;
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "MyExpenses");
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new Exception("Could not create Downloads folder");
                }
                File file = new File(dir, safe);
                out = new FileOutputStream(file);
                location = file.getAbsolutePath();
            }
            if (out == null) {
                throw new Exception("Could not open backup file");
            }
            out.write(this.pendingJson.getBytes("UTF-8"));
            out.close();
            out = null;

            final String msg = "Backup saved: " + location;
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MyExpensesNative.this.cordova.getActivity(), msg, Toast.LENGTH_LONG).show();
                }
            });
            this.pendingBackup.success(location);
            this.pendingBackup = null;
        } catch (Exception e) {
            if (this.pendingBackup != null) {
                this.pendingBackup.error("Backup failed: " + e.getMessage());
            }
            this.pendingBackup = null;
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception ignored) {}
            }
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
