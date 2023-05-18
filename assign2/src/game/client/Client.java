package game.client;

import game.config.Configurations;
import game.config.GameConfig;
import game.protocols.CommunicationProtocol;
import game.server.GameServerInterface;
import game.utils.Logger;
import game.utils.SocketUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Client implements Serializable { // This is the game.client application runner.

    private static int NR_MAX_PLAYERS = -1;
    private static int MAX_GUESS = -1;
    private static int MAX_NR_GUESSES = -1;
    SocketChannel socketChannel;
    GamePlayer player;
    private String password;
    private String token;
    private int rank;
    private int numGuesses;

    public Client() throws IOException {
        try {
            openConnection();
        } catch (Exception e) {
            Logger.error("Server is not running");
            System.exit(0);
        }

    }

    public Client(String s, int i) throws IOException {
        this(); // call the Default constructor
        player = new GamePlayer(s, i);
    }

    public static void main(String[] args) throws IOException {
        Configurations configurations;
        List<String> argsList = Arrays.stream(args).toList();

        if (argsList.contains("-d"))
            Logger.setLevel(java.util.logging.Level.SEVERE);

        Client client = new Client();

        // Authenticate
        if (client.authenticate()) {

            // persist token
            saveTokenToFile(client.getPlayer().getName(), client.token);

            // Start game
            client.startGame();
        }
    }

    private static void saveTokenToFile(String username, String token) {
        // persist token and rank in file
        try {
            Files.createDirectories(Paths.get("cache"));
            FileWriter fileWriter = new FileWriter("cache/player_" + username + ".txt");
            fileWriter.write(token);
            fileWriter.close();
        } catch (IOException e) {
            Logger.error("Error persisting token/rank to file.");
        }
    }

    private static boolean dealWithServerGuessResponse(String data) {
        boolean ret = false;
        if (data.contains(CommunicationProtocol.GUESS_TOO_LOW.name())) {
            System.out.println("Guess is too low!");
            ret = true;
        }
        if (data.contains(CommunicationProtocol.GUESS_TOO_HIGH.name())) {
            System.out.println("Guess is too high!");
            ret = true;
        } else if (data.contains(CommunicationProtocol.GUESS_CORRECT.name())) {
            System.out.println("Your guess is correct!");
            System.out.println("Waiting for other players to finish...");
            ret = true;
        } else if (data.contains(CommunicationProtocol.GAME_END.name())) {
            System.out.println("Game over!");
            ret = true;
        }

        if (data.contains(CommunicationProtocol.GAME_RECONNECT.toString()) || data.contains(CommunicationProtocol.QUEUE_UPDATE.toString()) || data.contains(CommunicationProtocol.MENU_CONNECT.toString())) {
            return ret;
        }

        if (data.contains(CommunicationProtocol.PLAYER_LEFT.name())) {
            System.out.println("A Player left the game!");
            return ret;
        }
        if (!ret) Logger.error("Invalid response from game.server: " + data);
        return ret;
    }

    public void openConnection() throws IOException {
        GameConfig config = GameConfig.getInstance();
        InetSocketAddress address = new InetSocketAddress(config.getAddress(), config.getPort());
        socketChannel = SocketChannel.open(address);
    }

    public void waitForGameStart() {
        try {
            SocketUtils.NIOReadAndInput(this.socketChannel, this::dealWithServerMessages
                    , this::verifyUserWantToLeave);
        } catch (Exception e) {
            System.out.println("The game.server is down (or bad connection).\n" +
                    "Try to connect again." +
                    "\nYou won't loose your progress in queue.");

            System.exit(0);
        }
    }

    public boolean dealWithServerMessages(String data) {
        if (data.contains(CommunicationProtocol.GAME_STARTED.toString())) {
            System.out.println("Game started. Time to play!");
            String[] parts = data.split(" ");
            MAX_NR_GUESSES = Integer.parseInt(parts[1]);
            NR_MAX_PLAYERS = Integer.parseInt(parts[2]);
            MAX_GUESS = Integer.parseInt(parts[3]);
            this.numGuesses = MAX_NR_GUESSES;
            System.out.println("There are " + NR_MAX_PLAYERS + " players.");
            System.out.println("You have " + MAX_NR_GUESSES + " guesses.");
            return true;
        }

        if (data.contains(CommunicationProtocol.PLAYGROUND_UPDATE.toString())) {
            String[] parts = data.split(" ");
            System.out.println("There are " + parts[1] + "/" + parts[2] + " players in the game lobby.");
            return false;
        } else if (data.contains(CommunicationProtocol.QUEUE_ADD.toString())) {
            System.out.println("Our server is currently on it's max capacity.\n" + "You will be added to a queue before joining the game lobby.");
            return false;
        }

        if (data.contains(CommunicationProtocol.PLAYER_LEFT.toString()))
            System.out.println("A player left the game lobby.");
        else Logger.warning("Invalid response from game.server: " + data);

        return false;
    }

    private void verifyUserWantToLeave() {
        // check for user input while waiting for game.server response
        // if user inputs "exit", close the socket and exit the program
        try {
            if (System.in.available() <= 0) return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine().toLowerCase().strip();

        if (line.equals("exit")) {
            informServerOfLogoutAndLeave();
        }
    }

    protected void askToPlayGame() {
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(GameConfig.getInstance().getAddress(), GameConfig.getInstance().getRMIReg());
            GameServerInterface gameServer = (GameServerInterface) registry.lookup("playingServer");
            gameServer.tryToJoinGame(this.player, token);
            System.out.println("Waiting for other players to join...");
            System.out.println("Type 'exit' to leave the game lobby.");
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    private boolean authenticate() {

        int serverResult;
        String username;

        while (true) {

            Scanner scanner = new Scanner(System.in);

            System.out.println("+-------------------------+");
            System.out.println("|      Login Menu         |");
            System.out.println("+-------------------------+");
            System.out.print("| Enter username: ");
            username = scanner.nextLine().strip();
            System.out.print("| Enter password: ");
            password = scanner.nextLine().strip();
            System.out.println("+-------------------------+");


            if (username.equals("exit")) System.exit(0);

            if (username.isEmpty() || password.isEmpty()) System.out.println("Username and password are required!");
            else {
                String tok = getTokenFromFile(username); // check if token exists in file
                serverResult = serverAuthenticate(username, password, tok);
                break;
            }
        }

        switch (serverResult) {
            case 0 -> {
                if (registerUser()) {
                    this.player = new GamePlayer(username, rank);
                    return true;
                }
                return false;
            }
            case 1 -> {
                System.out.println("Login successful!");
                this.player = new GamePlayer(username, rank);
            }
            case 2 -> System.out.println("Incorrect password.");
            default -> System.out.println("Login failed.");
        }

        return serverResult == 1;
    }

    private String getTokenFromFile(String username) {
        // check if token exists in file
        try {
            File file = new File("cache/player_" + username + ".txt");
            Scanner scanner = new Scanner(file);
            String token = scanner.nextLine();
            scanner.close();
            return token;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private boolean registerUser() {

        // Register
        Scanner scanner = new Scanner(System.in);

        System.out.println("Do you want to register? (y/n)");
        String answer = scanner.nextLine().strip().toLowerCase();

        if (answer.equals("y")) {
            System.out.print("Repeat Password: ");
            answer = scanner.nextLine().strip();
        } else {
            System.out.println("An account is needed to play. Shutting down...");
            answer = "CANCEL_NEW_USER";
        }

        // send answer to game.server
        SocketUtils.writeData(socketChannel, answer);

        if (answer.equals("CANCEL_NEW_USER")) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                System.out.println("Error closing socket channel.");
            }
            return false;
        }

        // final outcome from game.server's registering
        int ret = Integer.parseInt(SocketUtils.readData(socketChannel));

        if (ret == 1) {
            System.out.println("Registration successful!");
            return true;
        }

        System.out.println("Registration failed!");
        return false;
    }

    private int serverAuthenticate(String username, String password, String tok) {

        // send username and password to game.server
        SocketUtils.writeData(socketChannel, username + "," + password + "," + (tok == null ? "0" : tok));

        String data = SocketUtils.readData(socketChannel);
        int code = Integer.parseInt(data.split(",")[0]);
        this.token = data.split(",")[1];
        this.rank = Integer.parseInt(data.split(",")[2]);
        Logger.info("Token |" + token + "|");
        Logger.info("Rank " + rank);

        return code;
    }

    public int options() {
        Scanner scanner = new Scanner(System.in);

        int rank = this.player.getRank();
        String rankString = String.format("%3d", rank);

        String menuHeader = "+-------------------------+\n" + "|    Select an option     |\n" + "|      (Rank = " + rankString + ")       |\n" + "+-------------------------+\n" + "|   1 - Start a new game  |\n" + "|   2 - Exit              |\n" + "+-------------------------+";

        System.out.println(menuHeader);

        try {
            return scanner.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid option!");
            return 0;
        }
    }

    public void startGame() {

        StringBuilder sb = new StringBuilder();
        int res = checkIfReconnect(sb);
        if (res > 0) {
            switch (res) {
                case 1 -> gameReconnect(sb);
                case 2 -> queueReconnect();
                case 3 -> playgroundReconnect(sb);
                default -> {
                    System.out.println("Reconnect failed!");
                    System.exit(0);
                }
            }
            return;
        }  // else : no reconnect

        mainGame(false, false);
    }

    private void mainGame(boolean reconnect, boolean wasMidGame) {

        int option = 0;

        if (!reconnect) System.out.println("Welcome to the game!");
        else option = 1;

        while (option != 2) {
            if (!reconnect) option = options();
            if (option == 1) {
                if (!reconnect) askToPlayGame();
                if (!wasMidGame) waitForGameStart();

                gameLoop();

                reconnect = false;
                wasMidGame = false;
                if (!wantsToPlayAgain()) break;
            }
        }
        try {
            socketChannel.close();
        } catch (IOException e) {
            System.out.println("Error closing socket channel to close the game.");
        }
    }

    private boolean wantsToPlayAgain() {
        // Clear the input stream
        try {
            while (System.in.available() > 0) System.in.read();
            System.out.println("\nDo you want to play again? (y/n)");
            String answer = (new Scanner(System.in)).nextLine().strip().toLowerCase();
            if (answer.equals("y")) return true;
            System.out.println("Thanks for playing!");
            return false;
        } catch (IOException e) {
            Logger.error("Error reading input stream when asking to play again!");
        }
        return false;
    }

    private void playgroundReconnect(StringBuilder sb) {
        System.out.println("Reconnecting to playground...");

        String[] parts = sb.toString().split(" ");
        int numPlayers = Integer.parseInt(parts[1]);
        NR_MAX_PLAYERS = Integer.parseInt(parts[2]);

        System.out.println("Welcome back " + player.getName() + "!");
        System.out.println("There are " + numPlayers + "/" + NR_MAX_PLAYERS + " players in the playground.");

        mainGame(true, false);
    }

    private void queueReconnect() {
        System.out.println("Reconnecting to queue..."); // TODO: There are still problems with this and dont know why
        System.out.println("Welcome back " + player.getName() + "!");
        System.out.println("You are still in the queue...");
        mainGame(true, false);
    }

    private void gameReconnect(StringBuilder sb) {
        System.out.println("Reconnecting to game...");

        String[] parts = sb.toString().split(" ");
        MAX_NR_GUESSES = Integer.parseInt(parts[1]);
        NR_MAX_PLAYERS = Integer.parseInt(parts[2]);
        MAX_GUESS = Integer.parseInt(parts[3]);
        this.numGuesses = Integer.parseInt(parts[4]);
        int bestGuess = Integer.parseInt(parts[5]);
        String guessDirection = parts[6];

        System.out.println("Welcome back " + player.getName() + "!");
        System.out.println("There are " + NR_MAX_PLAYERS + " players.");
        System.out.println("You have " + numGuesses + " guesses left.");
        if (bestGuess != -1) {
            System.out.println("Your best guess so far is " + bestGuess + ".");
            System.out.println("The number is " + guessDirection + " than your best guess.");
        }

        mainGame(true, true);
    }

    private int checkIfReconnect(StringBuilder dataBuffer) {
        final int[] x = {-1};
        SocketUtils.NIORead(socketChannel, (data) -> {
            if (data.contains(CommunicationProtocol.MENU_CONNECT.toString())) x[0] = 0;
            else if (data.contains(CommunicationProtocol.GAME_RECONNECT.toString())) x[0] = 1;
            else if (data.contains(CommunicationProtocol.QUEUE_RECONNECT.toString())) x[0] = 2;
            else if (data.contains(CommunicationProtocol.PLAYGROUND_RECONNECT.toString())) x[0] = 3;
            dataBuffer.append(data);
            return true;
        });

        return x[0];
    }

    public void getTokenFromServer() {
        this.token = SocketUtils.readData(socketChannel);
    }

    public boolean tryToReconnect() {
        socketChannel = null;
        while (socketChannel == null) { // add timeout
            try {
                openConnection();
            } catch (IOException e) {
                socketChannel = null;
            }
        }
        serverAuthenticate(player.getName(), password, token);
        return true;
    }

    protected void gameLoop() {
        String serverResponse;

        while (numGuesses > 0) {
            if (checkIfGameTimeout()) break;

            System.out.println("Guess the number between 1 and " + MAX_GUESS + " (" + numGuesses + " guesses left): ");
            int guess = getIntegerInput();

            serverResponse = sendGuess(String.valueOf(guess));

            Logger.info(serverResponse);
            if (serverResponse.contains(CommunicationProtocol.GUESS_CORRECT.toString())) {
                break;
            }

            numGuesses--;
        }

        if (numGuesses == 0) {
            System.out.println("You are out of guesses! Waiting for game to end...");
        }

        gameEnd();
    }

    private void gameEnd() {

        SocketUtils.NIORead(socketChannel, (data) -> {
            if (data.contains(CommunicationProtocol.GAME_END.toString())) {
                System.out.println("The game ended!");
                return true;
            }
            Logger.error("Invalid response from game.server: " + data);
            return false;
        });

        int finalNumGuesses = MAX_NR_GUESSES - numGuesses + 1;
        SocketUtils.NIORead(socketChannel, (data) -> {
            if (data.contains(CommunicationProtocol.GAME_RESULT.toString())) {

                // Points , Position/Players
                String[] args = data.split(" ");
                int delta = Integer.parseInt(args[4]) - Integer.parseInt(args[5]);

                if (delta != 0) System.out.println("The correct number was " + Integer.parseInt(args[5]));
                else System.out.println("You guessed the correct number " + Integer.parseInt(args[5]));

                if (Integer.parseInt(args[1]) > 0 || delta == 0) System.out.println("You won!");
                else System.out.println("You lost!");

                System.out.println("Points: " + args[1] + " --> New Rank = " + (player.getRank() + Integer.parseInt(args[1])));
                System.out.println("Position: " + args[2] + "/" + args[3]);
                if (delta != 0)
                    System.out.println("Your closest guess: " + args[4] + " was off by " + Math.abs(delta) + " and took you " + (finalNumGuesses - 1) + " guesses");
                else System.out.println("It took you " + (finalNumGuesses - 1) + " guesses to win");
                return true;
            }
            Logger.error("Invalid response from game.server: " + data);
            return false;
        });
    }

    private boolean checkIfGameTimeout() {
        final boolean[] timedOut = {false};
        SocketUtils.NIORead(socketChannel, (data) -> {
            if (data.contains(CommunicationProtocol.GAME_TIMEOUT.toString())) {
                System.out.println("Game timed out!");
                timedOut[0] = true;
            }
            return true;
        }, 50L);
        return timedOut[0];
    }

    public void informServerOfLogoutAndLeave() {

        Registry registry;
        try {
            System.out.println("Logging out at your request...");
            registry = LocateRegistry.getRegistry(GameConfig.getInstance().getAddress(), GameConfig.getInstance().getRMIReg());
            GameServerInterface gameServer = (GameServerInterface) registry.lookup("playingServer");
            gameServer.logoutGame(this.player, token);
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private int getIntegerInput() {
        int guess;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                guess = scanner.nextInt();
                break;
            } catch (NumberFormatException | InputMismatchException e) {
                String input = scanner.nextLine().strip();
                if (input.equals("exit")) {
                    informServerOfLogoutAndLeave();
                }
                System.out.println("Invalid input!");
            }
        }
        return guess;
    }

    private String sendGuess(String guess) {
        SocketUtils.sendToServer(socketChannel, CommunicationProtocol.GUESS, String.valueOf(guess));
        String serverResponse = SocketUtils.NIORead(socketChannel, Client::dealWithServerGuessResponse);

        if (serverResponse.contains(CommunicationProtocol.DISCONNECTED.toString())) {
            if (!tryToReconnect()) {
                System.out.println("Server is in maintenance...");
                System.exit(0);
            } else {
                return sendGuess(guess);
            }
        }

        return serverResponse;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public GamePlayer getPlayer() {
        return player;
    }

}
