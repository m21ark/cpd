package game.server;

import game.client.GamePlayer;
import game.config.GameConfig;
import game.logic.structures.GameHeap;
import game.logic.structures.MyConcurrentList;
import game.protocols.CommunicationProtocol;
import game.protocols.TokenState;
import game.utils.Logger;
import game.utils.SocketUtils;

import java.io.IOException;
import java.io.Serial;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class PlayingServer extends UnicastRemoteObject implements GameServerInterface {

    public MyConcurrentList<WrappedPlayerSocket> queueToPlay = new MyConcurrentList<>();
    static PlayingServer instance = null;
    public final GameHeap games = new GameHeap();
    private static ExecutorService executorGameService;

    PlayingServer() throws RemoteException {
        super();
        instance = this;

        int poolSize = GameConfig.getInstance().getGamePoolSize();
        executorGameService = Executors.newFixedThreadPool(poolSize);

        for (int i = 0; i < poolSize; i++) games.addGame(new GameModel(new MyConcurrentList<>()));
    }

    static public PlayingServer getInstance() {
        return instance;
    }

    public static void setExecutor(ExecutorService newFixedThreadPool) {
        executorGameService = newFixedThreadPool;
    }

    public void restartGames() {
        // restart the games ... after failure
        for (GameModel game : games) {
            if (game.gameStarted()) {
                Logger.info("Game has been restarted after game.server failure");
                executorGameService.submit(game);
            }
        }
    }

    private boolean rankMode(GamePlayer client, String token) {

        // This mode uses the player's rank to determine the order of the players in the game

        int rankDelta = GameConfig.getInstance().getBaseRankDelta();
        GameModel game;

        // try to find a game with the given rank tolerance
        // TODO: ver o que acontece quando um com mais de 500 n entra no primeiro jogo

        game = games.getGameWithClosestRank(client.getRank(), rankDelta);


        if (game == null) {
            // if the tolerance is too big, just add the player to the queue
            // the player will be added to a game when a game with the given tolerance is available
            rankDelta *= 2; // increase the tolerance
            var player = new WrappedPlayerSocket(client, GameServer.getSocket(token), rankDelta);
            player.setToken(token);
            Logger.warning("Player added to queue due to rank tolerance");
            return false;
        }

        var player = new WrappedPlayerSocket(client, GameServer.getSocket(token));
        player.setToken(token);
        game.addPlayer(player);

        if (game.isFull()) {
            Logger.info("Game started");
            executorGameService.submit(game);
        } else {
            Logger.info("Waiting for more players ... " + game.getGamePlayers().size() + " / " + GameModel.getNrMaxPlayers());
            boolean hasEnoughPlayer =  game.playgroundUpdate(); // check if a ranked player waiting can enter this updated game
            if (hasEnoughPlayer) {
                Logger.info("Game started");
                executorGameService.submit(game);
            } else {
                notifyPlaygroundUpdate(game);
            }
        }

        return true;
    }

    private boolean simpleMode(GamePlayer client, String token) {

        // this is a heap, so the first game is the one with the most players and available
        // this was done to improve performance as a simple list would require a linear search
        // if all games except one are full, a list would require a linear search to find the game available
        // now we just need to check the first game
        for (GameModel game : games) {

            if (game.isAvailable()) {
                var player = new WrappedPlayerSocket(client, GameServer.getSocket(token));
                player.setToken(token);
                game.addPlayer(player);
                if (game.isFull()) {
                    Logger.info("Game started");
                    executorGameService.submit(game);
                } else {
                    Logger.info("Waiting for more players ... " + game.getGamePlayers().size() + " / " + GameModel.getNrMaxPlayers());
                    notifyPlaygroundUpdate(game);
                    //TODO: adicionar timeout para o caso de n haver mais jogadores
                    //todo : talvez notificar os jogadores que estão na queue de quantos jogadores faltam (ETA)
                }
                return true;
            } else {
                break;
            }
        }
        return false;
    }

    public void notifyPlaygroundUpdate(GameModel game) {
        game.notifyPlayers(CommunicationProtocol.PLAYGROUND_UPDATE,
                String.valueOf(game.getGamePlayers().size()), String.valueOf(GameConfig.getInstance().getNrMaxPlayers()));
    }

    public void addToQueue(GamePlayer client, String token) {
        Logger.warning("No games available, player will be set to a queue");
        // latter the game is responsible for removing the player from the queue and add it to the game
        // probably it should not be a queue has in rank mode the order is not that important, and we don't want
        // to discard some players

        Socket s = GameServer.getSocket(token);
        var player = new WrappedPlayerSocket(client, s);
        player.setToken(token);
        queueToPlay.add(player);

        // Informing player that he was sent to queue
        SocketUtils.NIOWrite(s.getChannel(), CommunicationProtocol.QUEUE_ADD.toString());
    }

    @Override
    public void tryToJoinGame(GamePlayer client, String token) throws RemoteException {

        boolean gamesAvailable;
        String mode = GameConfig.getInstance().getMode();

        if (mode.equals("Simple")) gamesAvailable = simpleMode(client, token);
        else gamesAvailable = rankMode(client, token);

        if (!gamesAvailable) {
            addToQueue(client, token);
            GameServer.getInstance().clientsStates.put(token, new TokenState(null, TokenState.TokenStateEnum.QUEUED));
        }
    }

    @Override
    public void logoutGame(GamePlayer gamePlayer, String token) throws RemoteException {
        Socket socket = GameServer.getSocket(token);

        TokenState tokenState = GameServer.getInstance().clientsStates.get(token);

        Logger.info("Player " + token + " left the game" + " " + tokenState.getState().toString());
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (tokenState.getState() == TokenState.TokenStateEnum.PLAYING
                    || tokenState.getState() == TokenState.TokenStateEnum.PLAYGROUND) {
                tokenState.getModel().playerLeftNotify();
                tokenState.getModel().removePlayer();
                Logger.info("removed player from game");
            if (tokenState.getState() == TokenState.TokenStateEnum.PLAYGROUND) {
                tokenState.getModel().playerLeftNotify();
                Logger.info("removed player from game");
            }
            } else if (tokenState.getState() == TokenState.TokenStateEnum.QUEUED) {
                queueToPlay.removeWhere(x -> x.getToken().equals(token));
            }

            GameServer.getInstance().clientsStates.remove(token);
        }

    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();


    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        int poolSize = GameConfig.getInstance().getGamePoolSize();

        executorGameService = Executors.newFixedThreadPool(poolSize);

        System.out.println(games.getSize());

        this.restartGames();

    }

    public static class WrappedPlayerSocket extends GamePlayer {

        ReentrantLock lock = new ReentrantLock();
        private transient Socket connection;
        private int tolerance;
        private boolean leftGame = false;
        private String token;

        public WrappedPlayerSocket(GamePlayer client, Socket connection) {
            super(client.getName(), client.getRank());
            this.rank = client.getRank();
            this.score = client.getScore();
            this.connection = connection;
            this.tolerance = 1000;
        }

        public WrappedPlayerSocket(GamePlayer client, Socket connection, int tolerance) {
            super(client.getName(), client.getRank());
            this.rank = client.getRank();
            this.score = client.getScore();
            this.connection = connection;
            this.tolerance = tolerance;
        }

        public boolean hasLeftGame() {
            return leftGame;
        }

        public void setLeftGame(boolean leftGame) {
            this.leftGame = leftGame;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public GamePlayer getClient() {
            return this;
        }

        public Socket getConnection() {
            return connection;
        }

        public void setConnection(Socket newSocket) {
            connection = newSocket;
        }

        public int getTolerance() {
            return tolerance;
        }

        public void increaseTolerance() {
            this.tolerance *= 2;
        }
    }
}
