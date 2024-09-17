import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;

import static java.lang.Math.min;

public class DataHelper {

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
        if (data.length % 4 != 0) throw new Exception("Invalid data size!");
        long[] result = new long[data.length / 4];
        for (int i = 0; i < data.length; i += 4) {
            long tmp = 0;
            for (int j = 0; j < 4; j++) {
                tmp = (tmp << 8) | (data[i + j] & 0xFF);
            }
            result[i / 4] = tmp;
        }
        return result;
    }

    public static byte[] longArrayToBytes(long[] data) {
        byte[] result = new byte[data.length * 4];
        for (int i = 0; i < data.length; i++) {
            long tmp = data[i];
            for (int j = 3; j >= 0; j--) {
                result[i * 4 + j] = (byte) (tmp & 0xFF);
                tmp >>= 8;
            }
        }

        return result;
    }

    // UNIT TEST
//    public static void main(String[] args) {
//
//        byte[] a = {1, 2, 55, 127, 77, 1, 5, 8};
//        long[] b = null;
//        try {
//            b = bytesToLongArray(a);
//            System.out.println(Arrays.toString(b));
//            System.out.println(Arrays.toString(longArrayToBytes(b)));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//    }

}
