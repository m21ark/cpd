package game.server;

import game.client.GamePlayer;
import game.config.GameConfig;
import game.logic.GameModel;
import game.logic.structures.GameHeap;
import game.logic.structures.MyConcurrentList;
import game.protocols.CommunicationProtocol;
import game.utils.Logger;
import game.utils.SocketUtils;

import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayingServer extends UnicastRemoteObject implements GameServerInterface {
    public static final GameHeap games = new GameHeap();
    public static MyConcurrentList<WrappedPlayerSocket> queueToPlay = new MyConcurrentList<>();
    private final ExecutorService executorGameService;

    PlayingServer() throws RemoteException {
        super();

        // TODO: é possivel permitir mais jogos, mesmo com o mesmo numero de threads ... por alguma razao o enunciado diz que tem de ser fixo
        executorGameService = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; i++) games.addGame(new GameModel(new MyConcurrentList<>()));
    }

    private boolean rankMode(GamePlayer client, String token) {

        // This mode uses the player's rank to determine the order of the players in the game

        int rankDelta = GameConfig.getInstance().getBaseRankDelta();
        GameModel game = null;

        // try to find a game with the given rank tolerance
        // TODO: ver o que acontece quando um com mais de 500 n entra no primeiro jogo

        game = games.getGameWithClosestRank(client.getRank(), rankDelta);


        if (game == null) {
            // if the tolerance is too big, just add the player to the queue
            // the player will be added to a game when a game with the given tolerance is available
            rankDelta *= 2; // increase the tolerance
            var player = new WrappedPlayerSocket(client, GameServer.getSocket(token), rankDelta);
            player.setToken(token);
            queueToPlay.add(player);
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
            game.queueUpdate(); // check if a ranked player waiting can enter this updated game
            //TODO: adicionar timeout para o caso de n haver mais jogadores
            //todo : talvez notificar os jogadores que estão na queue de quantos jogadores faltam (ETA)
        }

        return true;
    }

    private boolean simpleMode(GamePlayer client, String token) {

        // this is a heap, so the first game is the one with the most players and available
        // this was done to improve performance as a simple list would require a linear search
        // if all games except one are full, a list would require a linear search to find the game available
        // now we just need to check the first game
        for (GameModel game : games) {

            // Check if the players are still connected to the server
            if (!checkIfPlayersAreStillConnected(game)) notifyQueueUpdate(game);

            if (game.isAvailable()) {
                var player = new WrappedPlayerSocket(client, GameServer.getSocket(token));
                player.setToken(token);
                game.addPlayer(player);
                if (game.isFull()) {

                    Logger.info("Game started");
                    executorGameService.submit(game);
                } else {
                    Logger.info("Waiting for more players ... " + game.getGamePlayers().size() + " / " + GameModel.getNrMaxPlayers());
                    notifyQueueUpdate(game);
                    //TODO: adicionar timeout para o caso de n haver mais jogadores
                    //todo : talvez notificar os jogadores que estão na queue de quantos jogadores faltam (ETA)
                }
                return true;
            }
            // else {
            //     return false;
            // }
        }
        return false;
    }

    private void notifyQueueUpdate(GameModel game) {
        game.notifyPlayers(CommunicationProtocol.QUEUE_UPDATE, String.valueOf(game.getGamePlayers().size()), String.valueOf(GameConfig.getInstance().getNrMaxPlayers()));
    }

    private boolean checkIfPlayersAreStillConnected(GameModel game) {
        for (WrappedPlayerSocket player : game.getGamePlayers()) {

            // Check if they logged out or lost connection
            try {
                String s = SocketUtils.NIORead(player.getConnection().getChannel(), (String x) -> {
                    if (x.contains(CommunicationProtocol.LOGOUT.name())) {
                        Logger.warning("Player logged out of the game");
                        game.removePlayer(player);
                    }
                    return true;
                }, 100L); // TODO: timeout number ?
            } catch (Exception e) {
                Logger.warning("Player " + player.getToken() + " lost connection");
                return false;
            }
        }
        return true;
    }

    public void addToQueue(GamePlayer client, String token) {
        Logger.warning("No games available, player will be set to a queue");
        // latter the game is responsible for removing the player from the queue and add it to the game
        // probably it should not be a queue has in rank mode the order is not that important, and we don't want
        // to discard some players

        var player = new WrappedPlayerSocket(client, GameServer.getSocket(token));
        player.setToken(token);
        queueToPlay.add(player);
    }

    @Override
    public void queueGame(GamePlayer client, String token) throws RemoteException {

        // TODO: detetar se o player desistiu da queue
        Logger.info("Added player to queue");

        boolean gamesAvailable;
        String mode = GameConfig.getInstance().getMode();

        if (mode.equals("Simple")) gamesAvailable = simpleMode(client, token);
        else gamesAvailable = rankMode(client, token);

        if (!gamesAvailable) {
            addToQueue(client, token);
            // TODO: send message to client that he is in queue, waiting for games to end ... NOT priority
        }

    }

    public static class WrappedPlayerSocket extends GamePlayer {

        private final Socket connection;
        private int tolerance;

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

        public int getTolerance() {
            return tolerance;
        }

        public void increaseTolerance() {
            this.tolerance *= 2;
        }
    }
}
