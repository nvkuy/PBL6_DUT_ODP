import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OfficeServer implements Runnable {

    // TODO: may replace concurrent data structure by database later..

    private static final String FILE_PATH = "./receive_file/";
    private static final int FILE_TIMEOUT = 10 * 1000;

    private static final int OFFICE_PORT = 8888;
    private static final int SCADA_PORT = 6969;
    private static final InetAddress SCADA_ADDRESS = InetAddress.getLoopbackAddress(); // SCADA_IP

    private final DatagramSocket socket;
    private boolean running;

    private final ConcurrentHashMap<Long, ReadWriteLock> registerLocks;
    private final ConcurrentHashMap<Long, File> files;

    private static final int STRONG_THREAD = Math.ceilDiv(Runtime.getRuntime().availableProcessors() * 7, 10);
    private final ExecutorService executorService;

    public OfficeServer() throws Exception {
        executorService = Executors.newFixedThreadPool(STRONG_THREAD);
        socket = new DatagramSocket(OFFICE_PORT);
        registerLocks = new ConcurrentHashMap<>();
        files = new ConcurrentHashMap<>();
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
                Thread.startVirtualThread(new FileRegister(buffer));
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

        }

    }

    private class FileRegister implements Runnable {

        /*
        * light-weight, short-live thread use to:
        * 1. Register file with FileTracker (if needed)
        * 2. Push task to os thread do computational intensive task
        */

        private byte[] data;

        public FileRegister(byte[] data) {
            this.data = data;
        }

        @Override
        public void run() {

            boolean init = false;
            long fileId = DataHelper.bytesToLong(data, 0, Packet.FILE_ID_SIZE);
            int numOfBytes = (int) DataHelper.bytesToLong(data, Packet.NUM_OF_BYTES_START, Packet.NUM_OF_BYTES_SIZE);

            ReadWriteLock initLock = registerLocks.putIfAbsent(fileId, new ReentrantReadWriteLock());

            try {
                initLock.readLock().lock();
                File file = files.get(fileId);
                if (file == null) {
                    try {
                        initLock.readLock().unlock();
                        initLock.writeLock().lock();
                        file = files.put(fileId, new File(numOfBytes));
                        init = true;
                    } finally {
                        initLock.writeLock().unlock();
                        initLock.readLock().lock();

                    }
                }

                if (file.getState() != File.STATE_RECEIVING) return;

                if (init) Thread.startVirtualThread(new FileTracker(file));

                executorService.execute(new FileHandler(file, data));

            } finally {
                initLock.readLock().unlock();
            }

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
                }
                if (file.getState() != File.STATE_RECEIVING) break;
                version = file.deleteIfSame(version);
                if (version < 0) break;
            }

        }
    }

    private class FileHandler implements Runnable {

        /*
         * os, long-live thread use to process packet and construct final file
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
                    if (checksum[i] != partData[Packet.CHECKSUM_START + i]) return;

                file.addPart(partId, partData);
                if (file.getState() == File.STATE_RECEIVE_ENOUGH)
                    file.saveFile(FILE_PATH);

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

        }
    }

//    private class PacketHandler implements Runnable {
//
//        private byte[] data;
//
//        public PacketHandler(byte[] data) {
//            this.data = data;
//        }
//
//        @Override
//        public void run() {
//
//            boolean init = false;
//            long fileId = DataHelper.bytesToLong(data, 0, Packet.FILE_ID_SIZE);
//            int numOfBytes = (int) DataHelper.bytesToLong(data, Packet.NUM_OF_BYTES_START, Packet.NUM_OF_BYTES_SIZE);
//
//            ReadWriteLock initLock = registerLocks.putIfAbsent(fileId, new ReentrantReadWriteLock());
//
//            try {
//                initLock.readLock().lock();
//                File file = files.get(fileId);
//                if (file == null) {
//                    try {
//                        initLock.readLock().unlock();
//                        initLock.writeLock().lock();
//                        file = files.put(fileId, new File(FILE_PATH, numOfBytes));
//                        init = true;
//                    } finally {
//                        initLock.writeLock().unlock();
//                        initLock.readLock().lock();
//
//                    }
//                }
//
//                if (file.getState() != File.STATE_RECEIVING) return;
//
//                int partId = (int) DataHelper.bytesToLong(data, Packet.PACKET_ID_START, Packet.PACKET_ID_SIZE);
//                byte[] partData = Arrays.copyOfRange(data, Packet.PACKET_DATA_START, Packet.PACKET_DATA_END);
//                byte[] checksum = Hasher.hash(Arrays.copyOfRange(data, 0, Packet.PACKET_DATA_END));
//                for (int i = 0; i < Packet.CHECKSUM_SIZE; i++)
//                    if (checksum[i] != partData[Packet.CHECKSUM_START + i])
//                        throw new Exception("Checksum does not match");
//
//                file.addPart(partId, partData);
//
//                if (init) new Thread(new FileHandler(file)).start();
//
//            } catch (Exception e) {
//                System.out.println("Error: " + e.getMessage());
//            } finally {
//                initLock.readLock().unlock();
//            }
//
//        }
//    }

//    private class FileHandler implements Runnable {
//
//        private final File file;
//
//        public FileHandler(File file) {
//            this.file = file;
//        }
//
//        @Override
//        public void run() {
//
//            int version = 0;
//            int back_off = INIT_WAIT;
//            while (running) {
//
//                try {
//                    if (file.saveIfDone()) return;
//                    if (back_off >= FILE_TIMEOUT) {
//                        version = file.deleteIfSame(version);
//                        if (version < 0) return;
//                        back_off = INIT_WAIT;
//                    }
//                    Thread.sleep(back_off);
//                    back_off *= 2;
//                } catch (Exception e) {
//                    System.out.println("Error: " + e.getMessage());
//                }
//
//            }
//
//        }
//    }

}
