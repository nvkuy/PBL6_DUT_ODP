import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class File {

    private static final int FILE_NAME_SIZE = 69; // Allow only 68 bytes, last byte use to determine length

    public static final Integer STATE_RECEIVE_SUCCESS = 1; // The file was constructed successfully.
    public static final Integer STATE_RECEIVE_FAIL = 0; // In case of construction failure or timeout, stop receiving.
    public static final Integer STATE_RECEIVING = 2; // Receiving.
    public static final Integer STATE_RECEIVE_ENOUGH = 3; // Received enough data to construct the file, stop receiving.

    private final AtomicInteger state;
    private DataPartHandler data;
    private AtomicInteger version;

    public File(int numOfBytes) {
        state = new AtomicInteger(STATE_RECEIVING);
        data = new DataPartHandler(numOfBytes);
    }

    public int getState() {
        return state.get();
    }

    public void addPart(int partId, byte[] partData) {
        if (state.get() != STATE_RECEIVING) return;
        version.incrementAndGet();
        data.addPart(partId, partData);
        if (data.isDone()) state.compareAndSet(STATE_RECEIVING, STATE_RECEIVE_ENOUGH);
    }

    public void saveFile(String path) throws Exception {
//        assert state.get() == STATE_RECEIVE_ENOUGH;
        byte[] rawFile = data.getFileBytes();
        rawFile = Compresser.decompress(rawFile);
        String tmp = new String(Arrays.copyOfRange(rawFile, 0, FILE_NAME_SIZE));
        int last = FILE_NAME_SIZE - 2;
        while (tmp.charAt(last) == tmp.charAt(last + 1)) last--;
        String fileName = System.currentTimeMillis() + "_" + tmp.substring(0, last + 1);
        DataHelper.writeFileBytes(path + fileName, Arrays.copyOfRange(rawFile, FILE_NAME_SIZE, rawFile.length));
        data = null; // allow gc clear data
        state.set(STATE_RECEIVE_SUCCESS);
    }

    public int deleteIfSame(int oldVersion) {
        if (state.get() == STATE_RECEIVING && version.get() == oldVersion && state.compareAndSet(STATE_RECEIVING, STATE_RECEIVE_FAIL)) {
            data = null; // allow gc clear data
            return -1;
        }
        return version.get();
    }

}
