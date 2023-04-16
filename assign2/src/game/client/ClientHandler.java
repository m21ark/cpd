package game.client;

import java.net.Socket;

public class ClientHandler implements Runnable{
    private final Socket socket;
    public ClientHandler(Socket accept) {
        this.socket = accept;
    }

    @Override
    public void run() {
        System.out.println("Client connected");
    }
}
