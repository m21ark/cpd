package game.server;

import game.client.GamePlayer;
import game.config.GameConfig;
import game.logic.GameHeap;
import game.logic.GameModel;
import game.protocols.CommunicationProtocol;

import java.io.IOException;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayingServer extends UnicastRemoteObject implements GameServerInterface {
    public static final GameHeap games = new GameHeap();
    private final ExecutorService executorGameService;
    public static Queue<WrappedPlayerSocket> queueToPlay = new LinkedList<>();

    PlayingServer() throws RemoteException {
        super();

        // TODO: é possivel permitir mais jogos, mesmo com o mesmo numero de threads ... por alguma razao o enunciado diz que tem de ser fixo
        executorGameService = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 500; i++) games.addGame(new GameModel(new ArrayList<>()));
    }
    private boolean rankMode(GamePlayer client, String token) {
        //TODO: implementar
        return false;
    }

    private boolean simpleMode(GamePlayer client, String token) {

        // this is a heap, so the first game is the one with the most players and available
        // this was done to improve performance as a simple list would require a linear search
        // if all games except one are full, a list would require a linear search to find the game available
        // now we just need to check the first game
        for (GameModel game : games) {
            if (game.isAvailable()) {
                game.addPlayer(new WrappedPlayerSocket(client, GameServer.getSocket(token)));
                if (game.isFull()) {
                    System.out.println("Game started");
                    executorGameService.submit(game);
                } else {
                    System.out.println("Waiting for more players ... " + game.getGamePlayers().size() + " / " + GameModel.getNrMaxPlayers());
                    game.notifyPlayers(CommunicationProtocol.QUEUE_UPDATE, String.valueOf(game.getGamePlayers().size()));
                    // TODO: adicionar timeout para o caso de n haver mais jogadores
                    // todo : talvez notificar os jogadores que estão na queue de quantos jogadores faltam (ETA)
                }
                return true;
            }
        }
        return false;
    }

    public void addToQueue(GamePlayer client, String token) {
        // TODO: add lock
        System.out.println("No games available, player will be set to a queue");
        // latter the game is responsible for removing the player from the queue and add it to the game
        // probably it should not be a queue has in rank mode the order is not that important and we dont want
        // to discard some players

        queueToPlay.add(new WrappedPlayerSocket(client, GameServer.getSocket(token)));
    }

    @Override
    public void queueGame(GamePlayer client, String token) throws RemoteException {

        // TODO: detetar se o player desistiu da queue
        System.out.println("Added player to queue");

        String mode = "";
        boolean gamesAvailable;
        try {
            mode = GameConfig.getInstance().getMode();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mode.equals("Simple")) {
            gamesAvailable = simpleMode(client, token);
        } else {
            gamesAvailable = rankMode(client, token);
        }

        if (!gamesAvailable) {
            addToQueue(client, token);
            // TODO: send message to client that he is in queue, waiting for games to end ... NOT priority
        }

    }

    public static class WrappedPlayerSocket extends GamePlayer {

        private final Socket connection;

        public WrappedPlayerSocket(GamePlayer client, Socket connection) {
            super(client.getName(), client.getRank());
            this.rank = client.getRank();
            this.score = client.getScore();
            this.connection = connection;
        }

        public GamePlayer getClient() {
            return this;
        }

        public Socket getConnection() {
            return connection;
        }
    }
}
