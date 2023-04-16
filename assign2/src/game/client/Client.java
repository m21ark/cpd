package game.client;

import game.config.GameConfig;
import game.protocols.CommunicationProtocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client { // This is the client application runner.

    SocketChannel socketChannel;


    Client() throws IOException {
        GameConfig config = new GameConfig();

        InetSocketAddress address = new InetSocketAddress(config.getAddress(), config.getPort());
        socketChannel = SocketChannel.open(address);
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
        }
        catch (Exception e) {
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
                this.playGame();
            }
        }
        socketChannel.close();
    }

    private void playGame() {
        ByteBuffer buffer = ByteBuffer.wrap(CommunicationProtocol.NEW_GAME.toString().getBytes());
        try {
            int bytesWritten = socketChannel.write(buffer);

            if (bytesWritten == -1) {
                socketChannel.close();
                return;
            }else if (bytesWritten == 0){
                Selector selector = Selector.open();
                socketChannel.register(selector, SelectionKey.OP_WRITE);
                selector.select();
                selector.close();
            }else {
                System.out.println("Game started!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.startGame();
    }
}
