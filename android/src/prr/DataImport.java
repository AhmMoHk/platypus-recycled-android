package prr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * First-launch installer for the converted game data. The data pack (made from the user's own copy with
 * tools/export/convert_assets.py) comes as a few zip parts, PlatypusRecycled-data-part1of5.zip ... part5of5.zip.
 * The user picks them with the system file picker (no storage permission); the app streams each zip and
 * extracts it into its internal storage (getFilesDir()/data), so no file manager ever has to copy thousands of
 * loose files into Android/data. Parts can be picked all at once or in several rounds.
 */
final class DataImport {
    private DataImport() {}
    static final int REQ = 4711;
    private static final Pattern PART = Pattern.compile("part(\\d+)of(\\d+)", Pattern.CASE_INSENSITIVE);

    static boolean isReady(AndroidHost host) {
        return new File(host.data, ".installed").exists() && new File(host.data, "unity/meta.json").exists();
    }

    static void prompt(final Activity a, final AndroidHost host) {
        TreeSet<Integer> done = new TreeSet<>();
        int n = readParts(host, done);
        String have = n > 0 ? "\n\nInstalled so far: " + done.size() + " of " + n + " parts. Missing: " + missing(done, n) + "." : "";
        new AlertDialog.Builder(a)
            .setTitle("Install game data")
            .setMessage("Select the data parts (PlatypusRecycled-data-part1of5.zip ... part5of5.zip, usually in Download). "
                + "You can select all of them at once — they are unpacked inside the app." + have)
            .setCancelable(false)
            .setPositiveButton("Select parts", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream"});
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    a.startActivityForResult(i, REQ);
                }
            })
            .setNegativeButton("Quit", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) { a.finish(); }
            })
            .show();
    }

    static void onResult(final Activity a, final AndroidHost host, Intent data, final Runnable done) {
        final ArrayList<Uri> uris = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) for (int i = 0; i < clip.getItemCount(); i++) uris.add(clip.getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        if (uris.isEmpty()) { prompt(a, host); return; }
        final ProgressDialog p = new ProgressDialog(a);
        p.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        p.setTitle("Installing game data");
        p.setMessage("Preparing…");
        p.setCancelable(false);
        p.setMax(1000);
        p.show();
        new Thread(new Runnable() {
            public void run() {
                final String err = install(a, host, uris, p);
                a.runOnUiThread(new Runnable() {
                    public void run() {
                        p.dismiss();
                        if (err != null) {
                            new AlertDialog.Builder(a).setTitle("Install failed").setMessage(err).setCancelable(false)
                                .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                                    public void onClick(android.content.DialogInterface d, int w) { prompt(a, host); }
                                }).show();
                        } else if (!isReady(host)) {
                            prompt(a, host);     // more parts to go
                        } else {
                            cleanLegacy(a);
                            done.run();
                        }
                    }
                });
            }
        }, "prr-install").start();
    }

    // ------------------------------------------------------------------ extraction
    private static String install(final Activity a, AndroidHost host, ArrayList<Uri> uris, final ProgressDialog p) {
        try {
            File root = host.data.getParentFile();          // zip entries are "data/..."
            host.data.mkdirs();
            String[] names = new String[uris.size()];
            long[] sizes = new long[uris.size()];
            long total = 0;
            for (int i = 0; i < uris.size(); i++) {
                names[i] = "part";
                sizes[i] = -1;
                Cursor c = a.getContentResolver().query(uris.get(i), new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
                if (c != null) {
                    if (c.moveToFirst()) {
                        if (!c.isNull(0)) names[i] = c.getString(0);
                        if (!c.isNull(1)) sizes[i] = c.getLong(1);
                    }
                    c.close();
                }
                total += Math.max(0, sizes[i]);
            }
            TreeSet<Integer> done = new TreeSet<>();
            int n = readParts(host, done);
            final long[] read = {0};
            final long tot = Math.max(1, total);
            byte[] buf = new byte[1 << 20];
            for (int i = 0; i < uris.size(); i++) {
                Matcher m = PART.matcher(names[i]);
                int part = 1, of = 1;
                if (m.find()) { part = Integer.parseInt(m.group(1)); of = Integer.parseInt(m.group(2)); }
                if (n > 0 && of != n) return names[i] + " belongs to a different data pack (" + of + " parts instead of " + n + ").";
                n = of;
                final String label = "Part " + part + " of " + of + " — " + names[i];
                a.runOnUiThread(new Runnable() { public void run() { p.setMessage(label); } });
                InputStream raw = a.getContentResolver().openInputStream(uris.get(i));
                if (raw == null) return "Cannot open " + names[i];
                final long base = read[0];
                InputStream counted = new FilterInputStream(new BufferedInputStream(raw, 1 << 20)) {
                    long last;
                    @Override public int read(byte[] b, int off, int len) throws IOException {
                        int r = super.read(b, off, len);
                        if (r > 0) { read[0] += r; tick(); }
                        return r;
                    }
                    @Override public int read() throws IOException {
                        int r = super.read();
                        if (r >= 0) { read[0]++; tick(); }
                        return r;
                    }
                    void tick() {
                        if (read[0] - last < (4 << 20)) return;
                        last = read[0];
                        final int v = (int) (read[0] * 1000 / tot);
                        a.runOnUiThread(new Runnable() { public void run() { p.setProgress(v); } });
                    }
                };
                int files = 0;
                ZipInputStream z = new ZipInputStream(counted);
                try {
                    ZipEntry e;
                    String rootPath = root.getCanonicalPath() + File.separator;
                    while ((e = z.getNextEntry()) != null) {
                        String name = e.getName().replace('\\', '/');
                        if (!name.startsWith("data/")) name = "data/" + name;   // packs zipped without the top folder
                        File out = new File(root, name);
                        if (!out.getCanonicalPath().startsWith(rootPath)) continue;   // no ../ escapes
                        if (e.isDirectory()) { out.mkdirs(); continue; }
                        out.getParentFile().mkdirs();
                        FileOutputStream o = new FileOutputStream(out);
                        try {
                            int r;
                            while ((r = z.read(buf)) > 0) o.write(buf, 0, r);
                        } finally {
                            o.close();
                        }
                        files++;
                    }
                } finally {
                    z.close();
                }
                if (files == 0) return names[i] + " is not a data pack zip.";
                if (sizes[i] < 0) read[0] = base;    // unknown size: progress stays per part
                done.add(part);
                writeParts(host, done, n);
            }
            if (done.size() >= n && new File(host.data, "unity/meta.json").exists()) {
                new FileOutputStream(new File(host.data, ".installed")).close();
            }
            return null;
        } catch (Exception e) {
            return "Error: " + e;
        }
    }

    private static int readParts(AndroidHost host, TreeSet<Integer> done) {
        File f = new File(host.data, ".parts");
        if (!f.exists()) return 0;
        try {
            byte[] b = new byte[(int) f.length()];
            FileInputStream in = new FileInputStream(f);
            int k = 0, r;
            while (k < b.length && (r = in.read(b, k, b.length - k)) > 0) k += r;
            in.close();
            String[] s = new String(b, "UTF-8").trim().split("\\s+");
            int n = Integer.parseInt(s[0]);
            for (int i = 1; i < s.length; i++) done.add(Integer.parseInt(s[i]));
            return n;
        } catch (Exception e) {
            return 0;
        }
    }

    private static void writeParts(AndroidHost host, TreeSet<Integer> done, int n) throws IOException {
        StringBuilder sb = new StringBuilder().append(n);
        for (int d : done) sb.append(' ').append(d);
        FileOutputStream o = new FileOutputStream(new File(host.data, ".parts"));
        o.write(sb.toString().getBytes("UTF-8"));
        o.close();
    }

    private static String missing(TreeSet<Integer> done, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= n; i++) if (!done.contains(i)) sb.append(sb.length() > 0 ? ", " : "").append(i);
        return sb.length() == 0 ? "none" : sb.toString();
    }

    /** an older, partial copy in Android/data/<pkg>/files/data is no longer used: free its space */
    private static void cleanLegacy(final Activity a) {
        final File old = new File(a.getExternalFilesDir(null), "data");
        if (!old.exists()) return;
        new Thread(new Runnable() { public void run() { delete(old); } }, "prr-clean").start();
    }

    private static void delete(File f) {
        File[] l = f.listFiles();
        if (l != null) for (File c : l) delete(c);
        f.delete();
    }
}
