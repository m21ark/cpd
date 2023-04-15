package game.client;

import game.config.GameConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class Client {
    public static void main(String[] args) throws IOException {

        GameConfig config = new GameConfig();

        InetSocketAddress address = new InetSocketAddress(config.getAddress(), config.getPort());
        SocketChannel socketChannel = SocketChannel.open(address);


        socketChannel.close();
    }
}
