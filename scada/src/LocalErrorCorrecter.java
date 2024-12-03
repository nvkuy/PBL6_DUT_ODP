import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;


public class LocalErrorCorrecter {

    /*
     * using extended Hamming code to detect and correct 1-bit error in packet
     * https://en.wikipedia.org/wiki/Hamming_code
     * */

    public static final int ECC_BIT = 14;
    public static final int DECODE_SIZE = (1 << (ECC_BIT - 1));
    public static final int ENCODE_SIZE = ((DECODE_SIZE - ECC_BIT) >> 3) << 3;

    public static final int DECODE_BYTE_SIZE = DECODE_SIZE >> 3;
    public static final int ENCODE_BYTE_SIZE = ENCODE_SIZE >> 3;

    public static BitSet encode(BitSet data) throws Exception {
        if (data.length() > ENCODE_SIZE) throw new Exception("Invalid data size!");
        BitSet res = new BitSet(DECODE_SIZE);
        int tmp1 = 1, j = 0;
        for (int i = 1; i < DECODE_SIZE && j < data.length(); i++) {
            if (i == tmp1) tmp1 <<= 1;
            else res.set(i, data.get(j++));
        }
        boolean tmp2;
        for (int i = 1; i < DECODE_SIZE; i <<= 1) {
            tmp2 = false;
            for (j = 1; j < DECODE_SIZE; j++) {
                if ((i & j) != 0) tmp2 ^= res.get(j);
            }
            res.set(i, tmp2);
        }
        tmp2 = false;
        for (int i = 1; i < DECODE_SIZE; i++)
            tmp2 ^= res.get(i);
        res.set(0, tmp2);
        return res;
    }

    public static boolean correct(BitSet data) throws Exception {
        if (data.length() > DECODE_SIZE) throw new Exception("Invalid data size!");
        int pos = 0;
        boolean all_xor = data.get(0);
        for (int i = 1; i < DECODE_SIZE; i++) {
            all_xor ^= data.get(i);
            if (data.get(i))
                pos ^= i;
        }
        if (pos == 0) return true;
        if (all_xor) {
            data.flip(pos);
            return true;
        }
        return false;
    }

    public static BitSet decode(BitSet data) throws Exception {
        if (data.length() > DECODE_SIZE) throw new Exception("Invalid data size!");
        BitSet res = new BitSet(ENCODE_SIZE);
        int tmp = 1, j = 0;
        for (int i = 1; i < DECODE_SIZE; i++) {
            if (i == tmp) tmp <<= 1;
            else res.set(j++, data.get(i));
        }
        return res;
    }

    // UNIT TEST
//    public static void main(String[] args) throws Exception {
//
//        for (int j = 0; j < 500; j++) {
//            Random rand = new Random();
//            byte[] data = new byte[ENCODE_BYTE_SIZE];
//            for (int i = 0; i < ENCODE_BYTE_SIZE; i++)
//                data[i] = (byte) rand.nextInt(256);
////            System.out.println(Arrays.toString(data));
//
//            BitSet tmp1 = DataHelper.bytesToBitSet(data);
//            BitSet tmp2 = encode(tmp1);
//
//            // create error
//            tmp2.flip(84);
//
//            if (!correct(tmp2)) {
//                System.out.println("ERROR 1");
//                return;
//            }
//
//            BitSet tmp3 = decode(tmp2);
//            byte[] tmp4 = DataHelper.bitSetToBytes(tmp3, ENCODE_BYTE_SIZE);
//            for (int i = 0; i < ENCODE_BYTE_SIZE; i++) {
//                if (data[i] != tmp4[i]) {
//                    System.out.println("ERROR 2");
//                    break;
//                }
//            }
//
////            System.out.println(Arrays.toString(tmp4));
//        }
//
//    }

}
