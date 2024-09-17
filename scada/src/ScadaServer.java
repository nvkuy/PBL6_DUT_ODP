import java.net.DatagramPacket;
import java.net.DatagramSocket;

/*
* packet structure(in byte):
* file_id(8) - unique id of file (UUID)
* num_of_packet(4) - number of packet is sent
* packet_id(4) - packet id (use to reconstruct the file)
* packet_data(988) - packet data
* check_sum(16) - md5(file_id, num_of_packet, packet_id, packet_data)
* => total: 1020 bytes + 2 bytes(14 bit) hamming code
* */

public class ScadaServer implements Runnable {

    private static final int PORT = 8888;
    private static final int BUFFER_SIZE = 1 << 10;

    private DatagramSocket socket;
    private boolean running;

    public ScadaServer() throws Exception {
        socket = new DatagramSocket(PORT);
        running = true;
    }

    public void StopServer() {
        running = false;
    }

    @Override
    public void run() {

        while (running) {

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
            try {
                socket.receive(packet);

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

        }

    }

    private class PacketReceiver implements Runnable {

        private DatagramPacket packet;

        public PacketReceiver(DatagramPacket packet) {
            this.packet = packet;
        }

        @Override
        public void run() {

        }
    }

}
