public class Main {
    public static void main(String[] args) throws Exception {
        ScadaServer server = new ScadaServer();
        new Thread(server).start();

        // test
        server.sendFile("D:\\PBL6\\scada\\src\\GlobalErrorCorrecter.java");
    }
}