package game.server;

import game.client.Client;
import game.client.ClientHandler;
import game.config.Configurations;
import game.config.GameConfig;
import game.logic.GameModel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
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
        super();
        this.configurations = configurations;
    }

    public void init() {
        // var boundedQueue = new ArrayBlockingQueue<Runnable>(1000);
        // new ThreadPoolExecutor(10, 20, 60, SECONDS, boundedQueue, new AbortPolicy());

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

    private void acceptConnections() throws IOException {
        while (serverSocket.isOpen()) {
            SocketChannel socketChannel = serverSocket.accept();
            if (socketChannel != null) {
                executorService.submit(
                        new ClientHandler(socketChannel.socket()) // TODO: VER ISTO: isto garante que n está sempre a executar uma nova thread?
                );
            }
        }
    }

    private void start() throws IOException {
        init();
        openServerSocket();

        // Start the RMI registry on port 1099
        Registry registry = LocateRegistry.createRegistry(1099);

        // Create an instance of the remote object and bind it to the registry
        PlayingServer playingServer = new PlayingServer();
        registry.rebind("playingServer", playingServer);

        acceptConnections();
    }

    public static void main(String[] args) throws IOException {
        LOGGER.setLevel(Level.CONFIG);

        Configurations configurations = new GameConfig();
        GameServer gameServer = new GameServer(configurations);
        gameServer.start();
    }

}
