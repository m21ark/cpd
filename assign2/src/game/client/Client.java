package game.client;

import game.SocketUtils;
import game.config.GameConfig;
import game.server.GameServerInterface;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client implements Serializable { // This is the client application runner.

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
            System.out.println("Server is not running");
            System.exit(0);
        }

    }

    public Client(String s, int i) throws IOException {
        this(); // call the Default constructor
        player = new GamePlayer(s, i);
    }


    public static boolean dealWithServerMessages(String data) {
        if (data.startsWith("GAME_STARTED")) {
            System.out.println("Game started. Time to play!");
            return true;
        } else if (data.startsWith("QUEUE_UPDATE")) {
            String[] parts = data.split(" ");
            System.out.println("There are " + parts[1].split("\\n")[0] + " players in the game lobby.");
        }
        return false;
    }


    public static void waitForGameStart(SocketChannel socketChannel) throws IOException {
        String res = SocketUtils.NIORead(socketChannel, Client::dealWithServerMessages);
        System.out.println("Recieved: " + res);
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();

        // Authenticate
        if (client.authenticate()) {

            // Start game
            client.startGame();
        }
    }

    protected void playGame() {
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry("localhost", GameConfig.getInstance().getRMIReg());
            GameServerInterface gameServer = (GameServerInterface) registry.lookup("playingServer");
            gameServer.queueGame(this.player, token);
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    private boolean authenticate() {

        int serverResult;
        String username;

        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Username: ");
            username = scanner.nextLine().strip();
            System.out.print("Password: ");
            String password = scanner.nextLine().strip();

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
        } else answer = "CANCEL_NEW_USER";

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

        System.out.println("Rank " + rank);

        return code;
    }

    public int options() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("+-------------------------+");
        System.out.println("|    Please select an     |");
        System.out.println("|         option:         |");
        System.out.println("+-------------------------+");
        System.out.println("|   1 - Start a new game  |");
        System.out.println("|   2 - Exit              |");
        System.out.println("+-------------------------+");

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

                System.out.println("Do you want to play again? (y/n)");
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
        String msg = SocketUtils.extract(socketChannel);

        if (msg == null) {
            System.out.println("Message null!");
            return;
        }

        System.out.print("Your guess: ");
        int guess = (new Scanner(System.in)).nextInt();

        String response = sendGuess(String.valueOf(guess));

        System.out.println("Result: " + response);

        // TODO: Lia
    }

    private String sendGuess(String guess) {
        if (SocketUtils.NIOWrite(socketChannel, guess))
            return SocketUtils.NIORead(socketChannel, Client::dealWithServerMessages);
        return null;
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
