package game.server;

import game.config.GameConfig;
import game.logic.structures.MyConcurrentList;
import game.logic.structures.Pair;
import game.protocols.CommunicationProtocol;
import game.protocols.GuessErgo;
import game.protocols.TokenState;
import game.utils.Logger;
import game.utils.SocketUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.*;

public class GameModel implements Runnable, java.io.Serializable {

    public static final int NR_MAX_PLAYERS = GameConfig.getInstance().getNrMaxPlayers();
    public static final int MAX_GUESS = GameConfig.getInstance().getMaxGuess();
    public static final int MAX_NR_GUESSES = GameConfig.getInstance().getMaxNrGuess();

    // the following field does not have concurrent access, so it does not need to be thread-safe
    private final HashMap<String, Pair<Integer, Integer>> playerGuesses = new HashMap<>(); // <token, <num_guesses_left, best_guess>>
    private final MyConcurrentList<PlayingServer.WrappedPlayerSocket> gamePlayers;
    private int gameWinner = new Random().nextInt(MAX_GUESS);
    private boolean gameStarted = false;

    public GameModel(MyConcurrentList<PlayingServer.WrappedPlayerSocket> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public static int getNrMaxPlayers() {
        return NR_MAX_PLAYERS;
    }

    public static int getMaxNrGuesses() {
        return MAX_NR_GUESSES;
    }

    private static Map<String, Pair<Integer, Integer>> getLeaderboardScores(List<String> Leaderboard) {
        int numUsers = Leaderboard.size();
        int middleIndex = numUsers / 2;
        Map<String, Pair<Integer, Integer>> userScores = new HashMap<>();

        if (numUsers == 1) {
            userScores.put(Leaderboard.get(0), new Pair<>(0, 1));
            return userScores;
        }

        if (numUsers == 2) {
            // Two users, give the first user a score of 2 and the second user a score of -1
            userScores.put(Leaderboard.get(0), new Pair<>(2, 1)); // <score, position>
            userScores.put(Leaderboard.get(1), new Pair<>(-1, 2));
            return userScores;
        }

        // Calculate the score for each user
        for (int i = 0; i < numUsers; i++) {
            int score;
            if (i < middleIndex) score = numUsers - middleIndex + i + 1;
            else score = middleIndex - (i - middleIndex) - 1;
            userScores.put(Leaderboard.get(i), new Pair<>(score, i + 1));
        }
        return userScores;
    }

    public PlayingServer.WrappedPlayerSocket getPlayer(String token) {
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers)
            if (gamePlayer.getToken().equals(token)) return gamePlayer;
        return null;
    }


    public void notifyPlayers(CommunicationProtocol protocol, String... args) {

        Logger.info("Notifying clients: " + protocol.name() + " | args = " + String.join(";", args));
        List<PlayingServer.WrappedPlayerSocket> toRemove = new ArrayList<>();

        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {
            Socket connection = gamePlayer.getConnection();

            if (connection.isConnected() && !connection.isClosed())
                SocketUtils.sendToClient(connection, protocol, args);
            else toRemove.add(gamePlayer);
        }

        for (PlayingServer.WrappedPlayerSocket gamePlayer : toRemove) {
            gamePlayers.remove(gamePlayer);
            Logger.warning("Removed player '" + gamePlayer.getName() + "' from the game.");
        }

        if (!toRemove.isEmpty()) PlayingServer.getInstance().games.updateHeap(this);
    }

    public void upadtePlayerSocket(String token, Socket newSocket) {
        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers)
            if (gamePlayer.getToken().equals(token)) {
                gamePlayer.setConnection(newSocket);
                break;
            }
    }

    public void queueUpdate() {
        this.notifyPlayers(CommunicationProtocol.QUEUE_UPDATE, String.valueOf(this.gamePlayers.size()), String.valueOf(NR_MAX_PLAYERS));

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
            playerGuesses.put(token, new Pair<>(MAX_NR_GUESSES - 1, guess));
            return;
        }

        // if the guess is better than the previous one, update it
        if (Math.abs(guess - gameWinner) < Math.abs(guesses.getSecond() - gameWinner))
            playerGuesses.put(token, new Pair<>(guesses.getFirst() - 1, guess));
        else playerGuesses.put(token, new Pair<>(guesses.getFirst() - 1, guesses.getSecond()));

    }

    public int getGuessesLeft(String token) {
        Pair<Integer, Integer> guesses = playerGuesses.get(token);
        return (guesses == null) ? MAX_NR_GUESSES : guesses.getFirst();
    }

    public int getBestGuess(String token) {
        Pair<Integer, Integer> guesses = playerGuesses.get(token);
        return (guesses == null) ? -1 : guesses.getSecond();
    }

    public int getGameWinner() {
        return gameWinner;
    }

    public GuessErgo responseToGuess(PlayingServer.WrappedPlayerSocket gamePlayer) {

        //while (guessesLeft(gamePlayer.getToken()) > 0) {
        Socket connection = gamePlayer.getConnection();
        if (connection.isClosed()) {
            return GuessErgo.ALREADY_LEFT_GAME;
        }
        String s = SocketUtils.NIORead(connection.getChannel(), null, 0L);
        if (s == null) {
            return GuessErgo.NOT_PLAYED;
        } else if (s.equals("DISCONNECTED")) {
            return GuessErgo.ALREADY_LEFT_GAME;
        }

        int guess = Integer.parseInt(Objects.requireNonNull(s));


        if (guess == gameWinner) {
            SocketUtils.sendToClient(gamePlayer.getConnection(), CommunicationProtocol.GUESS_CORRECT, String.valueOf(gameWinner));
            updateGuesses(gamePlayer.getToken(), gameWinner);
            return GuessErgo.WINNING_MOVE;
        }

        updateGuesses(gamePlayer.getToken(), guess);

        if (guess > gameWinner) {
            SocketUtils.sendToClient(gamePlayer.getConnection(), CommunicationProtocol.GUESS_TOO_HIGH);
        } else {
            SocketUtils.sendToClient(gamePlayer.getConnection(), CommunicationProtocol.GUESS_TOO_LOW);
        }
        // }
        return GuessErgo.PLAYED;
    }

    private void gameLoop() {

        Logger.info("The answer is " + gameWinner);

        int finishedPlayers = 0;
        long startTime = System.currentTimeMillis();
        int gameSize = gamePlayers.size();
        while (true) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime > GameConfig.getInstance().getGameTimeout()) {
                Logger.info("Game timed out!");
                break;
            }


            if (finishedPlayers == gameSize) break;
            for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {
                GuessErgo response;

                response = responseToGuess(gamePlayer);

                if (response == GuessErgo.WINNING_MOVE) {
                    finishedPlayers++;
                    gamePlayer.setLeftGame(true);
                } else if (response == GuessErgo.PLAYED) {
                    if (getGuessesLeft(gamePlayer.getToken()) == 0) {
                        finishedPlayers++;
                    }
                }
            }
        }


    }

    public void endGame() {

        notifyPlayers(CommunicationProtocol.GAME_END, String.valueOf(gameWinner));

        Map<String, Pair<Integer, Integer>> leaderboard = getLeaderboard();

        // Notify who won and who lost + update ranks
        int i = 0;
        for (String token : leaderboard.keySet()) {
            i++;
            PlayingServer.WrappedPlayerSocket player = getPlayer(token);

            if (player == null) {
                Logger.warning("Player with token " + token + " not found!");
                continue;
            }

            if (player.getConnection().isClosed()) continue;

            SocketUtils.sendToClient(player.getConnection(), CommunicationProtocol.GAME_RESULT, String.valueOf(leaderboard.get(token).getFirst()), String.valueOf(leaderboard.get(token).getSecond()), String.valueOf(leaderboard.size()), String.valueOf(playerGuesses.get(token).getSecond()), String.valueOf(gameWinner));
            int newRank = player.getRank() + leaderboard.get(token).getFirst();
            if (newRank < 0) newRank = 0;
            player.setRank(newRank);
            updateRank(player.getName(), newRank); // saving to file
            GameServer.getInstance().clientsStates.put(token, new TokenState());
        }

        gamePlayers.clear();
        PlayingServer.getInstance().games.updateHeap(this);
        Logger.warning("Game cleared");
        gameWinner = new Random().nextInt(MAX_GUESS);
        // TODO: ir buscar à queue os jogadores que estavam à espera e preenche-los aqui
        // se for simple mode preencher por ordem de chegada, senão fazer o modo rankeado
        // o gameconfig é um singleton e tem o modo de jogo definido
    }

    private void updateRank(String username, int rank) {
        Logger.info("Updating persistant rank for user " + username);
        // TODO: ADD LOCK HERE TO WRITE TO FILE
        try {

            RandomAccessFile raf = new RandomAccessFile("database/users.txt", "rw");
            Formatter formatter = new Formatter();

            // Read the file line by line
            String line;
            while ((line = raf.readLine()) != null) {

                // Split the line into its components
                String[] parts = line.split(",");

                // Check if the username matches
                if (parts[0].equals(username)) {
                    // Update the score
                    long pointer = raf.getFilePointer();
                    raf.seek(pointer - line.length() - 1);
                    String updatedLine = formatter.format("%s,%s,%s,%05d", parts[0], parts[1], parts[2], rank).toString();
                    raf.writeBytes(updatedLine);
                    break;
                }
            }

            raf.close();
            Logger.info("Updated persistant rank for user " + username);
        } catch (IOException e) {
            throw new RuntimeException("Error updating score in file.");
        }
    }


    @Override
    public void run() {
        Logger.info("Game playground");
        // TODO: Add max timeout to the game

        gameStarted = true;
        notifyPlayers(CommunicationProtocol.GAME_STARTED, String.valueOf(MAX_NR_GUESSES), String.valueOf(NR_MAX_PLAYERS), String.valueOf(MAX_GUESS));

        gameLoop();
        endGame();

        gameStarted = false;
    }

    public MyConcurrentList<PlayingServer.WrappedPlayerSocket> getGamePlayers() {
        return gamePlayers;
    }

    public boolean isAvailable() {
        return gamePlayers.size() < NR_MAX_PLAYERS;
    }

    public void addPlayer(PlayingServer.WrappedPlayerSocket client) {
        gamePlayers.add(client);
        PlayingServer.getInstance().games.updateHeap(this);
        GameServer.getInstance().clientsStates.put(client.getToken(), new TokenState(this));
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

    private Map<String, Pair<Integer, Integer>> getLeaderboard() {
        List<String> leaderboard = new ArrayList<>();

        // Sort descendant by guesses left and then by distance to the answer
        List<Map.Entry<String, Pair<Integer, Integer>>> sorted = new ArrayList<>(playerGuesses.entrySet());

        // <token, <num_guesses_left, best_guess>>
        sorted.sort((o1, o2) -> {
            int guessesLeft = o2.getValue().getFirst() - o1.getValue().getFirst();
            if (guessesLeft != 0) return guessesLeft;
            return Math.abs(o1.getValue().getSecond() - gameWinner) - Math.abs(o2.getValue().getSecond() - gameWinner);
        });

        // Add to leaderboard
        for (Map.Entry<String, Pair<Integer, Integer>> entry : sorted)
            leaderboard.add(entry.getKey()); // token list

        return getLeaderboardScores(leaderboard);
    }


    public void removePlayer(PlayingServer.WrappedPlayerSocket player) {
        gamePlayers.remove(player);
        PlayingServer.getInstance().games.updateHeap(this);
    }

    public void playerLeftNotify() {
        if (!gameStarted) {
            notifyPlayers(CommunicationProtocol.PLAYER_LEFT);
            return;
        }

        CommunicationProtocol protocol = CommunicationProtocol.PLAYER_LEFT;

        for (PlayingServer.WrappedPlayerSocket gamePlayer : gamePlayers) {

            Socket connection = gamePlayer.getConnection();
            if (connection.isConnected() && !connection.isClosed()) {
                SocketUtils.sendToClient(connection, protocol);
            }
        }

    }

    public boolean gameStarted() {
        return gameStarted;
    }
}
