public class Main {
    public static void main(String[] args) throws Exception {
        new Thread(new OfficeServer()).start();
    }
}