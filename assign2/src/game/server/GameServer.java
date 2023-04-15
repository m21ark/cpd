package game.server;

import game.client.ClientHandler;
import game.config.Configurations;
import game.config.GameConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

// Lembrar de ver isto melhor ... https://hackernoon.com/implementing-an-event-loop-in-java-for-fun-and-profit

public class GameServer {
    private ExecutorService executorService;
    private final Configurations configurations;
    private ServerSocketChannel serverSocket;
    private static final Logger LOGGER = Logger.getLogger(GameServer.class.getName());

    public GameServer(Configurations configurations) throws IOException {
        this.configurations = configurations;
    }

    public void init() {
        executorService = Executors.newCachedThreadPool();
    }

    private void openServerSocket() {
        try {
            this.serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress(configurations.getAddress(), configurations.getPort()));
            serverSocket.configureBlocking(false);
            LOGGER.info("Server started on port " + configurations.getPort());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleClient() {

    }

    private void acceptConnections() throws IOException {
        while (serverSocket.isOpen()) {
            SocketChannel socketChannel = serverSocket.accept();
            if (socketChannel != null) {
                executorService.submit(
                        new ClientHandler(socketChannel.socket())
                );
            }
        }
    }

    private void start() throws IOException {
        init();
        openServerSocket();
        acceptConnections();
    }

    public static void main(String[] args) throws IOException {
        LOGGER.setLevel(Level.CONFIG);

        Configurations configurations = new GameConfig();
        GameServer gameServer = new GameServer(configurations);
        gameServer.start();
    }
}
