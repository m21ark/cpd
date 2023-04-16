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
                game.addPlayer(client);
                if (game.isFull()) {
                    executorGameService.submit(game);
                }
                return;
            }
        }
    }
}
