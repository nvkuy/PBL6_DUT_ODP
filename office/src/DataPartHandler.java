import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.ceilDiv;

public class DataPartHandler {

    private final int numOfBytes;
    private final int numOfWordNeeded;
    private final int numOfPartNeeded;

    private final byte[][] data;
    private final AtomicBoolean[] received;
    private final AtomicInteger numPartReceived;

    public DataPartHandler(int numOfBytes) {
        this.numOfBytes = numOfBytes;
        numOfWordNeeded = ceilDiv(numOfBytes, GlobalErrorCorrecter.WORD_LEN) * GlobalErrorCorrecter.WORD_LEN;
        int redundantWord = ceilDiv(numOfWordNeeded * GlobalErrorCorrecter.REDUNDANT_PERCENT, 100);
        int maxNumOfWord = ceilDiv(numOfWordNeeded + redundantWord, Packet.NUM_OF_WORD_PER_PACKET) * Packet.NUM_OF_WORD_PER_PACKET;
        numOfPartNeeded = ceilDiv(numOfWordNeeded, Packet.NUM_OF_WORD_PER_PACKET) * Packet.NUM_OF_WORD_PER_PACKET;
        int maxNumOfPart = maxNumOfWord / Packet.NUM_OF_WORD_PER_PACKET;
        data = new byte[maxNumOfPart][];
        received = new AtomicBoolean[maxNumOfPart];
        numPartReceived = new AtomicInteger();
    }

    public void addPart(int partId, byte[] partData) {
        if (!received[partId].compareAndSet(false, true)) { // avoid duplicate udp packet
            return;
        }
        if (numPartReceived.incrementAndGet() > numOfPartNeeded) { // ignore when received enough
            numPartReceived.decrementAndGet();
            return;
        }
        data[partId] = partData;
    }

    public boolean isDone() {
        return numPartReceived.get() >= numOfPartNeeded; // may need atomic int
    }

    public byte[] getFileBytes() {
        int lostPartCount = 0;
        for (int i = 0; i < numOfPartNeeded; i++) lostPartCount += (data[i] == null ? 1 : 0);

        if (lostPartCount > 0) {
            long[] xns = new long[lostPartCount * Packet.NUM_OF_WORD_PER_PACKET];
            long[] ys = new long[numOfWordNeeded];
            long[] xs = new long[numOfWordNeeded];
            int k = 0;
            for (int i = 0; i < received.length; i++) {
                if (data[i] != null) {
                    int startId = i * Packet.NUM_OF_WORD_PER_PACKET;
                    for (int j = 0; j < data[i].length; j += GlobalErrorCorrecter.WORD_LEN) {
                        xs[k] = startId++;
                        ys[k++] = DataHelper.bytesToLong(data[i], j, GlobalErrorCorrecter.WORD_LEN);
                    }
                }
            }

            k = 0;
            for (int i = 0; i < numOfPartNeeded; i++) {
                if (data[i] == null) {
                    int startId = i * Packet.NUM_OF_WORD_PER_PACKET;
                    for (int j = 0; j < Packet.NUM_OF_WORD_PER_PACKET; j++) xns[k++] = startId++;
                }
            }

            GlobalErrorCorrecter gec = new GlobalErrorCorrecter();
            gec.init(xs, ys);
            long[] yns = gec.getValues(xns);

            k = 0;
            for (int i = 0; i < numOfPartNeeded; i++) {
                if (data[i] == null) {
                    data[i] = DataHelper.symbolsToBytes(Arrays.copyOfRange(yns, k, k + Packet.NUM_OF_WORD_PER_PACKET));
                    k += Packet.NUM_OF_WORD_PER_PACKET;
                }
            }
        }

        byte[] file = new byte[numOfBytes];
        for (int i = 0; i + 1 < numOfPartNeeded; i++) {
//            assert data[i] != null;
            System.arraycopy(data[i], 0, file, i * Packet.PACKET_DATA_SIZE, Packet.PACKET_DATA_SIZE);
        }
        int remainder = numOfBytes % Packet.PACKET_DATA_SIZE;
//        assert data[numOfPartNeeded - 1] != null;
        System.arraycopy(data[numOfPartNeeded - 1], 0, file, numOfBytes - remainder, remainder);

        return file;

    }

}