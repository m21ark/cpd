package game.server;

import game.config.Configurations;
import game.config.GameConfig;
import game.logic.structures.MyConcurrentMap;
import game.protocols.TokenState;
import game.server.schedulers.ScheduledRankRelaxer;
import game.server.schedulers.ScheduledSerializer;
import game.utils.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class GameServer implements Serializable {

    static GameServer instance = null;
    private final Configurations configurations;
    public PlayingServer playingServer;
    public MyConcurrentMap<String, Socket> clients = new MyConcurrentMap<>();
    public MyConcurrentMap<String, TokenState> clientsStates = new MyConcurrentMap<>();
    private transient ExecutorService executorService;
    private transient ServerSocketChannel serverSocket;
    private transient ScheduledExecutorService scheduler;

    public GameServer(Configurations configurations) {
        super();
        this.configurations = configurations;
    }

    public static Socket getSocket(String token) {
        return instance.clients.get(token);
    }

    public static GameServer getInstance() {
        return instance;
    }

    private static void setInstance(GameServer gameServer) {
        GameServer.instance = gameServer;
    }

    public static GameServer checkSerializableServer() {
        // if files exists call and deserialize game.server
        try {
            FileInputStream fileIn = new FileInputStream("cache/gameServer.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            GameServer gameServer = (GameServer) in.readObject();
            in.close();
            fileIn.close();
            return gameServer;
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException | ClassNotFoundException e) {
            Logger.error("Retrieving previous game.server instance failed: " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        Configurations configurations;
        List<String> argsList = Arrays.stream(args).toList();
        Files.createDirectories(Paths.get("cache"));

        if (argsList.contains("-debug")) {
            configurations = new GameConfig();
            ClientHandler.DEBUG_MODE = true;
        } else configurations = GameConfig.getInstance();

        GameServer gameServer = checkSerializableServer();

        if (gameServer == null || argsList.contains("-new")) {

            gameServer = new GameServer(configurations);
        } else {
            Logger.info("Using previous game.server instance.");

            // necessary to set the static instance of GameConfig to the previous instance
            GameConfig.instance = (GameConfig) gameServer.configurations;
        }

        GameServer.setInstance(gameServer);

        gameServer.start();

    }

    private ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public void init() {
        // var boundedQueue = new ArrayBlockingQueue<Runnable>(1000);
        // new ThreadPoolExecutor(10, 20, 60, SECONDS, boundedQueue, new AbortPolicy());

        executorService = Executors.newFixedThreadPool(3);
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    private void openServerSocket() {
        try {
            this.serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress(configurations.getAddress(), configurations.getPort()));
            serverSocket.configureBlocking(false);
            Logger.info("Server started on port " + configurations.getPort());
        } catch (IOException e) {
            Logger.error("Could not open game.server socket on port " + configurations.getPort());
            System.exit(1);
        }
    }

    private void acceptConnections() throws IOException {
        Logger.info("Waiting for connections...");
        while (serverSocket.isOpen()) {
            SocketChannel socketChannel = serverSocket.accept();
            if (socketChannel != null) executorService.submit(new ClientHandler(socketChannel.socket()));
        }
    }


    private void start() throws IOException {
        init();
        openServerSocket();

        // Start the RMI registry
        int RMIPort = GameConfig.getInstance().getRMIReg();
        Registry registry = LocateRegistry.createRegistry(RMIPort);
        ScheduledRankRelaxer rankRelaxer;
        // Create an instance of the remote object and bind it to the registry
        playingServer = new PlayingServer();
        registry.rebind("playingServer", playingServer);

        ScheduledSerializer<GameServer> serializer = new ScheduledSerializer<>("cache/gameServer.ser", this, this.getScheduler());

        serializer.start();

        if (!configurations.getMode().equals("Simple")) {
            rankRelaxer = new ScheduledRankRelaxer(scheduler);
            rankRelaxer.start();
        }

        acceptConnections();

        // Add a shutdown hook to stop the serializer, to properly stop the ScheduledExecutorService
        Runtime.getRuntime().addShutdownHook(new Thread(serializer::stop));
    }

}
