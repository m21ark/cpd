package game.logic;

import game.client.SocketUtils;
import game.protocols.CommunicationProtocol;
import game.server.PlayingServer;

import java.net.Socket;
import java.util.List;
import java.util.Random;

public class GameModel implements Runnable {

    private static final int NR_MIN_PLAYERS = 2;
    private static final int NR_MAX_PLAYERS = 2;
    private static final int MAX_GUESS = 100;
    private static final int MAX_NR_GUESS = 100;
    private final int gameWinner = new Random().nextInt(MAX_GUESS);
    private List<PlayingServer.WrappedPlayerSocket> gamePlayers;

    public GameModel(List<PlayingServer.WrappedPlayerSocket> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public static int getNrMaxPlayers() {
        return NR_MAX_PLAYERS;
    }

    private void notifyPlayers() {
        // notify clients to start game ...
        // TODO: podemos meter que se n estiver disponível perde a vez e entra outro
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {
            Socket connection = gamePlayer.getConnection();
            SocketUtils.sendToClient(connection, CommunicationProtocol.GAME_STARTING);
        }
    }

    public int getGameWinner() {
        return gameWinner;
    }

    private void gameLoop() {
        // TODO: LIA
    }

    @Override
    public void run() {
        System.out.println("Game playground");
        // TODO: Add a maximum time for the game to end

        notifyPlayers();

        gameLoop();

        // TODO: LIA : implement game logic

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
