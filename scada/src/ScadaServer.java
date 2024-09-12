import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/*
*
* */

public class ScadaServer implements Runnable {

    private static final int PORT = 8888;
    private static final int BUFFER_SIZE = 1024;

    private DatagramSocket socket;
    private boolean running;

    public ScadaServer() throws Exception {
        socket = new DatagramSocket(PORT);
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
