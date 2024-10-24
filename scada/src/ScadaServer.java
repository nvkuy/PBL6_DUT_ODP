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

    // TODO: may replace concurrent data structure by database later..

    private static final int FILE_NAME_SIZE = 69; // Allow only 68 bytes, last byte use to determine length

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

                byte[] file_name = DataHelper.stringToBytes(DataHelper.getFileName(filePath));
                byte[] file_data = DataHelper.readFileBytes(filePath);

                if (file_name.length + 1 > FILE_NAME_SIZE) throw new Exception("File name too long!");

                byte[] data = new byte[FILE_NAME_SIZE + file_data.length];
                System.arraycopy(file_name, 0, data, 0, file_name.length);
                byte lastByte = 0;
                if (file_name[file_name.length - 1] == lastByte) lastByte++;
                for (int i = file_name.length; i < FILE_NAME_SIZE; i++) data[i] = lastByte;
                System.arraycopy(file_data, 0, data, FILE_NAME_SIZE, file_data.length);

                data = Compresser.compress(data);

                byte[] file_id = DataHelper.longToBytes(System.currentTimeMillis(), Packet.FILE_ID_SIZE);
                byte[] num_of_bytes = DataHelper.longToBytes(data.length, Packet.NUM_OF_BYTES_SIZE);

                data = DataHelper.addPaddingWord(data);
                long[] ys = DataHelper.bytesToSymbols(data);
                long[] xs = new long[ys.length];
                for (int i = 0; i < ys.length; i++) xs[i] = i;
                GlobalErrorCorrecter gec = new GlobalErrorCorrecter();
                gec.init(xs, ys);
                int redundant_word = ceilDiv(xs.length * GlobalErrorCorrecter.REDUNDANT_PERCENT, 100);
                int remainder_word = ((xs.length + redundant_word) * GlobalErrorCorrecter.WORD_LEN) % Packet.PACKET_DATA_SIZE;
                if (remainder_word > 0)
                    redundant_word += Packet.PACKET_DATA_SIZE - remainder_word;
                long[] xrs = new long[redundant_word];
                for (int i = 0; i < redundant_word; i++) xrs[i] = i + xs.length;
                long[] yrs = gec.getValues(xrs);
                ys = DataHelper.concatLongs(ys, yrs);

                data = DataHelper.symbolsToBytes(ys);
                xs = xrs = ys = yrs = null; // allow gc to clear data
                byte[] buf = new byte[Packet.BUFFER_SIZE];
                System.arraycopy(file_id, 0, buf, Packet.FILE_ID_START, Packet.FILE_ID_SIZE);
                System.arraycopy(num_of_bytes, 0, buf, Packet.NUM_OF_BYTES_START, Packet.NUM_OF_BYTES_SIZE);
                for (int i = 0; i < data.length; i += Packet.PACKET_DATA_SIZE) {
                    byte[] packet_id = DataHelper.longToBytes(i, Packet.PACKET_ID_SIZE);
                    System.arraycopy(packet_id, 0, buf, Packet.PACKET_ID_START, Packet.PACKET_ID_SIZE);
                    System.arraycopy(data, i, buf, Packet.PACKET_DATA_START, Packet.PACKET_DATA_SIZE);
                    byte[] tmp = new byte[Packet.CHECKSUM_START];
                    System.arraycopy(buf, 0, tmp, 0, Packet.CHECKSUM_START);
                    byte[] checksum = Hasher.hash(tmp);
                    System.arraycopy(checksum, 0, buf, Packet.CHECKSUM_START, Packet.CHECKSUM_SIZE);
                    buf = DataHelper.bitSetToBytes(LocalErrorCorrecter.encode(DataHelper.bytesToBitSet(Arrays.copyOf(buf, Packet.CHECKSUM_END))));
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, OFFICE_ADDRESS, OFFICE_PORT);
                    socket.send(packet);
                }


            } catch (Exception e) {
                System.out.println("Error while sending file " + filePath + " " + e.getMessage());
            }

        }

    }

}
