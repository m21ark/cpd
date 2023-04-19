package game.client;

import game.SocketUtils;
import game.config.GameConfig;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class Client implements Serializable { // This is the client application runner.

    SocketChannel socketChannel;
    GamePlayer player;
    private String token;


    public Client() throws IOException {
        GameConfig config = new GameConfig();

        InetSocketAddress address = new InetSocketAddress(config.getAddress(), config.getPort());

        try {
            socketChannel = SocketChannel.open(address);
        } catch (Exception e) {
            System.out.println("Server is not running");
            System.exit(0);
        }

    }

    public static void waitForGameStart(SocketChannel socketChannel) throws IOException {

        // register a SocketChannel for reading data asynchronously (non-blocking)
        socketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ); // registered with the Selector for read events

        System.out.println("Waiting for game to start ...");
        while (true) {

            int readyChannels = selector.select(); // await for events (blocking)

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey selectionKey = keyIterator.next();

                if (selectionKey.isReadable()) {

                    String data = SocketUtils.extract(socketChannel);
                    if (data == null) break;
                    System.out.println("Received data: " + data);


                    // if (selectionKey.isWritable()) {

                    // String message = "Hello, world!";
                    // ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                    // socketChannel.write(buffer);

                    //
                    // selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    //}
                }

                keyIterator.remove();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();

        // Authenticate
        if (client.authenticate()) {

            // Start game
            client.startGame();
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
                return registerUser();
            }
            case 1 -> System.out.println("Login successful!");
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
        SocketUtils.writeData(socketChannel, username);
        SocketUtils.writeData(socketChannel, password);

        // receive result from server
        return Integer.parseInt(SocketUtils.readData(socketChannel));
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

    public synchronized void startGame() throws IOException {
        System.out.println("Welcome to the game!");
        System.out.println("Waiting for token...");
        String token = SocketUtils.readData(socketChannel);
        System.out.println("Your token is :" + token);

        this.token = token;
        this.player = new GamePlayer(this.token, 0);

        int option = 0;
        while (option != 2) {
            option = this.options();
            if (option == 1) {
                waitForGameStart(socketChannel);
                System.out.println("Game Starting...!");
                playGame();
            }
        }
        socketChannel.close();
    }

    protected void playGame() {
        String msg = SocketUtils.extract(socketChannel);

        do {
            if (msg == null) {
                System.out.println("There was an error while playing the game.");
                return;
            }
            System.out.println("Received game data: " + msg);
        } while (!msg.equals("GAME_STARTED"));

        System.out.println("Game started! Let's play!");
        // TODO: Lia
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
