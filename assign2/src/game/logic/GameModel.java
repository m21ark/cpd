package game.logic;

import game.logic.structures.MyConcurrentList;
import game.protocols.CommunicationProtocol;
import game.server.PlayingServer;
import game.utils.Logger;
import game.utils.SocketUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Random;

public class GameModel implements Runnable {

    private static final int NR_MIN_PLAYERS = 1;
    private static final int NR_MAX_PLAYERS = 1;
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

        Logger.info("Notifying clients: " + protocol.name() + " | args = " + String.join(";", args));
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {

            Socket connection = gamePlayer.getConnection();
            if (connection.isConnected() && !connection.isClosed()) {
                SocketUtils.sendToClient(connection, protocol, args);
            } else {

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

    public boolean responseToGuess() {
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {
            Socket connection = gamePlayer.getConnection();
            int guess = Integer.parseInt(Objects.requireNonNull(SocketUtils.NIORead(connection.getChannel(), null)));

            if (guess == gameWinner) {
                notifyPlayers(CommunicationProtocol.GUESS_CORRECT, String.valueOf(gameWinner));
                return true;
            } else if (guess > gameWinner) {
                SocketUtils.sendToClient(gamePlayer.getConnection(), CommunicationProtocol.GUESS_TOO_HIGH);
            } else {
                SocketUtils.sendToClient(gamePlayer.getConnection(), CommunicationProtocol.GUESS_TOO_LOW);
            }
        }
        return false;
    }

    private void gameLoop() {
        int nrGuesses = 0;
        while (nrGuesses < MAX_NR_GUESS) {
            if(responseToGuess()) break;
            nrGuesses++;
        }
    }

    public void endGame() {
        // TODO: LIA
        notifyPlayers(CommunicationProtocol.GAME_END, String.valueOf(gameWinner));
        gamePlayers.clear();
        PlayingServer.games.updateHeap(this);
        System.out.println("Game ended");
        // TODO: ir buscar à queue os jogadores que estavam à espera e preenche-los aqui
        // se for simple mode preencher por ordem de chegada, senão fazer o modo rankeado
        // o gameconfig é um singleton e tem o modo de jogo definido


        /*if(PlayingServer.gameConfig.getGameMode() == GameMode.RANKED) {
            queueUpdate();
        } else {
            queueUpdate();
        }*/

        //System.out.println("Your final score is " + (1000 - Math.abs(numberToGuess - closestGuess) - 1) + ".");
        
    }

    @Override
    public void run() {
        System.out.println("Game playground");
        // TODO: Add max timeout to the game

        if(gamePlayers.size() < NR_MIN_PLAYERS) {
            notifyPlayers(CommunicationProtocol.GAME_WAIT);
            return;
        }

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
