// Platypus Reclayed Android port: reads the table of contents of an APK (a zip file) to learn how each file is
// stored. A file stored as is can be read straight out of the APK at any position (that is how the asset bundles are
// loaded); a compressed one has to be unpacked from its start for every read, which made loading take minutes.
// Used by the game at start (ModFiles) and by the build script to check the APK it made.
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace PlatypusAndroid
{
    public static class ApkZip
    {
        public struct Entry
        {
            public string name;
            public int method;            // 0 = stored, 8 = deflated
            public long size, packed;
            public bool Stored => method == 0;
        }

        /// <summary>All entries of the zip's central directory (empty on any error).</summary>
        public static Dictionary<string, Entry> Read(string path)
        {
            var result = new Dictionary<string, Entry>(StringComparer.Ordinal);
            try
            {
                using (var fs = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read))
                {
                    long len = fs.Length;
                    int tail = (int)Math.Min(len, 65557);
                    var buf = new byte[tail];
                    fs.Seek(len - tail, SeekOrigin.Begin);
                    ReadFully(fs, buf, tail);
                    int eocd = -1;
                    for (int i = tail - 22; i >= 0; i--) if (U32(buf, i) == 0x06054b50) { eocd = i; break; }
                    if (eocd < 0) return result;
                    long count = U16(buf, eocd + 10), cdSize = U32(buf, eocd + 12), cdOffset = U32(buf, eocd + 16);
                    if (cdOffset == 0xFFFFFFFF || count == 0xFFFF)                        // zip64
                    {
                        int loc = eocd - 20;
                        if (loc < 0 || U32(buf, loc) != 0x07064b50) return result;
                        long z64 = (long)U64(buf, loc + 8);
                        var z = new byte[56];
                        fs.Seek(z64, SeekOrigin.Begin);
                        ReadFully(fs, z, 56);
                        if (U32(z, 0) != 0x06064b50) return result;
                        count = (long)U64(z, 32); cdSize = (long)U64(z, 40); cdOffset = (long)U64(z, 48);
                    }
                    var cd = new byte[cdSize];
                    fs.Seek(cdOffset, SeekOrigin.Begin);
                    ReadFully(fs, cd, (int)cdSize);
                    int p = 0;
                    for (long n = 0; n < count && p + 46 <= cd.Length; n++)
                    {
                        if (U32(cd, p) != 0x02014b50) break;
                        int nameLen = U16(cd, p + 28), extraLen = U16(cd, p + 30), commentLen = U16(cd, p + 32);
                        var e = new Entry
                        {
                            method = U16(cd, p + 10),
                            packed = U32(cd, p + 20),
                            size = U32(cd, p + 24),
                            name = Encoding.UTF8.GetString(cd, p + 46, nameLen),
                        };
                        if (e.size == 0xFFFFFFFF || e.packed == 0xFFFFFFFF)               // zip64 sizes in the extra field
                        {
                            int x = p + 46 + nameLen, end = x + extraLen;
                            while (x + 4 <= end)
                            {
                                int id = U16(cd, x), sz = U16(cd, x + 2);
                                if (id == 1)
                                {
                                    int q = x + 4;
                                    if (e.size == 0xFFFFFFFF) { e.size = (long)U64(cd, q); q += 8; }
                                    if (e.packed == 0xFFFFFFFF) e.packed = (long)U64(cd, q);
                                    break;
                                }
                                x += 4 + sz;
                            }
                        }
                        result[e.name] = e;
                        p += 46 + nameLen + extraLen + commentLen;
                    }
                }
            }
            catch (Exception) { }
            return result;
        }

        static void ReadFully(Stream s, byte[] b, int n)
        {
            int got = 0;
            while (got < n) { int r = s.Read(b, got, n - got); if (r <= 0) throw new EndOfStreamException(); got += r; }
        }

        static int U16(byte[] b, int i) => b[i] | (b[i + 1] << 8);
        static long U32(byte[] b, int i) => (uint)(b[i] | (b[i + 1] << 8) | (b[i + 2] << 16) | (b[i + 3] << 24));
        static ulong U64(byte[] b, int i) => (ulong)U32(b, i) | ((ulong)U32(b, i + 4) << 32);
    }
}
