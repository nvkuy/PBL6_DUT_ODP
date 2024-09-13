import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public static long[] bytesToLongArray(byte[] data) {
        long[] result = new long[(data.length + 3) / 4];
        for (int i = 0; i < data.length; i += 4) {
            long tmp = 0;
            for (int j = 0; j < 4 && i + j < data.length; j++) {
                tmp = (tmp << 8) | (data[i + j] & 0xFF);
            }
            result[i / 4] = tmp;
        }
        return result;
    }

    public static byte[] longArrayToBytes(long[] data, int n) {
        byte[] result = new byte[n];
        for (int i = 0; i < data.length; i++) {
            long tmp = data[i];
            for (int j = min(3, n - i * 4 - 1); j >= 0; j--) {
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
//        long[] b = bytesToLongArray(a);
//        System.out.println(Arrays.toString(b));
//        System.out.println(Arrays.toString(longArrayToBytes(b, a.length)));
//
//    }

}
