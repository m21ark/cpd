package game.server;

import game.client.Client;
import game.client.GamePlayer;
import game.logic.GameModel;

import java.rmi.RemoteException;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayingServer extends UnicastRemoteObject implements GameServerInterface{
    private final List<GameModel> games = new ArrayList<>();
    private final ExecutorService executorGameService;

    public class WrappedPlayerConnection extends GamePlayer {

        private final RemoteRef connection;
        public WrappedPlayerConnection(GamePlayer client, RemoteRef connection) {
            super(client.getName(), client.getRank());
            this.rank = client.getRank();
            this.score = client.getScore();
            this.connection = connection;
        }

        public GamePlayer getClient() {
            return this;
        }

        public RemoteRef getConnection() {
            return connection;
        }
    }

    PlayingServer() throws RemoteException {
        super();
        executorGameService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            games.add(new GameModel(new ArrayList<>()));
        }
    }
    @Override
    public void queueGame(GamePlayer client) throws RemoteException{
        // TODO: adicionar lock dos jogos e adicionar parte dos rankings
        // RemoteRef ref = this.getRef();
        // ref.invoke
        System.out.println("Added player to queue");
        for (GameModel game : games) {
            if (game.isAvailable()) {
                game.addPlayer(new WrappedPlayerConnection(client, this.getRef()));
                if (game.isFull()) {
                    System.out.println("Game started");
                    executorGameService.submit(game);
                }
                return;
            }
        }
    }
}
