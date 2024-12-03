public class Main {
    public static void main(String[] args) throws Exception {
        ScadaServer server = new ScadaServer();
        new Thread(server).start();

        // test
//        server.sendFile("C:\\Users\\nguye\\Downloads\\Slides-20240929T162825Z-001\\Slides\\Chapter5_DISCRETE FOURIER TRANSFORM.pdf");
//        server.sendFile("D:\\PBL6\\office\\received\\firefox.exe");
        server.sendFile("D:\\PBL6\\scada\\src\\GlobalErrorCorrecter.java");
//        server.sendFile("/home/uy/PBL6/PBL6_DUT_ODP/scada/src/GlobalErrorCorrecter.java");
//        server.sendFile("/home/uy/PBL6/PBL6_DUT_ODP/office/received/rs_nasa.pdf");
//        server.sendFile("D:\\PBL6\\office\\received\\dft.pdf");
//        server.sendFile("D:\\PBL6\\office\\received\\justine.pdf");
//        server.sendFile("/home/uy/PBL6/PBL6_DUT_ODP/office/received/NovelPolynomialBasisFFT2016.pdf");
//        server.sendFile("/home/uy/PBL6/PBL6_DUT_ODP/office/received/justine.pdf");
    }
}