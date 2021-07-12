package cc.ioctl.dextail;

import static cc.ioctl.dextail.HexUtils.bytesEqu;
import static cc.ioctl.dextail.HexUtils.readLe32;
import static cc.ioctl.dextail.HexUtils.subByteArray;
import static cc.ioctl.dextail.HexUtils.writeLe32;

import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.zip.Adler32;

public class DexTail {

    public static final int kSHA1DigestLen = 20;
    public static final int kMD5DigestLen = 16;

    public static byte[] createChunk(byte[] dat) {
        return createChunk(dat, new Random().nextInt() & 0xFF);
    }

    public static byte[] createChunk(byte[] dat, int x) {
        int size = 16 + dat.length;
        if (size % 4 != 0) {
            size = (size / 4 + 1) * 4;
        }
        byte[] buf = new byte[size];
        writeLe32(buf, 0, dat.length);
        writeLe32(buf, 4, 0);
        Adler32 adler32 = new Adler32();
        adler32.update(dat);
        writeLe32(buf, 8, (int) adler32.getValue());
        x &= 0xFF;
        if (x == 0) {
            writeLe32(buf, 12, 0);
            System.arraycopy(dat, 0, buf, 16, dat.length);
        } else {
            writeLe32(buf, 12, x);
            byte[] enc = new byte[dat.length];
            for (int i = 0; i < dat.length; i++) {
                enc[i] = (byte) (x ^ dat[i]);
            }
            System.arraycopy(enc, 0, buf, 16, dat.length);
        }
        return buf;
    }

    public static byte[] extractPayload(byte[] dex, PrintStream out) {
        int chunkROff = readLe32(dex, dex.length - 4);
        if (chunkROff > dex.length) {
            return null;
        }
        int base = dex.length - chunkROff;
        int size = readLe32(dex, base);
        if (size > dex.length) {
            return null;
        }
        int flags = readLe32(dex, base + 4);
        int a32_got = readLe32(dex, base + 8);
        int extra = readLe32(dex, base + 12);
        if (flags != 0) {
            out.printf("E Unexpected flags 0x%x\n", flags);
            return null;
        }
        int key = extra & 0xFF;
        out.printf("I Xor is 0x%x\n", key);
        byte[] dat = new byte[size];
        if (key == 0) {
            System.arraycopy(dex, base + 16, dat, 0, size);
        } else {
            for (int i = 0; i < size; i++) {
                dat[i] = (byte) (key ^ dex[base + 16 + i]);
            }
        }
        Adler32 adler32 = new Adler32();
        adler32.update(dat);
        int a32 = (int) adler32.getValue();
        if (a32 != a32_got) {
            out.printf("E Adler32 mismatch, got 0x08%x, expected 0x08%x\n", a32_got, a32);
            return null;
        }
        return dat;
    }

    public static byte[] injectPayload(byte[] origin, byte[] payload, boolean noXor,
        PrintStream out) {
        byte[] chunk = noXor ? createChunk(payload, 0) : createChunk(payload);
        byte[] result = new byte[origin.length + chunk.length + 4];
        System.arraycopy(origin, 0, result, 0, origin.length);
        System.arraycopy(chunk, 0, result, origin.length, chunk.length);
        writeLe32(result, origin.length + chunk.length, chunk.length + 4);
        writeLe32(result, 32, result.length);
        out.printf("I Dex size is 0x%x(%d)\n", result.length, result.length);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        md.update(result, 32, result.length - 32);
        byte[] signature = md.digest();
        if (signature.length != kSHA1DigestLen) {
            throw new RuntimeException("unexpected digest write: " + signature.length + " bytes");
        }
        System.arraycopy(signature, 0, result, 12, 20);
        out.println("I SHA1 sig: " + HexUtils.byteArrayToString(signature));
        Adler32 adler32 = new Adler32();
        adler32.update(result, 12, result.length - 12);
        int a32 = (int) adler32.getValue();
        writeLe32(result, 8, a32);
        out.printf("I Dex Adler32 checksum: %08x\n", a32);
        return result;
    }

    public static boolean checkDexSum(byte[] origin, PrintStream out) {
        if (origin.length < 52) {
            out.println("E Invalid dex file: truncated header");
            return false;
        }
        if (origin[0] != 'd' || origin[1] != 'e' || origin[2] != 'x') {
            out.println("E Invalid dex file: bad magic");
            return false;
        }
        if (origin[4] != '0' || origin[5] != '3' || (origin[6] != '5' && origin[6] != '7')) {
            out.println("E Unsupported dex version: " + new String(origin, 4, 3)
                + ", only dex035 is supported.");
            return false;
        }
        int a32_dex = readLe32(origin, 8);
        byte[] sha1sig_dex = subByteArray(origin, 12, 20);
        int size = readLe32(origin, 32);
        if (size != origin.length) {
            out.printf("E Corrupted dex file: expected %d bytes, %d got\n", size, origin.length);
            return false;
        }
        out.printf("I Dex size is 0x%x(%d)\n", size, size);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        md.update(origin, 32, origin.length - 32);
        byte[] signature = md.digest();
        if (signature.length != kSHA1DigestLen) {
            throw new RuntimeException("unexpected digest write: " + signature.length + " bytes");
        }
        Adler32 adler32 = new Adler32();
        adler32.update(origin, 12, origin.length - 12);
        int a32 = (int) adler32.getValue();
        boolean ret = true;
        if (a32 == a32_dex) {
            out.printf("I Dex Adler32 checksum: %08x\n", a32_dex);
        } else {
            out.printf("E Incorrect Adler32 checksum: %08x, expected %08x\n", a32_dex, a32);
            ret = false;
        }
        if (bytesEqu(sha1sig_dex, signature)) {
            out.println("I SHA1 sig: " + HexUtils.byteArrayToString(sha1sig_dex));
        } else {
            out.println(
                "E Incorrect SHA1 sig: " + HexUtils.byteArrayToString(sha1sig_dex) + ", expected "
                    + HexUtils.byteArrayToString(signature));
            ret = false;
        }
        return ret;
    }

}
