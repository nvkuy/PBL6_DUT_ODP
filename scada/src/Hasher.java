import java.security.MessageDigest;

public class Hasher {

    /*
    * using MD5 for hashing, mainly use for checksum
    * https://vi.wikipedia.org/wiki/MD5
    * */

    public static byte[] hash(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        return md.digest();
    }

    // UNIT TEST
//    public static void main(String[] args) throws Exception {
//
//        String a = "123abc";
//        String b = "123abd";
//
//        System.out.println(Arrays.toString(hash(a.getBytes())));
//        System.out.println(Arrays.toString(hash(b.getBytes())));
//        System.out.println(hash(a.getBytes()).length);
//
//    }

}
