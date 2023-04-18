import game.client.Client;
import game.logic.GameModel;
import game.server.GameServer;
import game.server.PlayingServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


public class MyTests {

    static ArrayList<Client> clients = new ArrayList<>();

    @BeforeAll
    public static void setup() {
        String[] args = new String[0];
        // start server on a different thread
        new Thread(() -> {
            try {
                GameServer.main(args);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();


        try {
            Thread.sleep(1000); // wait for server to start
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    @BeforeEach
    public void spamClientsAndGamesInRow() {

        // expected time ... https://matttomasetti.medium.com/websocket-performance-comparison-10dc89367055

        // start multiple games
        System.out.println("Server started");
        // create all clients ... does not start (on purpose) the game
        for (int i = 0; i < 10; i++) {
            try {
                Client client = new Client(); // this is a mocked player
                clients.add(client); // this is a mocked player
            } catch (IOException e) {
                Assertions.fail("Could not create client");
            }
        }

        Client client;
        for (int i = 0; i < 5; i++) {
            client = clients.get(i);
            try {
                Method privateMethod = client.getClass().getDeclaredMethod("playGame");
                privateMethod.setAccessible(true);
                privateMethod.invoke(client);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                Assertions.fail("Could not create a game");
            }
        }
    }

    @Test
    public void spamGamePlaysInRow() {

        for (int i = 0; i < 5; i++) { // MAX numbers of tries ... make this more modular
            for (Client client : clients) {
                try {
                    Method privateMethod = client.getClass().getDeclaredMethod("sendGuess", String.class);
                    privateMethod.setAccessible(true);
                    privateMethod.invoke(client, String.valueOf(new Random().nextInt(100))); // de 0 a 99
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    Assertions.fail("Could not send a Guess");
                }
            }
        }
    }

    @Test
    public void spamGamePlaysInConcurrently() {

        List<Thread> threads = new ArrayList<>();


        // create 3 threads and start them
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            threads.add(new Thread(() -> { // TODO: test this better when game is working
                for (int j = 0; j < 5; j++) { // MAX numbers of tries ... make this more modular
                    for (int k = 0; k < clients.size(); k++) {
                        if (k % 3 != finalI) continue; // Dealing with the threads concurrency
                        Client client = clients.get(k);
                        try {
                            Method privateMethod = client.getClass().getDeclaredMethod("sendGuess", String.class);
                            privateMethod.setAccessible(true);
                            privateMethod.invoke(client, String.valueOf(new Random().nextInt(100))); // de 0 a 99
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                            Assertions.fail("Could not send a Guess");
                        }
                    }
                }
            }));
        }

        // start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        // wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void gameWinnerInConcurrentTest() {
        List<PlayingServer.WrappedPlayerSocket> list = new ArrayList<>();
        List<Client> _clients = MyTests.clients.stream().limit(GameModel.getNrMaxPlayers()).collect(Collectors.toList());
        long limit = GameModel.getNrMaxPlayers();
        for (Client client : clients) {
            if (limit-- == 0) break;
            list.add(new PlayingServer.WrappedPlayerSocket(client.getPlayer(), client.getSocketChannel().socket()));
        }


        GameModel gameModel = new GameModel(list);
        int winner = gameModel.getGameWinner();

        // create n threads and play game
        // check if winner is correct

        Thread thread = new Thread(gameModel);
        thread.start();

        List<Thread> threads = new ArrayList<>();
        // create 3 threads and start them
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            threads.add(new Thread(() -> { // TODO: test this better when game is working
                for (int j = 0; j < 5; j++) { // MAX numbers of tries ... make this more modular
                    for (int k = 0; k < list.size(); k++) {
                        if (k % 3 != finalI) continue; // Dealing with the threads concurrency
                        PlayingServer.WrappedPlayerSocket client = list.get(k);
                        int guess = k == list.size() - 1 ? winner + 1 : winner;
                        try {
                            Method privateMethod = _clients.get(k).getClass().getDeclaredMethod("sendGuess", String.class);
                            privateMethod.setAccessible(true);
                            privateMethod.invoke(_clients.get(k), String.valueOf(guess)); // de 0 a 99
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                            Assertions.fail("Could not send a Guess");
                        }
                    }
                }
            }));
        }

        // start all threads
        for (Thread t : threads) {
            t.start();
        }
        // wait for all threads to finish
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Assertions.assertEquals(gamemodel.getWinner(), list.getLast ...);
    }


}

