import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;

import static java.lang.Math.ceilDiv;
import static java.lang.Math.min;

public class DataHelper {

    public static long[] concatLongs(long[] a, long[] b) {
        long[] c = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static byte[] addPadding(byte[] data) {
        byte[] result = new byte[ceilDiv(data.length, GlobalErrorCorrecter.WORD_LEN) * GlobalErrorCorrecter.WORD_LEN];
        System.arraycopy(data, 0, result, 0, data.length);
        return result;
    }

    public static byte[] removePadding(byte[] data, int size) {
        byte[] result = new byte[size];
        System.arraycopy(data, 0, result, 0, size);
        return result;
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

    public static byte[] bitSetToBytes(BitSet bitSet) {
        return bitSet.toByteArray();
    }

    public static long[] bytesToLongArray(byte[] data) throws Exception {
        if (data.length % GlobalErrorCorrecter.WORD_LEN != 0) throw new Exception("Invalid data size!");
        long[] result = new long[data.length / GlobalErrorCorrecter.WORD_LEN];
        for (int i = 0; i < data.length; i += GlobalErrorCorrecter.WORD_LEN) {
            long tmp = 0;
            for (int j = 0; j < GlobalErrorCorrecter.WORD_LEN; j++) {
                tmp = (tmp << 8) | (data[i + j] & 0xFF);
            }
            result[i / GlobalErrorCorrecter.WORD_LEN] = tmp;
        }
        return result;
    }

    public static byte[] longArrayToBytes(long[] data) {
        byte[] result = new byte[data.length * GlobalErrorCorrecter.WORD_LEN];
        for (int i = 0; i < data.length; i++) {
            long tmp = data[i];
            for (int j = GlobalErrorCorrecter.WORD_LEN - 1; j >= 0; j--) {
                result[i * GlobalErrorCorrecter.WORD_LEN + j] = (byte) (tmp & 0xFF);
                tmp >>= 8;
            }
        }

        return result;
    }



    // UNIT TEST
//    public static void main(String[] args) {
//
////        byte[] a = {1, 2, 55, 127, -128, 0, 5, 8};
//        byte[] a = {-128, -128, -128};
//        long[] b = null;
//        try {
//            b = bytesToLongArray(addPadding(a));
//            System.out.println(Arrays.toString(b));
//            System.out.println(Arrays.toString(removePadding(longArrayToBytes(b), a.length)));
//        } catch (Exception e) {
//            System.out.println(Arrays.toString(addPadding(a)));
//            throw new RuntimeException(e);
//        }
//
//    }

}
