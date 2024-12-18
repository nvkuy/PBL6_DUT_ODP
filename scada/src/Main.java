import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        ScadaServer server = new ScadaServer();
        new Thread(server).start();

        // console app..
        System.out.println("-f {file path} -n {num of file} or -q to quit.");
        Scanner scanner = new Scanner(System.in);
        String input;
        while (true) {

            input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("-q")) {
                System.out.println("Exiting program. Goodbye!");
                break;
            }

            if (input.startsWith("-f") && input.contains("-n")) {
                try {
                    String[] parts = input.split(" ");

                    String filePath = null;
                    int numOfFile = 0;

                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].equals("-f") && i + 1 < parts.length) {
                            filePath = parts[i + 1];
                        } else if (parts[i].equals("-n") && i + 1 < parts.length) {
                            numOfFile = Integer.parseInt(parts[i + 1]);
                        }
                    }

                    if (filePath != null && numOfFile > 0) {

                        for (int i = 0; i < numOfFile; i++)
                            server.sendFile(filePath);

                    } else {
                        System.out.println("Invalid command. Please use the format: -f {file path} -n {num of file}");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number format for {num_of_file}. Please enter a valid integer.");
                }
            } else {
                System.out.println("Invalid command. Please use the format: -f {file_path} -n {num_of_file} or -q to quit.");
            }

        }

        server.stopServer();

    }
}