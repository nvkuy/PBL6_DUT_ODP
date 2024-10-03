import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

public class OfficeServer implements Runnable {

    // TODO: may replace concurrent data structure by database later..
    // TODO: may create class for packet structure later..

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

            byte[] buffer = new byte[Packet.BUFFER_SIZE];
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
