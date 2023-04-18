package game.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket accept) {
        this.socket = accept;
    }

    private String generateRandomToken() {
        return "token" + Math.random() + socket.getLocalPort() + socket.getPort();
    }

    @Override
    public void run() {
        String token = generateRandomToken();
        System.out.println("Client connected with token : " + token);

        // add client to the server's list
        GameServer.clients.put(token, socket); //TODO: lock here --> we are writting

        // TODO: authentication



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
