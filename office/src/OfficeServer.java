import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

public class OfficeServer implements Runnable {

    /*
     * packet structure(in byte):
     * file_id(8) - unique id of file (in reality it will have some structure, for now it will be current millisecond)
     * num_of_bytes(4) - number of byte in file after compress
     * packet_id(4) - packet id (use to reconstruct the file)
     * packet_data(990) - packet data
     * checksum(16) - md5(file_id, num_of_packet, packet_id, packet_data)
     * => total: 1022 bytes + 2 bytes(14 bit ECC + 1 bit end) hamming code
     * TODO: add encryption later..
     * TODO: may use another port for signal..
     * */

    // TODO: may replace concurrent data structure by database later..
    // TODO: may create class for packet structure later..

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
    private static final int SCADA_PORT = 6969;
    private static final InetAddress SCADA_ADDRESS = InetAddress.getLoopbackAddress(); // SCADA_IP

    private final DatagramSocket socket;
    private boolean running;

    private static final int RECEIVE_TIMEOUT = 10000;

    private static final Integer STATE_RECEIVE_SUCCESS = 1; // The file was constructed successfully.
    private static final Integer STATE_RECEIVE_FAIL = 0; // In case of construction failure or timeout, stop receiving.
    private static final Integer STATE_RECEIVING = 2; // Receiving.
    private static final Integer STATE_RECEIVE_ENOUGH = 3; // Received enough data to construct the file, stop receiving.
    private final ConcurrentHashMap<Integer, Integer> history;

    private final ConcurrentHashMap<Integer, DataPartHandler> receivingFiles;

    public OfficeServer() throws Exception {
        socket = new DatagramSocket(OFFICE_PORT);
        history = new ConcurrentHashMap<>();
        receivingFiles = new ConcurrentHashMap<>();
        running = true;
    }

    public void stopServer() {
        running = false;
        socket.close();
    }

    @Override
    public void run() {

        while (running) {

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                if (!packet.getAddress().equals(SCADA_ADDRESS) || packet.getPort() != SCADA_PORT)
                    throw new Exception("Invalid SCADA packet " + packet.getAddress() + ":" + packet.getPort());
                new Thread(new PacketHandler(buffer)).start();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

        }

    }

    private class PacketHandler implements Runnable {

        private byte[] data;

        public PacketHandler(byte[] data) {
            this.data = data;
        }

        @Override
        public void run() {

        }
    }

}
