package game.client;

import game.server.GameServer;
import game.server.PlayingServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private final Socket socket;
    public ClientHandler(Socket accept) {
        this.socket = accept;
    }

    private String generateRandomToken() {
        return "token" + Math.random() + socket.getLocalPort()+ socket.getPort();
    }

    @Override
    public void run() {
        System.out.println("Client connected");
        String token = generateRandomToken();

        GameServer.clients.put(token, socket); //TODO: lock aqui

        // TODO: auhtentication

        // write to client
        OutputStream output = null;
        try {
            output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(token);
            // GameServer.playingServer.queueGame(
            //         new PlayingServer.WrappedPlayerSocket(
            //                 new GamePlayer("Player", 1), //tem de vir da autenticaçao
            //                 socket)
            // );
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
