package game.logic;

import game.SocketUtils;
import game.logic.structures.MyConcurrentList;
import game.protocols.CommunicationProtocol;
import game.server.PlayingServer;

import java.net.Socket;
import java.util.Objects;
import java.util.Random;

public class GameModel implements Runnable {

    private static final int NR_MIN_PLAYERS = 2;
    private static final int NR_MAX_PLAYERS = 2;
    private static final int MAX_GUESS = 100;
    private static final int MAX_NR_GUESS = 100;
    private int gameWinner = new Random().nextInt(MAX_GUESS);
    private MyConcurrentList<PlayingServer.WrappedPlayerSocket> gamePlayers;

    public GameModel(MyConcurrentList<PlayingServer.WrappedPlayerSocket> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public static int getNrMaxPlayers() {
        return NR_MAX_PLAYERS;
    }

    public void notifyPlayers(CommunicationProtocol protocol, String... args) {

        System.out.println("Notifying clients: " + protocol.name() + " | args = " + String.join(";", args));
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {
            System.out.println("Markito 1");
            Socket connection = gamePlayer.getConnection();
            if (connection.isConnected() && !connection.isClosed()) {
                System.out.println("Markito 2");
                SocketUtils.sendToClient(connection, protocol, args);
            } else {
                System.out.println("Markito 3");
                // TODO: este remove é melhor ser feito à parte, n é boa prática remover enquanto se itera
                gamePlayers.remove(gamePlayer); // todo: should they be removed?
                PlayingServer.games.updateHeap(this);
            }
        }
    }

    public void queueUpdate() {
        this.notifyPlayers(CommunicationProtocol.QUEUE_UPDATE, String.valueOf(this.gamePlayers.size()));

        // Check if there's a player in queue suitable for this game
        // Only for the ranking mode
        for (PlayingServer.WrappedPlayerSocket player : PlayingServer.queueToPlay) {
            if (player.getTolerance() >= Math.abs(this.getRank() - player.getRank())) {
                this.addPlayer(player);
                PlayingServer.queueToPlay.remove(player);
                break;
            } else {
                player.increaseTolerance();
            }
        }
    }

    public int getGameWinner() {
        return gameWinner;
    }

    private void gameLoop() {

        gameWinner = new Random().nextInt(10);

        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {
            Socket connection = gamePlayer.getConnection();
            int anwser = Integer.parseInt(Objects.requireNonNull(SocketUtils.NIORead(connection.getChannel(), null)));
            SocketUtils.sendToClient(connection, CommunicationProtocol.GUESS_TOO_LOW, String.valueOf(Math.abs(gameWinner - anwser)));
        }

        // TODO: LIA
    }

    public void endGame() {
        // notifyPlayers(CommunicationProtocol.GAME_END);
        // TODO: LIA
        notifyPlayers(CommunicationProtocol.GAME_END, String.valueOf(gameWinner));
        gamePlayers.clear();
        PlayingServer.games.updateHeap(this);
        System.out.println("Game ended");
        // TODO: ir buscar à queue os jogadores que estavam à espera e preenche-los aqui
        // se for simple mode preencher por ordem de chegada, senão fazer o modo rankeado
        // o gameconfig é um singleton e tem o modo de jogo definido
    }

    @Override
    public void run() {
        System.out.println("Game playground");
        // TODO: Add max timeout to the game

        notifyPlayers(CommunicationProtocol.GAME_STARTED);

        gameLoop();
        endGame();
    }

    public MyConcurrentList<PlayingServer.WrappedPlayerSocket> getGamePlayers() {
        return gamePlayers;
    }

    public void setGamePlayers(MyConcurrentList<PlayingServer.WrappedPlayerSocket> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public boolean isAvailable() {
        return gamePlayers.size() < NR_MAX_PLAYERS;
    }

    public void addPlayer(PlayingServer.WrappedPlayerSocket client) {
        gamePlayers.add(client);
        PlayingServer.games.updateHeap(this);
    }

    public boolean isFull() {
        return gamePlayers.size() == NR_MAX_PLAYERS;
    }

    public int getCurrentPlayers() {
        return gamePlayers.size();
    }

    public int getRank() {

        if (gamePlayers.size() == 0) return -1;

        // Game rank is the mean of the players' ranks
        int sum = 0;
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers)
            sum += gamePlayer.getRank();
        return sum / gamePlayers.size();
    }

}
