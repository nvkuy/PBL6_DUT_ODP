import java.util.BitSet;

public class LocalErrorCorrecter {

    /*
    * using extended Hamming code to detect and correct 1-bit error in packet
    * https://en.wikipedia.org/wiki/Hamming_code
    * */

    public static final int ECC_BIT = 14;
    public static final int DECODE_SIZE = (1 << (ECC_BIT - 1));
    public static final int ENCODE_SIZE = DECODE_SIZE - ECC_BIT;

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
//    public static void main(String[] args) {
//        byte[] arr = {1, 0, 2, 4, 5, 1, 99, 127, 27, -128, 0, 0};
////        byte[] arr = new byte[1022];
////        arr[1021] = 2;
//        BitSet data = DataHelper.bytesToBitSet(arr);
//        System.out.println(data.length());
////        System.out.println(data);
//        try {
//            data = encode(data);
////            System.out.println(data.get(DECODE_SIZE - 1));
//            System.out.println(data.length());
////            System.out.println(data);
////            data.flip(12); // create error
//            data.flip(84); // create error
//            if (correct(data)) {
////                System.out.println("YES");
//                data = decode(data);
//                System.out.println(Arrays.toString(DataHelper.bitSetToBytes(data)));
//            } else {
//                System.out.println("Can't correct!");
//            }
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//    }

}
