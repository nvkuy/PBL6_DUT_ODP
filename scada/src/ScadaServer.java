import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.lang.Math.ceilDiv;

public class ScadaServer implements Runnable {

    /*
     * packet structure(in byte):
     * file_id(8) - unique id of file (in reality it will have some structure, for now it will be current millisecond)
     * num_of_bytes(4) - number of byte in file after compress
     * packet_id(4) - packet id (use to reconstruct the file)
     * packet_data(990) - packet data
     * checksum(16) - md5(file_id, num_of_packet, packet_id, packet_data)
     * => total: 1022 bytes + 2 bytes(14 bit ECC + 1 bit end) hamming code
     * TODO: add encryption later..
     * TODO: use another port for signal..
     * */

    private static final int FILE_ID_SIZE = 8;
    private static final int NUM_OF_BYTES_SIZE = 4;
    private static final int PACKET_ID_SIZE = 4;
    private static final int PACKET_DATA_SIZE = 990; // PACKET_DATA_SIZE % WORD_LEN == 0
    private static final int CHECKSUM_SIZE = 16;

    private static final int BUFFER_SIZE = 1 << 10;
    private static final int FILE_ID_START = 0;
    private static final int NUM_OF_BYTES_START = FILE_ID_START + FILE_ID_SIZE;
    private static final int PACKET_ID_START = NUM_OF_BYTES_START + NUM_OF_BYTES_SIZE;
    private static final int PACKET_DATA_START = PACKET_ID_START + PACKET_ID_SIZE;
    private static final int CHECKSUM_START = PACKET_DATA_START + PACKET_DATA_SIZE;
    private static final int CHECKSUM_END = CHECKSUM_START + CHECKSUM_SIZE;

    private static final int OFFICE_PORT = 8888;
    private static final InetAddress OFFICE_ADDRESS = InetAddress.getLoopbackAddress(); // OFFICE_IP

    private final DatagramSocket socket;
    private boolean running;

    private final ConcurrentLinkedQueue<String> dataQueue;

    public ScadaServer() throws Exception {
        dataQueue = new ConcurrentLinkedQueue<>();
        socket = new DatagramSocket();
        running = true;
    }

    public void stopServer() {
        running = false;
        socket.close();
    }

    public void sendFile(String filePath) {
        dataQueue.add(filePath);
    }

    @Override
    public void run() {

        while (running) {

            // TODO: try thread pool later..
            String filePath = dataQueue.poll();
            try {

                byte[] data = DataHelper.readFileBytes(filePath);
                data = Compresser.compress(data);

                byte[] file_id = DataHelper.longToBytes(System.currentTimeMillis(), FILE_ID_SIZE);
                byte[] num_of_bytes = DataHelper.longToBytes(data.length, NUM_OF_BYTES_SIZE);

                data = DataHelper.addPaddingWord(data);
                long[] ys = DataHelper.bytesToLongs(data);
                long[] xs = new long[ys.length];
                for (int i = 0; i < ys.length; i++) xs[i] = i;
                GlobalErrorCorrecter gec = new GlobalErrorCorrecter();
                gec.init(xs, ys);
                int redundant_word = ceilDiv(xs.length * GlobalErrorCorrecter.REDUNDANT_PERCENT, 100);
                int remainder_word = ((xs.length + redundant_word) * GlobalErrorCorrecter.WORD_LEN) % PACKET_DATA_SIZE;
                if (remainder_word > 0)
                    redundant_word += PACKET_DATA_SIZE - remainder_word;
                long[] xrs = new long[redundant_word];
                for (int i = 0; i < redundant_word; i++) xrs[i] = i + xs.length;
                long[] yrs = gec.getValues(xrs);
                ys = DataHelper.concatLongs(ys, yrs);

                data = DataHelper.longsToBytes(ys);
                xs = xrs = ys = yrs = null; // allow gc to clear data
                byte[] buf = new byte[BUFFER_SIZE];
                System.arraycopy(file_id, 0, buf, FILE_ID_START, FILE_ID_SIZE);
                System.arraycopy(num_of_bytes, 0, buf, NUM_OF_BYTES_START, NUM_OF_BYTES_SIZE);
                for (int i = 0; i < data.length; i += PACKET_DATA_SIZE) {
                    byte[] packet_id = DataHelper.longToBytes(i, PACKET_ID_SIZE);
                    System.arraycopy(packet_id, 0, buf, PACKET_ID_START, PACKET_ID_SIZE);
                    System.arraycopy(data, i, buf, PACKET_DATA_START, PACKET_DATA_SIZE);
                    byte[] tmp = new byte[CHECKSUM_START];
                    System.arraycopy(buf, 0, tmp, 0, CHECKSUM_START);
                    byte[] checksum = Hasher.hash(tmp);
                    System.arraycopy(checksum, 0, buf, CHECKSUM_START, CHECKSUM_SIZE);
                    buf = DataHelper.bitSetToBytes(LocalErrorCorrecter.encode(DataHelper.bytesToBitSet(Arrays.copyOf(buf, CHECKSUM_END))));
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, OFFICE_ADDRESS, OFFICE_PORT);
                    socket.send(packet);
                }


            } catch (Exception e) {
                System.out.println("Error while sending file " + filePath + " " + e.getMessage());
            }

        }

    }

}
