import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class GuessingGameClient {

    public static void main(String[] args) throws IOException {

        Socket socket = new Socket("localhost", 4444);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        Scanner scanner = new Scanner(System.in);

        String serverResponse;

        //System.out.println("Entering while loop");
        while (true) {
            //System.out.println("Waiting for server response");
            serverResponse = in.readLine();

            if (serverResponse.contains("Guess")) {
                System.out.println(serverResponse);
                int guess = scanner.nextInt();
                out.println(guess);
                //  System.out.println("Sent guess to server");
            } else if (serverResponse.contains("higher") || serverResponse.contains("lower")) {
                System.out.println(serverResponse);
            } else {
                System.out.println(serverResponse);
                break;
            }
            out.flush();
            //System.out.println("End of loop iteration");
        }

        // Get final score
        serverResponse = in.readLine();
        System.out.println(serverResponse);

        in.close();
        out.close();
        socket.close();
    }
}
