import java.io.FileWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.ceilDiv;

public class DataPartHandler {

    private final int numOfBytes;
    private final int numOfWordNeeded;
    private final int numOfPartNeeded;

    private final byte[][] data;
    private final AtomicBoolean[] receiveState;
    private final AtomicInteger numPartWaiting, numPartReceived;

    public DataPartHandler(int numOfBytes) {
        this.numOfBytes = numOfBytes;
        numOfWordNeeded = ceilDiv(numOfBytes, GlobalErrorCorrecter.WORD_LEN);
        int redundantWord = ceilDiv(numOfWordNeeded * GlobalErrorCorrecter.REDUNDANT_PERCENT, 100);
        int maxNumOfWord = ceilDiv(numOfWordNeeded + redundantWord, Packet.NUM_OF_WORD_PER_PACKET) * Packet.NUM_OF_WORD_PER_PACKET;
        numOfPartNeeded = ceilDiv(numOfWordNeeded, Packet.NUM_OF_WORD_PER_PACKET);
        int maxNumOfPart = maxNumOfWord / Packet.NUM_OF_WORD_PER_PACKET;
        data = new byte[maxNumOfPart][];
        receiveState = new AtomicBoolean[maxNumOfPart];
        for (int i = 0; i < maxNumOfPart; i++) receiveState[i] = new AtomicBoolean();
        numPartWaiting = new AtomicInteger();
        numPartReceived = new AtomicInteger();
    }

    public void addPart(int partId, byte[] partData) {
        if (!receiveState[partId].compareAndSet(false, true)) { // avoid duplicate udp packet
            return;
        }
        if (numPartWaiting.incrementAndGet() > numOfPartNeeded) { // ignore when received enough
            numPartWaiting.decrementAndGet();
            return;
        }
        data[partId] = partData;
        numPartReceived.incrementAndGet();
    }

    public boolean isDone() {
        return numPartReceived.get() >= numOfPartNeeded;
    }

    public byte[] getFileBytes() {
        assert isDone();
        int lostPartCount = 0;
        for (int i = 0; i < numOfPartNeeded; i++) lostPartCount += (data[i] == null ? 1 : 0);

        if (lostPartCount > 0) {
            long[] xns = new long[lostPartCount * Packet.NUM_OF_WORD_PER_PACKET];
            long[] ys = new long[numOfWordNeeded];
            long[] xs = new long[numOfWordNeeded];
            int k = 0;
            for (int i = 0; i < data.length; i++) {
                if (k >= numOfWordNeeded) break;
                if (data[i] != null) {
                    int startId = i * Packet.NUM_OF_WORD_PER_PACKET;
                    for (int j = 0; j < data[i].length; j += GlobalErrorCorrecter.WORD_LEN) {
                        if (k >= numOfWordNeeded) break;
                        xs[k] = startId++;
                        ys[k++] = DataHelper.bytesToLong(data[i], j, GlobalErrorCorrecter.WORD_LEN);
                    }
                }
            }
            assert k == numOfWordNeeded;

            k = 0;
            for (int i = 0; i < numOfPartNeeded; i++) {
                if (data[i] == null) {
                    int startId = i * Packet.NUM_OF_WORD_PER_PACKET;
                    for (int j = 0; j < Packet.NUM_OF_WORD_PER_PACKET; j++) xns[k++] = startId++;
                }
            }
            assert k == xns.length;

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
            assert k == yns.length;
        }

        byte[] file = new byte[numOfBytes];
        for (int i = 0; i + 1 < numOfPartNeeded; i++) {
            assert data[i] != null;
            System.arraycopy(data[i], 0, file, i * Packet.PACKET_DATA_SIZE, Packet.PACKET_DATA_SIZE);
        }
        int remainder = numOfBytes % Packet.PACKET_DATA_SIZE;
        assert data[numOfPartNeeded - 1] != null;
        System.arraycopy(data[numOfPartNeeded - 1], 0, file, numOfBytes - remainder, remainder);

        return file;

    }

}