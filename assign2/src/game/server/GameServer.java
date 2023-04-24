package game.server;

import game.config.Configurations;
import game.config.GameConfig;
import game.utils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Lembrar de ver isto melhor ... https://hackernoon.com/implementing-an-event-loop-in-java-for-fun-and-profit

public class GameServer {

    public static PlayingServer playingServer;
    public static Map<String, Socket> clients = new HashMap<>(); // TODO: tornar isto thread safe
    private final Configurations configurations;
    private ExecutorService executorService;
    private ServerSocketChannel serverSocket;

    public GameServer(Configurations configurations) {
        super();
        this.configurations = configurations;
    }

    public static Socket getSocket(String token) {
        return clients.get(token);
    }

    public static void main(String[] args) throws IOException {
        Configurations configurations;
        if (Arrays.stream(args).toList().contains("-debug")) {
            configurations = new GameConfig(true);
            ClientHandler.DEBUG_MODE = true;
        } else configurations = GameConfig.getInstance();

        GameServer gameServer = new GameServer(configurations);
        gameServer.start();
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
            Logger.info("Server started on port " + configurations.getPort());
        } catch (IOException e) {
            Logger.error("Could not open server socket on port " + configurations.getPort());
            System.exit(1);
        }
    }

    private void acceptConnections() throws IOException {
        Logger.info("Waiting for connections...");
        while (serverSocket.isOpen()) {
            SocketChannel socketChannel = serverSocket.accept();
            if (socketChannel != null) executorService.submit(new ClientHandler(socketChannel.socket()));
            else {
                //try {
                //    Thread.sleep(1000); // Fiz isto para não ficar sempre a verificar se há novas conexões (RIP CPU) // TODO tirar fora dps
                //} catch (InterruptedException e) {
                //    e.printStackTrace();
                //}
            }
        }
    }


    private void start() throws IOException {
        init();
        openServerSocket();

        // Start the RMI registry
        int RMIPort = GameConfig.getInstance().getRMIReg();
        Registry registry = LocateRegistry.createRegistry(RMIPort);

        // Create an instance of the remote object and bind it to the registry
        playingServer = new PlayingServer();
        registry.rebind("playingServer", playingServer);

        acceptConnections();
    }

}
