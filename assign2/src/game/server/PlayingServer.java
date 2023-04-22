package game.server;

import game.client.GamePlayer;
import game.logic.GameModel;
import game.protocols.CommunicationProtocol;

import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayingServer extends UnicastRemoteObject implements GameServerInterface {
    private final List<GameModel> games = new ArrayList<>();
    private final ExecutorService executorGameService;
    public static Queue<WrappedPlayerSocket> queueToPlay;

    PlayingServer() throws RemoteException {
        super();

        // TODO: é possivel permitir mais jogos, mesmo com o mesmo numero de threads ... por alguma razao o enunciado diz que tem de ser fixo
        executorGameService = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; i++) games.add(new GameModel(new ArrayList<>()));
    }

    @Override
    public void queueGame(GamePlayer client, String token) throws RemoteException {

        // TODO: detetar se o player desistiu da queue
        System.out.println("Added player to queue");

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
                return;
            }

        }

        System.out.println("No games available, player will be set to a queue");
        // latter the game is responsible for removing the player from the queue and add it to the game
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
