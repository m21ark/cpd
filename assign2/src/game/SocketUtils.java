package game;

import game.protocols.CommunicationProtocol;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;


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
        SocketUtils.NIOWrite(connection.getChannel(), message.toString() + " " + String.join(" ", args));
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

    public static String NIORead(SocketChannel channel, IntPredicate dealFunc) {
        // register a SocketChannel for reading data asynchronously (non-blocking)
        try {
            // Configure the channel to be non-blocking and register it with the selector for reading
            Selector selector = Selector.open();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            StringBuilder sb = new StringBuilder();

            while (true) {
                System.out.println("Waiting for data...");
                buffer.clear();
                selector.select();

                // Handle read operation
                int numBytesRead = channel.read(buffer);
                if (numBytesRead == -1) throw new IOException("Error reading from channel");
                if (numBytesRead == 0) {
                    // No data available at the moment, wait a bit and try again
                    Thread.sleep(100);
                    continue;
                }

                String msg = new String(buffer.array(), 0, numBytesRead);
                System.out.println("Read from channel: |" + msg + "|");
                if (dealFunc == null) return msg;
                if (dealFunc.func(msg)) return msg;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean NIOWrite(SocketChannel channel, String data) {
        // register a SocketChannel for writing data asynchronously (non-blocking)
        try {
            // Configure the channel to be non-blocking and register it with the selector for writing
            Selector selector = Selector.open();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_WRITE);
            ByteBuffer buffer = ByteBuffer.wrap(data.getBytes());

            int totalBytesWritten = 0;
            while (buffer.hasRemaining()) {
                System.out.println("Waiting to write data...");
                selector.select();

                // Handle write operation
                int numBytesWritten = channel.write(buffer);
                if (numBytesWritten == -1) throw new IOException("Error writing to channel");

                totalBytesWritten += numBytesWritten;
                if (totalBytesWritten == data.length()) {
                    System.out.println("Wrote to channel: |" + data + "|");
                    return true;
                }

                // Not all bytes were written, retry after a short delay
                Thread.sleep(100);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }


    public interface IntPredicate {
        boolean func(String n);
    }

}