import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FileHandler {

    public static final Integer STATE_RECEIVE_SUCCESS = 1; // The file was constructed successfully.
    public static final Integer STATE_RECEIVE_FAIL = 0; // In case of construction failure or timeout, stop receiving.
    public static final Integer STATE_RECEIVING = 2; // Receiving.
    public static final Integer STATE_RECEIVE_ENOUGH = 3; // Received enough data to construct the file, stop receiving.

    public static final int FILE_RECEIVE_TIMEOUT = 10000;
    private final ConcurrentLinkedQueue<Integer> receivingQueue;
    private final ConcurrentHashMap<Integer, Integer> lastReceived;
    private final ConcurrentHashMap<Integer, Integer> history;

    public FileHandler() {
        receivingQueue = new ConcurrentLinkedQueue<>();
        lastReceived = new ConcurrentHashMap<>();
        history = new ConcurrentHashMap<>();
    }

    public Integer getState(Integer fileId) {
        return history.get(fileId);
    }


}
