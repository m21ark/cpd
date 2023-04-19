package game.logic;

import game.SocketUtils;
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

    public void notifyPlayers(CommunicationProtocol protocol, String... args) {
        // TODO: podemos meter que se n estiver disponível perde a vez e entra outro

        System.out.println("Notifying clients: " + protocol.name());
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {
            Socket connection = gamePlayer.getConnection();
            if (connection.isConnected()){
                SocketUtils.sendToClient(connection, protocol, args);
            }else{
                gamePlayers.remove(gamePlayer); // todo should they be removed?
            }
        }
    }

    public int getGameWinner() {
        return gameWinner;
    }

    private void gameLoop() {

        // TODO: LIA
    }

    public void endGame(){
        // notifyPlayers(CommunicationProtocol.GAME_END);
        // TODO: LIA
    }

    @Override
    public void run() {
        System.out.println("Game playground");
        // TODO: Add max timeout to the game

        notifyPlayers(CommunicationProtocol.GAME_STARTED);

        gameLoop();

        endGame();
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
