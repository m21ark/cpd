package game.client;

import game.config.GameConfig;
import game.server.GameServerInterface;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class Client implements Serializable { // This is the client application runner.

    SocketChannel socketChannel;
    GamePlayer player;
    private String token;


    public Client(String s, int i) throws IOException {
        GameConfig config = new GameConfig();

        InetSocketAddress address = new InetSocketAddress(config.getAddress(), config.getPort());
        try {
            socketChannel = SocketChannel.open(address);
        } catch (Exception e) {
            System.out.println("Server is not running");
            System.exit(0);
        }
        player = new GamePlayer("Player", 0); // TODO... tem de se fazer a autenticaçãao e mudar isto
    }

    private static String processData(ByteBuffer buffer) {
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes);
    }

    public static String waitForGameStart(SocketChannel socketChannel) throws IOException {
        // Configure the socket channel to non-blocking mode
        socketChannel.configureBlocking(false);

        Selector selector = Selector.open();
        SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);

        while (true) {
            int readyChannels = selector.select();
            if (readyChannels == 0) {
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey selectionKey = keyIterator.next();

                if (selectionKey.isReadable()) {

                    if (selectionKey.isReadable()) {
                        // Read data from the channel
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = socketChannel.read(buffer);
                        if (bytesRead == -1) {
                            socketChannel.close();
                            continue;
                        }

                        // Process the data that was read
                        String data = processData(buffer);
                        System.out.println("Received data: " + data);
                    }

                    //if (selectionKey.isWritable()) {

                    // String message = "Hello, world!";
                    // ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                    // socketChannel.write(buffer);
//
                    //
                    // selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    //}
                }

                keyIterator.remove();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client("Player 1", 1);
        client.startGame();
    }

    public int options() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Please select an option:");
        System.out.println("------------------------");
        System.out.println("1 - Start a new game");
        System.out.println("2 - Exit");
        System.out.println("------------------------");

        try {
            return scanner.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid option!");
            return 0;
        }
    }

    private void sendGuess(String guess) throws IOException {
        // OutputStream output = socketChannel.socket().getOutputStream();
        // PrintWriter writer = new PrintWriter(output, true);
        // writer.println(guess); .... TODO: LIA ... o copilot sugeriu isto mas n testei
        System.out.println("Sending guess: " + guess);

    }

    public synchronized void startGame() throws IOException {
        System.out.println("Welcome to the game!");
        InputStream input = socketChannel.socket().getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        String token = reader.readLine();
        System.out.println("Your token is :" + token);
        this.token = token;

        int option = 0;
        while (option != 2) {
            option = this.options();
            if (option == 1) {
                this.playGame();
                waitForGameStart(socketChannel);
                System.out.println("Game Starting...!");
            }
        }
        socketChannel.close();
    }

    protected void playGame() {
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry("localhost", 1099);
            GameServerInterface gameServer = (GameServerInterface) registry.lookup("playingServer");
            gameServer.queueGame(this.player, token);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

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
