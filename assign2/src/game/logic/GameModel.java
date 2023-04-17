package game.logic;

import game.protocols.CommunicationProtocol;
import game.server.GameServer;
import game.server.PlayingServer;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class GameModel implements Runnable {
    private final int NR_MAX_PLAYERS = 2;
    private List<PlayingServer.WrappedPlayerSocket> gamePlayers;

    public GameModel(List<PlayingServer.WrappedPlayerSocket> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    @Override
    public void run() {
        System.out.println("Game playground");

        // notify clients to start game
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {
            Socket connection = gamePlayer.getConnection();
            try {
                GameServer.sendToClient(connection, CommunicationProtocol.GAME_STARTING);
            } catch (IOException e) {
                e.printStackTrace(); // client left the game or connection error
            }
        }

    }

    public List<PlayingServer.WrappedPlayerSocket> getGamePlayers() {
        return gamePlayers;
    }

    public void setGamePlayers(List<PlayingServer.WrappedPlayerSocket> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public boolean isAvailable() {
        return gamePlayers.size() < NR_MAX_PLAYERS;
    }

    public void addPlayer(PlayingServer.WrappedPlayerSocket client) {
        gamePlayers.add(client);
    }

    public boolean isFull() {
        return gamePlayers.size() == NR_MAX_PLAYERS;
    }
}
