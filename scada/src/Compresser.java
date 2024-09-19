import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compresser {

    /*
     * using gzip to compress data before sending
     * TODO: research Asymmetric numeral systems later
     * */

    public static byte[] compress(byte[] data) throws Exception {
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(data);
        gzip.close();
        return obj.toByteArray();
    }

    public static byte[] decompress(byte[] data) throws Exception {
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1 << 10]; // may make it smaller..
        int len;
        while ((len = gis.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        gis.close();
        return bos.toByteArray();
    }

    // UNIT TEST
//    public static void main(String[] args) throws Exception {
//
//        String test = "AJDHFKLJS23435643@@~!@~!@~HDKFJHSDKJFHKS";
//        byte[] compressData = compress(test.getBytes());
//        byte[] decompressData = decompress(compressData);
//        System.out.println(new String(decompressData));
//        assert test.equals(new String(decompressData));
//
//    }

}
