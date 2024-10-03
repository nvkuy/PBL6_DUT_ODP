public abstract class Packet {

    /*
     * packet structure(in byte):
     * file_id(8) - unique id of file (in reality it will have some structure, for now it will be current millisecond)
     * num_of_bytes(4) - number of byte in file after compress
     * packet_id(4) - packet id (use to reconstruct the file)
     * packet_data(990) - packet data
     * checksum(16) - md5(file_id, num_of_packet, packet_id, packet_data)
     * => total: 1022 bytes + 2 bytes(14 bit ECC + 1 bit end) hamming code
     * TODO: add encryption later..
     * TODO: may use another port for signal..
     * */

    public static final int FILE_ID_SIZE = 8;
    public static final int NUM_OF_BYTES_SIZE = 4;
    public static final int PACKET_ID_SIZE = 4;
    public static final int PACKET_DATA_SIZE = 990; // PACKET_DATA_SIZE % WORD_LEN == 0
    public static final int CHECKSUM_SIZE = 16;

    public static final int BUFFER_SIZE = 1 << 10;
    public static final int FILE_ID_START = 0;
    public static final int NUM_OF_BYTES_START = FILE_ID_START + FILE_ID_SIZE;
    public static final int PACKET_ID_START = NUM_OF_BYTES_START + NUM_OF_BYTES_SIZE;
    public static final int PACKET_DATA_START = PACKET_ID_START + PACKET_ID_SIZE;
    public static final int CHECKSUM_START = PACKET_DATA_START + PACKET_DATA_SIZE;
    public static final int CHECKSUM_END = CHECKSUM_START + CHECKSUM_SIZE;
    public static final int NUM_OF_WORD_PER_PACKET = PACKET_DATA_SIZE / GlobalErrorCorrecter.WORD_LEN;

}
