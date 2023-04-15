package game.server;

import game.config.Configurations;
import game.config.GameConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameServer {
    private ExecutorService executorService;
    private final Configurations configurations;
    private ServerSocket serverSocket;
    private static final Logger LOGGER = Logger.getLogger(GameServer.class.getName());

    public GameServer(Configurations configurations) throws IOException {
        this.configurations = configurations;
    }

    public void init() {
        executorService = Executors.newCachedThreadPool();
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(configurations.getPort());
            LOGGER.info("Server started on port " + configurations.getPort());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start() {
        init();
        openServerSocket();
    }

    public static void main(String[] args) throws IOException {
        LOGGER.setLevel(Level.CONFIG);

        Configurations configurations = new GameConfig();
        GameServer gameServer = new GameServer(configurations);
        gameServer.start();
    }
}
