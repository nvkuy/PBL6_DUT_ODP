import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OfficeServer implements Runnable {

    // TODO: may replace concurrent data structure by database later..

    private static final boolean DEBUG = true;

    private final String FILE_PATH;
    private static final int FILE_TIMEOUT = 10 * 1000;

    private static final int OFFICE_PORT = 8888;
    private static final int SCADA_PORT = 6969;
    private final InetAddress OFFICE_ADDRESS; // OFFICE_IP
    private final InetAddress SCADA_ADDRESS; // SCADA_IP


    private final DatagramSocket socket;
    private boolean running;

    private final ConcurrentHashMap<Long, ReadWriteLock> registerLocks;
    private final ConcurrentHashMap<Long, File> files;

    private static final int STRONG_THREAD = Runtime.getRuntime().availableProcessors() * 4;
    private final ExecutorService executorService;

    public OfficeServer() throws Exception {

        if (DEBUG) {
            OFFICE_ADDRESS = InetAddress.getLoopbackAddress();
            SCADA_ADDRESS = InetAddress.getLoopbackAddress();
        } else {
            SCADA_ADDRESS = InetAddress.getByName("2.2.2.2");
            OFFICE_ADDRESS = InetAddress.getByName("3.3.3.2");
        }
        FILE_PATH = Paths.get("").toAbsolutePath().toString() +
                FileSystems.getDefault().getSeparator() + "received" + FileSystems.getDefault().getSeparator();

        executorService = Executors.newFixedThreadPool(STRONG_THREAD);
        socket = new DatagramSocket(OFFICE_PORT, OFFICE_ADDRESS);
        registerLocks = new ConcurrentHashMap<>();
        files = new ConcurrentHashMap<>();
        running = true;

        Files.createDirectories(Paths.get(FILE_PATH));

        if (DEBUG) {
            System.out.println("OfficeServer started!");
            System.out.println("Save file at: " + FILE_PATH);
        }

    }

    public void stopServer() {
        running = false;
        socket.close();

        if (DEBUG) {
            System.out.println("OfficeServer stopped!");
        }

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
                executorService.execute(new PacketHandler(buffer));
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

        }

    }

    private class FileRegister implements Runnable {

        /*
        * light-weight, short-live thread use to:
        * 1. Register file with FileTracker (if needed)
        * 2. Push task to os thread do computational intensive task
        */

        private final byte[] data;

        public FileRegister(byte[] data) {
            this.data = data;
        }

        @Override
        public void run() {

            boolean init = false;
            long fileId = DataHelper.bytesToLong(data, 0, Packet.FILE_ID_SIZE);
            int numOfBytes = (int) DataHelper.bytesToLong(data, Packet.NUM_OF_BYTES_START, Packet.NUM_OF_BYTES_SIZE);

            registerLocks.putIfAbsent(fileId, new ReentrantReadWriteLock(true));
            ReadWriteLock initLock = registerLocks.get(fileId);

            // Notes: no try-finally because no exception, may need add later..
            initLock.readLock().lock();
            File file = files.get(fileId);
            if (file == null) {
                initLock.readLock().unlock();
                initLock.writeLock().lock();
                file = files.get(fileId);
                if (file == null) {
                    files.put(fileId, new File(numOfBytes));
                    file = files.get(fileId);
                    init = true;

                    if (DEBUG) {
                        System.out.println("File " + fileId + " registered!");
                    }

                }
                initLock.writeLock().unlock();
                initLock.readLock().lock();
            }
            initLock.readLock().unlock();

            if (file.getState() != File.STATE_RECEIVING) return;
            if (init) Thread.startVirtualThread(new FileTracker(file));
            executorService.execute(new FileHandler(file, data));

        }
    }

    private class FileTracker implements Runnable {

        /*
         * light-weight, long-live thread use to track and delete if file is timeout
         */

        private final File file;

        public FileTracker(File file) {
            this.file = file;
        }

        @Override
        public void run() {

            int version = 0;
            while (running) {
                try {
                    Thread.sleep(FILE_TIMEOUT);
                } catch (InterruptedException e) {
                    System.out.println("Error: " + e.getMessage());
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
                if (file.getState() != File.STATE_RECEIVING) break;
                version = file.deleteIfSame(version);
                if (version < 0) {
                    if (DEBUG) {
                        System.out.println("File timeout, deleted!");
                    }
                    break;
                }
            }

        }
    }

    private class PacketHandler implements Runnable {

        /*
         * os, long-live thread use to process packet(hamming code)
         */

        private final byte[] data;

        private PacketHandler(byte[] data) {
            this.data = data;
        }

        @Override
        public void run() {

            BitSet bits = DataHelper.bytesToBitSet(data);
            try {
                if (DEBUG) {
                    // create error
                    bits.flip(84);
                }
                if (LocalErrorCorrecter.correct(bits)) {
                    bits = LocalErrorCorrecter.decode(bits);
                    Thread.startVirtualThread(new FileRegister(DataHelper.bitSetToBytes(bits, LocalErrorCorrecter.ENCODE_BYTE_SIZE)));
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

        }
    }

    private class FileHandler implements Runnable {

        /*
         * os, long-live thread use to process part and construct final file
         */

        private final File file;
        private final byte[] data;

        public FileHandler(File file, byte[] data) {
            this.file = file;
            this.data = data;
        }

        @Override
        public void run() {

            try {
                int partId = (int) DataHelper.bytesToLong(data, Packet.PACKET_ID_START, Packet.PACKET_ID_SIZE);
                byte[] partData = Arrays.copyOfRange(data, Packet.PACKET_DATA_START, Packet.PACKET_DATA_END);
                byte[] checksum = Hasher.hash(Arrays.copyOfRange(data, 0, Packet.PACKET_DATA_END));
                for (int i = 0; i < Packet.CHECKSUM_SIZE; i++)
                    if (checksum[i] != data[Packet.CHECKSUM_START + i]) return;

                if (DEBUG) {
                    System.out.println("Received part id: " + partId);
//                    if (partId == 0) {
//                        System.out.println(Arrays.toString(partData));
//                    }
                    // create packet loss
//                    if (partId == 0) return;
                }

                file.addPart(partId, partData);
                if (file.getState() == File.STATE_RECEIVE_ENOUGH && file.saveFile(FILE_PATH)) {
                    System.out.println("File receive enough, saving to " + FILE_PATH);
                }

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

        }
    }

}
