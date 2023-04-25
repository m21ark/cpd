package game.logic;

import game.logic.structures.MyConcurrentList;
import game.protocols.CommunicationProtocol;
import game.server.PlayingServer;
import game.utils.Logger;
import game.utils.SocketUtils;
import kotlin.Pair;

import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameModel implements Runnable {

    private static final int NR_MIN_PLAYERS = 2;
    private static final int NR_MAX_PLAYERS = 2;
    private static final int MAX_GUESS = 100;
    private static final int MAX_NR_GUESS = 100;
    private final int gameWinner = new Random().nextInt(MAX_GUESS);
    private final HashMap<String, Pair<Integer, Integer>> playerGuesses = new HashMap<>(); // <token, <num_guesses_left, best_guess>>
    private MyConcurrentList<PlayingServer.WrappedPlayerSocket> gamePlayers;

    public GameModel(MyConcurrentList<PlayingServer.WrappedPlayerSocket> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public static int getNrMaxPlayers() {
        return NR_MAX_PLAYERS;
    }

    public PlayingServer.WrappedPlayerSocket getPlayer(String token) {
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers)
            if (gamePlayer.getToken().equals(token)) return gamePlayer;
        return null;
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

    public void updateGuesses(String token, int guess) {
        Pair<Integer, Integer> guesses = playerGuesses.get(token);
        if (guesses == null) {
            playerGuesses.put(token, new Pair<>(MAX_NR_GUESS - 1, guess));
        } else {
            playerGuesses.put(token, new Pair<>(guesses.getFirst() - 1, guess));
        }
    }

    public int guessesLeft(String token) {
        Pair<Integer, Integer> guesses = playerGuesses.get(token);
        if (guesses == null) {
            return MAX_NR_GUESS;
        } else {
            return guesses.getFirst();
        }
    }

    public int getGameWinner() {
        return gameWinner;
    }

    public boolean responseToGuess(PlayingServer.WrappedPlayerSocket gamePlayer) {

        while (guessesLeft(gamePlayer.getToken()) > 0) {
            Socket connection = gamePlayer.getConnection();
            int guess = Integer.parseInt(Objects.requireNonNull(SocketUtils.NIORead(connection.getChannel(), null)));

            if (guess == gameWinner) {
                SocketUtils.sendToClient(gamePlayer.getConnection(), CommunicationProtocol.GUESS_CORRECT, String.valueOf(gameWinner));
                return true;
            }

            updateGuesses(gamePlayer.getToken(), guess);

            if (guess > gameWinner) {
                SocketUtils.sendToClient(gamePlayer.getConnection(), CommunicationProtocol.GUESS_TOO_HIGH);
            } else {
                SocketUtils.sendToClient(gamePlayer.getConnection(), CommunicationProtocol.GUESS_TOO_LOW);
            }
        }
        return false;
    }

    private void gameLoop() {

        Logger.info("The awnser is " + gameWinner);

        ExecutorService executor = Executors.newFixedThreadPool(gamePlayers.size());

        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {
            executor.execute(() -> {
                responseToGuess(gamePlayer);
            });
        }

        // wait for all threads to finish
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        // kill all threads
        executor.shutdownNow();
    }

    public void endGame() {

        notifyPlayers(CommunicationProtocol.GAME_END, String.valueOf(gameWinner));

        List<String> leaderboard = getLeaderboard();

        // Notify who won and who lost + update ranks
        int maxPoints = 5;
        int deltaPoints = 6; // Points to be subtracted from the winner for each player // TODO: ver melhor esquema de pontos
        int playerCount = 1;

        for (String pos : leaderboard) {
            String[] split = pos.split(":");
            String token = split[0];
            int rank = Integer.parseInt(split[1]);

            PlayingServer.WrappedPlayerSocket player = getPlayer(token);
            if (player == null) continue;

            SocketUtils.sendToClient(player.getConnection(), CommunicationProtocol.GAME_RESULT, String.valueOf(maxPoints), playerCount++ + "/" + gamePlayers.size());

            player.setRank(player.getRank() + maxPoints);
            maxPoints -= deltaPoints;
        }

        gamePlayers.clear();
        PlayingServer.games.updateHeap(this);
        System.out.println("Game cleared");
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

        if (gamePlayers.size() < NR_MIN_PLAYERS) {
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

    private List<String> getLeaderboard() {
        List<String> leaderboard = new ArrayList<>();
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {
            leaderboard.add(gamePlayer.getToken() + ":" + gamePlayer.getRank());
        }

        leaderboard.sort((o1, o2) -> {
            String[] split1 = o1.split(":");
            String[] split2 = o2.split(":");
            return Integer.parseInt(split2[1]) - Integer.parseInt(split1[1]);
        });

        return leaderboard;
    }


}
