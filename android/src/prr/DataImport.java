package prr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * One-time import of the converted game data. The data pack is produced on a PC by tools/convert_assets.py
 * from the user's own copy of the game; here it is copied into the app's external files directory with the
 * system folder picker (no storage permission needed).
 */
final class DataImport {
    private DataImport() {}
    static final int REQ = 4711;

    static boolean isReady(AndroidHost host) {
        return new File(host.data, "unity/meta.json").exists();
    }

    static void prompt(final Activity a, final AndroidHost host) {
        new AlertDialog.Builder(a)
            .setTitle("Game data")
            .setMessage("Select the converted data folder (made on your PC from your own copy of the game with tools/convert_assets.py).")
            .setCancelable(false)
            .setPositiveButton("Choose folder", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) {
                    if (Build.VERSION.SDK_INT >= 21) {
                        a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ);
                    } else {
                        new AlertDialog.Builder(a).setMessage("Copy the data folder to:\n" + host.data.getAbsolutePath()).show();
                    }
                }
            })
            .setNegativeButton("Quit", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) { a.finish(); }
            })
            .show();
    }

    static void onResult(final Activity a, final AndroidHost host, final Uri tree, final Runnable done) {
        final ProgressDialog p = new ProgressDialog(a);
        p.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        p.setTitle("Importing game data");
        p.setCancelable(false);
        p.setMax(100);
        p.show();
        new Thread(new Runnable() {
            public void run() {
                final String err = copyTree(a, host, tree, p);
                a.runOnUiThread(new Runnable() {
                    public void run() {
                        p.dismiss();
                        if (err != null) {
                            new AlertDialog.Builder(a).setTitle("Import failed").setMessage(err).show();
                        } else {
                            done.run();
                        }
                    }
                });
            }
        }, "prr-import").start();
    }

    private static String copyTree(Activity a, AndroidHost host, Uri tree, ProgressDialog p) {
        try {
            if (Build.VERSION.SDK_INT < 21) return "Android 5.0 or newer is required for the folder picker.";
            Uri root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
            long[] counted = {0, 0};
            count(a, tree, root, counted);
            if (counted[0] == 0) return "The selected folder is empty.";
            host.data.mkdirs();
            copy(a, tree, root, host.data, p, counted, new long[]{0});
            if (!isReady(host)) return "unity/meta.json was not found in the selected folder — pick the folder that contains it.";
            return null;
        } catch (Exception e) {
            return String.valueOf(e);
        }
    }

    private static void count(Activity a, Uri tree, Uri dir, long[] out) {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(dir));
        android.database.Cursor c = a.getContentResolver().query(children,
            new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE}, null, null, null);
        if (c == null) return;
        try {
            while (c.moveToNext()) {
                String id = c.getString(0), mime = c.getString(1);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) count(a, tree, DocumentsContract.buildDocumentUriUsingTree(tree, id), out);
                else { out[0]++; out[1] += c.getLong(2); }
            }
        } finally {
            c.close();
        }
    }

    private static void copy(Activity a, Uri tree, Uri dir, File dst, ProgressDialog p, long[] total, long[] done) throws Exception {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(dir));
        android.database.Cursor c = a.getContentResolver().query(children,
            new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null);
        if (c == null) return;
        try {
            while (c.moveToNext()) {
                String id = c.getString(0), mime = c.getString(1), name = c.getString(2);
                Uri u = DocumentsContract.buildDocumentUriUsingTree(tree, id);
                File target = new File(dst, name);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    target.mkdirs();
                    copy(a, tree, u, target, p, total, done);
                } else {
                    InputStream in = a.getContentResolver().openInputStream(u);
                    FileOutputStream out = new FileOutputStream(target);
                    try {
                        byte[] buf = new byte[262144];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    } finally {
                        out.close();
                        if (in != null) in.close();
                    }
                    done[0]++;
                    final int pct = (int) (done[0] * 100 / Math.max(1, total[0]));
                    p.setProgress(pct);
                }
            }
        } finally {
            c.close();
        }
    }
}
