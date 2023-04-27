package game.client;

import game.config.GameConfig;
import game.protocols.CommunicationProtocol;
import game.server.GameServerInterface;
import game.utils.Logger;
import game.utils.SocketUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client implements Serializable { // This is the client application runner.

    private static int NR_MAX_PLAYERS = -1;
    private static int MAX_GUESS = -1;
    private static int MAX_NR_GUESSES = -1;
    SocketChannel socketChannel;
    GamePlayer player;
    private String token;
    private int rank;

    public Client() throws IOException {
        GameConfig config = GameConfig.getInstance();

        InetSocketAddress address = new InetSocketAddress(config.getAddress(), config.getPort());

        try {
            socketChannel = SocketChannel.open(address);
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
        //Logger.setLevel(java.util.logging.Level.SEVERE);
        Client client = new Client();

        // Authenticate
        if (client.authenticate()) {

            // Start game
            client.startGame();
        }
    }

    private static boolean dealWithServerGuessResponse(String data) {
        boolean ret = false;
        if (data.contains(CommunicationProtocol.GUESS_TOO_LOW.name())) {
            System.out.println("Guess is too low!");
            ret = true;
        } if (data.contains(CommunicationProtocol.GUESS_TOO_HIGH.name())) {
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
        if (data.contains(CommunicationProtocol.PLAYER_LEFT.name())) {
            System.out.println("A Player left the game!");
            return ret;
        }
        if (!ret) Logger.error("Invalid response from server: " + data);
        return ret;
    }

    public void waitForGameStart(SocketChannel socketChannel) {
        SocketUtils.NIOReadAndInput(socketChannel, this::dealWithServerMessages, this::verifyUserWantToLeave);
    }

    public boolean dealWithServerMessages(String data) {
        if (data.contains("GAME_STARTED")) {
            System.out.println("Game started. Time to play!");
            String[] parts = data.split(" ");
            MAX_NR_GUESSES = Integer.parseInt(parts[1]);
            NR_MAX_PLAYERS = Integer.parseInt(parts[2]);
            MAX_GUESS = Integer.parseInt(parts[3]);
            System.out.println("You have " + MAX_NR_GUESSES + " guesses.");
            System.out.println("There are " + NR_MAX_PLAYERS + " players.");

            return true;
        }
        if (data.contains("QUEUE_UPDATE")) {
            String[] parts = data.split(" ");
            System.out.println("There are " + parts[1] + "/" + parts[2] + " players in the game lobby.");
        }
        if (data.contains("PLAYER_LEFT")) {
            System.out.println("A player left the game lobby.");
        }


        return false;
    }

    private void verifyUserWantToLeave() {
        // check for user input while waiting for server response
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

    protected void playGame() {
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(GameConfig.getInstance().getAddress(), GameConfig.getInstance().getRMIReg());
            GameServerInterface gameServer = (GameServerInterface) registry.lookup("playingServer");
            gameServer.queueGame(this.player, token);
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
            String password = scanner.nextLine().strip();
            System.out.println("+-------------------------+");


            if (username.equals("exit")) System.exit(0);

            if (username.isEmpty() || password.isEmpty()) System.out.println("Username and password are required!");
            else {
                serverResult = serverAuthenticate(username, password);
                break;
            }
        }

        switch (serverResult) {
            case 0 -> {
                if (registerUser()) this.player = new GamePlayer(username, rank);
                else return false;
                return true;
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

        // send answer to server
        SocketUtils.writeData(socketChannel, answer);

        if (answer.equals("CANCEL_NEW_USER")) return false;

        // final outcome from server's registering
        int ret = Integer.parseInt(SocketUtils.readData(socketChannel));

        if (ret == 1) {
            System.out.println("Registration successful!");
            return true;
        }

        System.out.println("Registration failed!");
        return false;
    }

    private int serverAuthenticate(String username, String password) {

        // send username and password to server
        SocketUtils.writeData(socketChannel, username + "," + password);

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

    public void startGame() throws IOException {
        System.out.println("Welcome to the game!");

        int option = 0;
        while (option != 2) {
            option = this.options();
            if (option == 1) {
                playGame();
                waitForGameStart(socketChannel);
                gameLoop();

                // Clear the input stream
                while (System.in.available() > 0) System.in.read();

                System.out.println("\nDo you want to play again? (y/n)");
                String answer = (new Scanner(System.in)).nextLine().strip().toLowerCase();
                if (answer.equals("y")) continue;
                System.out.println("Thanks for playing!");
                break;
            }
        }
        socketChannel.close();
    }

    public void getTokenFromServer() {
        this.token = SocketUtils.readData(socketChannel);
    }

    protected void gameLoop() {
        int numGuesses = MAX_NR_GUESSES;
        Scanner scanner = new Scanner(System.in);
        String serverResponse;

        while (numGuesses > 0) {
            System.out.println("Guess the number between 1 and " + MAX_GUESS + " (" + numGuesses + " guesses left): ");
            int guess = getIntegerInput();
            serverResponse = sendGuess(String.valueOf(guess));

            Logger.info(serverResponse);

            if (serverResponse.contains("GUESS_CORRECT")) {
                break;
            }
            numGuesses--;
        }

        if (numGuesses == 0) {
            System.out.println("You are out of guesses! Waiting for game to end...");
        }

        serverResponse = SocketUtils.NIORead(socketChannel, (data) -> {
            if (data.contains("GAME_END")) {
                System.out.println("The game ended! The correct number was " + data.split(" ")[1]);
                return true;
            }
            Logger.error("Invalid response from server: " + data);
            return false;
        });

        int finalNumGuesses = MAX_NR_GUESSES - numGuesses + 1;
        serverResponse = SocketUtils.NIORead(socketChannel, (data) -> {
            if (data.contains("GAME_RESULT")) {

                // Points , Position/Players
                String[] args = data.split(" ");
                if (Integer.parseInt(args[1]) > 0) System.out.println("You won!");
                else System.out.println("You lost!");
                System.out.println("Points: " + args[1] + " --> New Rank = " + (player.getRank() + Integer.parseInt(args[1])));
                System.out.println("Position: " + args[2] + "/" + args[3]);
                int delta = Integer.parseInt(args[4]) - Integer.parseInt(args[5]);
                System.out.println("Your closest guess: " + args[4] + " was off by " + Math.abs(delta) + " and took you " + (finalNumGuesses - 1) + " guesses");
                return true;
            }
            Logger.error("Invalid response from server: " + data);
            return false;
        });
    }

    public void informServerOfLogoutAndLeave(){
        /*
        try {
            System.out.println("Logging out at your request...");
            SocketUtils.NIOWrite(socketChannel, CommunicationProtocol.LOGOUT.name());
            this.socketChannel.close();
        } catch (IOException e) {
            System.out.println("Error closing socket channel at logout: " + e.getMessage());
        }*/
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
        SocketUtils.NIOWrite(socketChannel, String.valueOf(guess));
        return SocketUtils.NIORead(socketChannel, Client::dealWithServerGuessResponse);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public GamePlayer getPlayer() {
        return player;
    }

    public void setPlayer(GamePlayer player) {
        this.player = player;
    }
}
