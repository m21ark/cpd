package game.logic;

import game.client.Client;
import game.client.GamePlayer;

import java.util.ArrayList;
import java.util.List;

public class GameModel implements Runnable {
    private final int NR_MAX_PLAYERS = 5;
    private List<GamePlayer> gamePlayers;

    public GameModel(List<GamePlayer> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    @Override
    public void run() {
        System.out.println("Game started");
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
