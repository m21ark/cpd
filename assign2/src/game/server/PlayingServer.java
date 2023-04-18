package game.server;

import game.client.GamePlayer;
import game.logic.GameModel;

import java.net.Socket;
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

    PlayingServer() throws RemoteException {
        super();
        executorGameService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            games.add(new GameModel(new ArrayList<>()));
        }
    }
    @Override
    public void queueGame(GamePlayer client, String token) throws RemoteException{
        // TODO: adicionar lock dos jogos (ver abaixo) e adicionar parte dos rankings
        // Neste momento fiz esta parte com rmi mas n fará muito sentido fazer assim
        // melhor seria chamar o metodo no handler do client. Se assim for teremos de adicionar um lock
        // nos jogos, acho que rmi aqui funciona bem mas n sei se o facto de ter de se fazer serealize n piora o resultado
        // esta parte do codigo parece ter de ser sequencial ... ????

        // RemoteRef ref = this.getRef();
        // ref.invoke
        System.out.println("Added player to queue");
        for (GameModel game : games) {
            if (game.isAvailable()) {
                game.addPlayer(new WrappedPlayerSocket(client, GameServer.getSocket(token)));
                if (game.isFull()) {
                    System.out.println("Game started");
                    executorGameService.submit(game);
                }
                return;
            }
        }
    }
}