public class Main {
    public static void main(String[] args) throws Exception {
        ScadaServer server = new ScadaServer();
        new Thread(server).start();

        // test
        for (int i = 0; i < 10; i++)
            server.sendFile("D:\\PBL6\\scada\\src\\GlobalErrorCorrecter.java");
//        server.sendFile("/home/uy/PBL6/PBL6_DUT_ODP/scada/src/GlobalErrorCorrecter.java");
    }
}