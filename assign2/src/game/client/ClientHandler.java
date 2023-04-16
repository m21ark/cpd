package game.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClientHandler implements Runnable{
    private final Socket socket;
    public ClientHandler(Socket accept) {
        this.socket = accept;
    }

    @Override
    public void run() {
        try {
            System.out.println("Client connected");

            // get input stream from socket
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // check if there is data available to be read
            if (inputStream.available() > 0) {
                // read string from client
                String message = reader.readLine();

                System.out.println("Received message from client: " + message);
            }
            System.out.println("Client connected2");


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
