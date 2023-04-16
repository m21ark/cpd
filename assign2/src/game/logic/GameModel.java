package game.logic;

import game.client.GamePlayer;
import game.server.PlayingServer;

import java.rmi.server.RemoteRef;
import java.util.List;

public class GameModel implements Runnable {
    private final int NR_MAX_PLAYERS = 2;
    private List<GamePlayer> gamePlayers;

    public GameModel(List<GamePlayer> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    @Override
    public void run() {
        System.out.println("Game playground");

        for (GamePlayer gamePlayer : gamePlayers) {
            System.out.println(gamePlayer.getName());
            RemoteRef connection = ((PlayingServer.WrappedPlayerConnection) gamePlayer).getConnection();
            System.out.println("connection" + connection);
            connection.notify();
            gamePlayer.notify(); // TODO: not working because it is not a remote object
        }

    }

    public List<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public void setGamePlayers(List<GamePlayer> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public boolean isAvailable() {
        return gamePlayers.size() < NR_MAX_PLAYERS;
    }

    public void addPlayer(GamePlayer client) {
        gamePlayers.add(client);
    }

    public boolean isFull() {
        return gamePlayers.size() == NR_MAX_PLAYERS;
    }
}
