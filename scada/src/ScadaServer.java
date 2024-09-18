import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.lang.Math.ceilDiv;

/*
* packet structure(in byte):
* file_id(8) - unique id of file (in reality it will have some structure, for now it will be current millisecond)
* num_of_bytes(4) - number of byte in file after compress
* packet_id(4) - packet id (use to reconstruct the file)
* packet_data(990) - packet data
* checksum(16) - md5(file_id, num_of_packet, packet_id, packet_data)
* => total: 1022 bytes + 2 bytes(14 bit) hamming code
* TODO: add encryption later..
* TODO: use another port for signal..
* */

public class ScadaServer implements Runnable {

    private static final int OFFICE_PORT = 8888;
    private static final InetAddress OFFICE_ADDRESS = InetAddress.getLoopbackAddress(); // OFFICE_IP

    private static final int BUFFER_SIZE = 1 << 10;
    private static final int PACKET_DATA_SIZE = 990; // PACKET_DATA_SIZE % WORD_LEN == 0

    private DatagramSocket socket;
    private boolean running;

    private ConcurrentLinkedQueue<String> dataQueue;

    public ScadaServer() throws Exception {
        dataQueue = new ConcurrentLinkedQueue<>();
        socket = new DatagramSocket();
        running = true;
    }

    public void StopServer() {
        running = false;
        socket.close();
    }

    public void SendFile(String filePath) {
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

                long file_id = System.currentTimeMillis();
                int num_of_bytes = data.length;

                data = DataHelper.addPadding(data);
                long[] ys = DataHelper.bytesToLongArray(data);
                long[] xs = new long[ys.length];
                for (int i = 0; i < ys.length; i++) xs[i] = i;
                GlobalErrorCorrecter gec = new GlobalErrorCorrecter();
                gec.Init(xs, ys);
                int redundant_word = ceilDiv(xs.length * 10, 100);
                long[] xrs = new long[redundant_word];
                for (int i = 0; i < redundant_word; i++) xrs[i] = i + xs.length;
                long[] yrs = gec.GetValues(xrs);
                ys = DataHelper.concatLongs(ys, yrs);

                data = DataHelper.longArrayToBytes(ys);
                xs = xrs = ys = yrs = null; // allow gc to clear data
                for (int i = 0; i < data.length; i += PACKET_DATA_SIZE) {
                    
                }


            } catch (Exception e) {
                System.out.println("Error while sending file " + filePath + " " + e.getMessage());
            }

        }

    }

}
