import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;

import static java.lang.Math.ceilDiv;

public class DataHelper {

    public static long[] concatLongs(long[] a, long[] b) {
        long[] c = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static byte[] addPaddingWord(byte[] data) {
        if (data.length % GlobalErrorCorrecter.WORD_LEN == 0) return data;
        byte[] result = new byte[ceilDiv(data.length, GlobalErrorCorrecter.WORD_LEN) * GlobalErrorCorrecter.WORD_LEN];
        System.arraycopy(data, 0, result, 0, data.length);
        return result;
    }

    public static byte[] removePaddingWord(byte[] data, int size) {
        byte[] result = new byte[size];
        System.arraycopy(data, 0, result, 0, size);
        return result;
    }

    public static byte[] stringToBytes(String s) throws UnsupportedEncodingException {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] b) throws UnsupportedEncodingException {
        return new String(b, StandardCharsets.UTF_8);
    }

    public static String getFileName(String path) {
        return Paths.get(path).getFileName().toString();
    }

    public static byte[] readFileBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    public static void writeFileBytes(String path, byte[] data) throws IOException {
        Files.write(Paths.get(path), data);
    }

    public static BitSet bytesToBitSet(byte[] data) {
        return BitSet.valueOf(data);
    }

    public static byte[] bitSetToBytes(BitSet bitSet, int length) {
        return Arrays.copyOf(bitSet.toByteArray(), length);
    }

    public static long bytesToLong(byte[] data, int start, int length) {
        long result = 0;
        for (int i = start; i < start + length; i++) result = (result << 8) | (data[i] & 0xff);
        return result;
    }

    public static long[] bytesToSymbols(byte[] data) throws Exception {
        if (data.length % GlobalErrorCorrecter.WORD_LEN != 0) throw new Exception("Invalid data size!");
        long[] result = new long[data.length / GlobalErrorCorrecter.WORD_LEN];
        for (int i = 0; i < data.length; i += GlobalErrorCorrecter.WORD_LEN)
            result[i / GlobalErrorCorrecter.WORD_LEN] = bytesToLong(data, i, GlobalErrorCorrecter.WORD_LEN);
        return result;
    }

    // note: don't use longToBytes with number < 0
    public static byte[] longToBytes(long value, int n) {
        byte[] result = new byte[n];
        for (int i = n - 1; i >= 0; i--) {
            result[i] = (byte) (value & 0xff);
            value >>= 8;
        }
        return result;
    }

    public static byte[] symbolsToBytes(long[] data) {
        byte[] result = new byte[data.length * GlobalErrorCorrecter.WORD_LEN];
        for (int i = 0; i < data.length; i++) {
            byte[] tmp = longToBytes(data[i], GlobalErrorCorrecter.WORD_LEN);
            System.arraycopy(tmp, 0, result, i * GlobalErrorCorrecter.WORD_LEN, tmp.length);
        }
        return result;
    }


    // UNIT TEST
//    public static void main(String[] args) throws Exception {
//
////        byte[] a = {1, 2, 55, 127, -128, 0, 5, 8, 77, 22, 1};
//////        byte[] a = {-128, -128, -128, 127, 127, 0};
////        long[] b = null;
////        try {
////            b = bytesToSymbols(addPaddingWord(a));
////            System.out.println(Arrays.toString(b));
////            System.out.println(Arrays.toString(removePaddingWord(symbolsToBytes(b), a.length)));
////        } catch (Exception e) {
////            System.out.println(Arrays.toString(addPaddingWord(a)));
////            throw new RuntimeException(e);
////        }
//
////        byte[] a = {-128, -128, -128, 127, 127, 0};
////        byte[] b = symbolsToBytes(bytesToSymbols(a));
////        System.out.println(Arrays.toString(b));
//
////        byte[] a = {-127, -127, -127, -127, -127, -127, -127, -127};
////        System.out.println(bytesToLong(a, 0, a.length));
//
//
//    }

}
