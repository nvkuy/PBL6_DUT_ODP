import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataHandler {

    private ReentrantReadWriteLock lock;


    public DataHandler() {
        lock = new ReentrantReadWriteLock(true);
    }



}
