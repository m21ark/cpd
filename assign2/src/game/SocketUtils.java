package game;

import game.protocols.CommunicationProtocol;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;


public class SocketUtils {
    public static String processData(ByteBuffer buffer) {
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes);
    }

    public static ByteBuffer prepareData(String data) {
        return ByteBuffer.wrap(data.getBytes());
    }

    public static String readData(SocketChannel socket) {
        return readData(socket.socket());
    }

    public static void writeData(SocketChannel socket, String data) {
        writeData(socket.socket(), data);
    }

    public static String readData(Socket socket) {
        try {
            System.out.println("Reading from socket...");
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String ret = reader.readLine();
            System.out.println("Read from socket: " + ret);
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error reading from socket: " + e.getMessage());
            return null;
        }
    }

    public static void writeData(Socket socket, String data) {
        try {
            System.out.println("Writing to socket: " + data);
            OutputStream output = new BufferedOutputStream(socket.getOutputStream());
            output.write((data + "\n").getBytes(StandardCharsets.UTF_8));
            output.flush();
            System.out.println("Wrote to socket: " + data);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error writing to socket: " + e.getMessage());
        }
    }

    public static void sendToClient(Socket connection, CommunicationProtocol message, String... args) {
        SocketUtils.writeData(connection, message.toString() + " " + String.join(" ", args));
    }

    public static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error closing socket!");
        }
    }

    public static String extract(SocketChannel socketChannel) {
        try {
            // Read data from the channel
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = socketChannel.read(buffer);

            if (bytesRead == -1) {
                socketChannel.close();
                return null;
            }

            // Process the data that was read
            return SocketUtils.processData(buffer);
        } catch (IOException e) {
            System.out.println("Error reading from socket: " + e.getMessage());
            return null;
        }
    }

    public static String NIORead(SocketChannel socketChannel, IntPredicate dealFunc) {

        // register a SocketChannel for reading data asynchronously (non-blocking)
        try {
            socketChannel.configureBlocking(false);

            Selector selector = Selector.open();
            SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);

            while (true) {
                // Wait for a channel to be ready for reading
                int readyChannels = selector.select();
                if (readyChannels == 0) continue;

                // TODO: add timeout
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();

                    if (selectionKey.isReadable()) {
                        // Read data from the channel
                        String data = SocketUtils.extract(socketChannel);
                        if (data == null) break;
                        if (dealFunc.func(data)) return data;
                    }
                    keyIterator.remove();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean NIOWrite(SocketChannel socketChannel, String data) {

        // register a SocketChannel for writing data asynchronously (non-blocking)
        try {
            socketChannel.configureBlocking(false);

            Selector selector = Selector.open();
            SelectionKey key = socketChannel.register(selector, SelectionKey.OP_WRITE);

            ByteBuffer buffer = ByteBuffer.wrap(data.getBytes());
            int bytesWritten = 0;

            while (buffer.hasRemaining()) {
                // Wait for a channel to be ready for writing
                int readyChannels = selector.select();
                if (readyChannels == 0) continue;

                // TODO: add timeout
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();

                    if (selectionKey.isWritable()) {
                        // Write data to the channel
                        bytesWritten += socketChannel.write(buffer);
                    }
                    keyIterator.remove();
                }
            }

            return bytesWritten == data.getBytes().length;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public interface IntPredicate {
        boolean func(String n);
    }

}