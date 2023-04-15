package game.client;

import java.net.Socket;

public class ClientHandler implements Runnable{
    public ClientHandler(Socket accept) {

    }

    @Override
    public void run() {
        System.out.println("Client connected");
    }
}
