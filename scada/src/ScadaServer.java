import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.ceilDiv;
import static java.lang.Math.min;

public class ScadaServer implements Runnable {

    // TODO: may replace concurrent data structure by database later..

    private static final boolean DEBUG = true;
    private static final int FILE_NAME_SIZE = 69; // Allow only 68 bytes, last byte use to determine length

    private static final int OFFICE_PORT = 8888;
    private static final int SCADA_PORT = 6969;
    private static final InetAddress OFFICE_ADDRESS = InetAddress.getLoopbackAddress(); // OFFICE_IP
    private static final InetAddress SCADA_ADDRESS = InetAddress.getLoopbackAddress(); // SCADA_IP
//    private static final InetAddress OFFICE_ADDRESS;
//    private static final InetAddress SCADA_ADDRESS;
//
//    static {
//        try {
//            SCADA_ADDRESS = InetAddress.getByName("10.10.27.199");
//            OFFICE_ADDRESS = InetAddress.getByName("10.10.26.207");
//        } catch (UnknownHostException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private final DatagramSocket socket;
    private boolean running;

    private final ConcurrentLinkedQueue<String> dataQueue;

    private static final int STRONG_THREAD = Runtime.getRuntime().availableProcessors() * 2;
    private final ExecutorService executorService;

    public ScadaServer() throws Exception {
        executorService = Executors.newFixedThreadPool(STRONG_THREAD);
        dataQueue = new ConcurrentLinkedQueue<>();
        socket = new DatagramSocket(SCADA_PORT, SCADA_ADDRESS);
        running = true;
        if (DEBUG) {
            System.out.println("ScadaServer started!");
        }
    }

    public void stopServer() {
        running = false;
        socket.close();
        if (DEBUG) {
            System.out.println("ScadaServer stopped!");
        }
    }

    public void sendFile(String filePath) {
        dataQueue.add(filePath);
    }

    private class FileSender implements Runnable {

        private final String filePath;
        public FileSender(String filePath) {
            this.filePath = filePath;
        }


        @Override
        public void run() {

            try {

                long currentTime = System.currentTimeMillis();
                if (DEBUG) {
                    System.out.println("File id: " + currentTime);
                }

                byte[] file_name = DataHelper.stringToBytes(currentTime + "_" + DataHelper.getFileName(filePath));
                if (file_name.length + 1 > FILE_NAME_SIZE) throw new Exception("File name too long!");

                byte[] file_data = DataHelper.readFileBytes(filePath);

                if (DEBUG) {
                    System.out.println("File size(before compression): " + file_data.length);
                }

                byte[] data = new byte[FILE_NAME_SIZE + file_data.length];
                byte lastByte = 69;
                if (file_name[file_name.length - 1] == lastByte) lastByte = 96;
                for (int i = file_name.length; i < FILE_NAME_SIZE; i++) data[i] = lastByte;
                System.arraycopy(file_name, 0, data, 0, file_name.length);
                System.arraycopy(file_data, 0, data, FILE_NAME_SIZE, file_data.length);

                data = Compresser.compress(data);

                if (DEBUG) {
                    System.out.println("Sending file: " + filePath);
                    System.out.println("File size(after compression): " + data.length);
                }

                byte[] file_id = DataHelper.longToBytes(currentTime, Packet.FILE_ID_SIZE);
                byte[] num_of_bytes = DataHelper.longToBytes(data.length, Packet.NUM_OF_BYTES_SIZE);

                data = DataHelper.addPaddingWord(data);
                long[] ys = DataHelper.bytesToSymbols(data);
                long[] xs = new long[ys.length];
                for (int i = 0; i < ys.length; i++) xs[i] = i;
                GlobalErrorCorrecter gec = new GlobalErrorCorrecter();
                gec.init(xs, ys);
                int redundant_word = ceilDiv(xs.length * GlobalErrorCorrecter.REDUNDANT_PERCENT, 100);
                int remainder_word = (xs.length + redundant_word) % Packet.NUM_OF_WORD_PER_PACKET;
                if (remainder_word > 0)
                    redundant_word += Packet.NUM_OF_WORD_PER_PACKET - remainder_word;
                long[] xrs = new long[redundant_word];
                for (int i = 0; i < redundant_word; i++) xrs[i] = i + xs.length;
                long[] yrs = gec.getValues(xrs);
                ys = DataHelper.concatLongs(ys, yrs);

                data = DataHelper.symbolsToBytes(ys);

//                int fuck_len = xs.length;
//                xs = DataHelper.concatLongs(xs, xrs);
//                GlobalErrorCorrecter gec2 = new GlobalErrorCorrecter();
//                long[] f1 = Arrays.copyOfRange(xs, 330, fuck_len + 330);
//                long[] f2 = Arrays.copyOfRange(ys, 330, fuck_len + 330);
//                gec2.init(f1, f2);
//                long[] fuck = gec2.getValues(xs);
//                for (int i = 0; i < ys.length; i++) {
//                    if (ys[i] != fuck[i]) {
//                        System.out.println("Error");
//                    }
//                }
//                FileWriter writer = new FileWriter("fuck.txt");
//                for (int i = 0; i < f1.length; i++)
//                    writer.write(f1[i] + " " + f2[i] + "\n");
//                writer.close();

                if (DEBUG) {
                    System.out.println("File size(after add redundant): " + data.length);
                }

                xs = xrs = ys = yrs = null; // allow gc to clear data
                byte[] buf = new byte[LocalErrorCorrecter.ENCODE_BYTE_SIZE];
                System.arraycopy(file_id, 0, buf, Packet.FILE_ID_START, Packet.FILE_ID_SIZE);
                System.arraycopy(num_of_bytes, 0, buf, Packet.NUM_OF_BYTES_START, Packet.NUM_OF_BYTES_SIZE);
                for (int i = 0; i < data.length; i += Packet.PACKET_DATA_SIZE) {
                    byte[] packet_id = DataHelper.longToBytes(i / Packet.PACKET_DATA_SIZE, Packet.PACKET_ID_SIZE);
                    System.arraycopy(packet_id, 0, buf, Packet.PACKET_ID_START, Packet.PACKET_ID_SIZE);
                    System.arraycopy(data, i, buf, Packet.PACKET_DATA_START, Packet.PACKET_DATA_SIZE);
                    byte[] tmp = new byte[Packet.CHECKSUM_START];
                    System.arraycopy(buf, 0, tmp, 0, Packet.CHECKSUM_START);
                    byte[] checksum = Hasher.hash(tmp);
                    System.arraycopy(checksum, 0, buf, Packet.CHECKSUM_START, Packet.CHECKSUM_SIZE);
                    byte[] buffer = DataHelper.bitSetToBytes(LocalErrorCorrecter.encode(DataHelper.bytesToBitSet(buf)), LocalErrorCorrecter.DECODE_BYTE_SIZE);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, OFFICE_ADDRESS, OFFICE_PORT);
                    socket.send(packet);
//                    if (i / Packet.PACKET_DATA_SIZE == 406) {
//                        byte[] fuck = Arrays.copyOfRange(buf, Packet.PACKET_DATA_START, Packet.PACKET_DATA_END);
//                        System.out.println(Arrays.toString(fuck));
//                    }
                }

                if (DEBUG) {
                    System.out.println("Number of parts: " + data.length / Packet.PACKET_DATA_SIZE);
                    System.out.println("Send completed!");
                }


            } catch (Exception e) {
                // TODO: try retry later..
                System.out.println("Error while sending file " + filePath + " " + e.getMessage());
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    public void run() {

        while (running) {

            while (dataQueue.isEmpty()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            String filePath = dataQueue.poll();
            executorService.execute(new FileSender(filePath));

        }

    }

}
