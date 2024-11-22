public class Main {
    public static void main(String[] args) throws Exception {
        ScadaServer server = new ScadaServer();
        new Thread(server).start();

        // test
//        server.sendFile("D:\\PBL6\\scada\\src\\GlobalErrorCorrecter.java");
//        server.sendFile("/home/uy/PBL6/PBL6_DUT_ODP/scada/src/GlobalErrorCorrecter.java");
        server.sendFile("/home/uy/PBL6/PBL6_DUT_ODP/office/received/rs_nasa.pdf");
    }
}