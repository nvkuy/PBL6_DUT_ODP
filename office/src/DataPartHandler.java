import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.ceilDiv;

public class DataPartHandler {

    private int numOfBytes;
    private int numOfWordNeeded;
    private byte[][] data;
    private int[] received;
    private int numPartReceived;

    private boolean isInit;
    private final ReentrantLock lock;

    public DataPartHandler() {
        lock = new ReentrantLock();
        isInit = false;
    }

    public void initIfNeeded(int numOfBytes) {
        if (isInit) return;
        try {
            lock.lock();
            if (isInit) return;
            this.numOfBytes = numOfBytes;
            numOfWordNeeded = ceilDiv(numOfBytes, GlobalErrorCorrecter.WORD_LEN) * GlobalErrorCorrecter.WORD_LEN;
            int redundantWord = ceilDiv(numOfWordNeeded * GlobalErrorCorrecter.REDUNDANT_PERCENT, 100);
            int remainder = (numOfWordNeeded + redundantWord) % Packet.PACKET_DATA_SIZE;
            if (remainder > 0)
                redundantWord += Packet.PACKET_DATA_SIZE - remainder;
            int maxNumOfWord = numOfWordNeeded + redundantWord;
            data = new byte[maxNumOfWord][];
            received = new int[numOfWordNeeded];
            numPartReceived = 0;
            isInit = true;
        } finally {
            lock.unlock();
        }
    }

    public void addPart(int partId, byte[] partData) {
        if (numPartReceived * Packet.NUM_OF_WORD_PER_PACKET >= numOfWordNeeded) return;
        try {
            lock.lock();
            if (numPartReceived * Packet.NUM_OF_WORD_PER_PACKET >= numOfWordNeeded) return;
            data[partId] = partData;
            received[numPartReceived++] = partId;
        } finally {
            lock.unlock();
        }
    }

    public boolean isDone() {
        return numPartReceived * Packet.NUM_OF_WORD_PER_PACKET >= numOfWordNeeded; // may need atomic int
    }

    public byte[] getFileBytes() {
        try {
            lock.lock();

            int lostPartCount = 0;
            for (int i = 0; i < numPartReceived; i++) lostPartCount += (data[i] == null ? 1 : 0);

            if (lostPartCount > 0) {
                long[] xns = new long[lostPartCount * Packet.NUM_OF_WORD_PER_PACKET];
                long[] ys = new long[numOfWordNeeded];
                long[] xs = new long[numOfWordNeeded];
                int k = 0;
                for (int i = 0; i < numPartReceived; i++) {
                    int startId = received[i] * Packet.NUM_OF_WORD_PER_PACKET;
                    for (int j = 0; j < data[received[i]].length; j += GlobalErrorCorrecter.WORD_LEN) {
                        xs[k] = startId++;
                        ys[k++] = DataHelper.bytesToLong(data[received[i]], j, GlobalErrorCorrecter.WORD_LEN);
                    }
                }

                k = 0;
                for (int i = 0; i < numPartReceived; i++) {
                    if (data[i] == null) {
                        int startId = i * Packet.NUM_OF_WORD_PER_PACKET;
                        for (int j = 0; j < Packet.NUM_OF_WORD_PER_PACKET; j++) xns[k++] = startId++;
                    }
                }

                GlobalErrorCorrecter gec = new GlobalErrorCorrecter();
                gec.init(xs, ys);
                long[] yns = gec.getValues(xns);

                k = 0;
                for (int i = 0; i < numPartReceived; i++) {
                    if (data[i] == null) {
                        data[i] = DataHelper.longsToBytes(Arrays.copyOfRange(yns, k, k + Packet.NUM_OF_WORD_PER_PACKET));
                        k += Packet.NUM_OF_WORD_PER_PACKET;
                    }
                }
            }

            byte[] file = new byte[numOfBytes];
            for (int i = 0; i + 1 < numPartReceived; i++)
                System.arraycopy(data[i], 0, file, i * Packet.PACKET_DATA_SIZE, Packet.PACKET_DATA_SIZE);
            int remainder = numOfBytes % Packet.PACKET_DATA_SIZE;
            System.arraycopy(data[numPartReceived - 1], 0, file, numOfBytes - remainder, remainder);

            return file;

        } finally {
            lock.unlock();
        }
    }

}
