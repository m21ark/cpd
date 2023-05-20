import game.client.Client;
import game.config.GameConfig;
import game.server.GameModel;
import game.logic.structures.MyConcurrentList;
import game.server.ClientHandler;
import game.server.GameServer;
import game.server.PlayingServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


public class MyTests {

    static int NUM_OF_CLIENTS = 100;
    static int NUM_OF_GAMES_PLAYERS = 2;
    static ArrayList<Client> clients = new ArrayList<>();
    static Thread gameServerThread;
    static int p = 0;

    @BeforeAll
    public static void setup() {
        String[] args = new String[2];
        args[0] = "-debug";
        args[1] = "-new";

        // start game.server on a different thread
        gameServerThread = new Thread(() -> {
            try {
                GameServer.main(args);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        gameServerThread.start();

        try {
            Thread.sleep(1000); // wait for game.server to start
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
        for (int i = 0; i < 1000; i++) {
            try {
                Client client = new Client("Test" + i, 0); // this is a mocked player
                client.getTokenFromServer();
                clients.add(client); // this is a mocked player
            } catch (IOException e) {
                Assertions.fail("Could not create game.client");
            }
        }

        Client client;
        int s = p;
        for (int i = 0; i < 2; i++) {
            client = clients.get(i + s);
            p++;
            try {
                Method privateMethod = client.getClass().getDeclaredMethod("askToPlayGame");
                privateMethod.setAccessible(true);
                privateMethod.invoke(client);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
                Assertions.fail("Could not create a game");
            }
        }
    }


    @Test
    public void spamGamePlaysInRow() {

        for (int i = 0; i < GameModel.getMaxNrGuesses(); i++) { // MAX numbers of tries ... make this more modular
            for (int j = 0; j < NUM_OF_GAMES_PLAYERS; j++) {
                Client client = clients.get(j + p - NUM_OF_GAMES_PLAYERS);
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
            threads.add(new Thread(() -> {
                for (int j = 0; j < GameModel.getMaxNrGuesses(); j++) { // MAX numbers of tries ... make this more modular
                    for (int k = 0; k < NUM_OF_GAMES_PLAYERS; k++) {
                        if (k % 3 != finalI) continue; // Dealing with the threads concurrency
                        Client client = clients.get(k + p - NUM_OF_GAMES_PLAYERS);
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

    /*
    @Test
    public void gameWinnerInConcurrentTest() {
        MyConcurrentList<PlayingServer.WrappedPlayerSocket> list = new MyConcurrentList<>();
        List<Client> _clients = MyTests.clients.stream().limit(GameModel.getNrMaxPlayers()).collect(Collectors.toList());
        long limit = NUM_OF_GAMES_PLAYERS;
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
            threads.add(new Thread(() -> {
                for (int j = 0; j < GameModel.getMaxNrGuesses(); j++) { // MAX numbers of tries ... make this more modular
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

    @Test
    public void authenticateClientConcurrent() {

        GameConfig.instance = new GameConfig(); // resetting the game.config to production environment
        ClientHandler.DEBUG_MODE = false;

        List<Thread> threads = new ArrayList<>();
        MyConcurrentList<Integer> returnCodes = new MyConcurrentList<>();
        // create 3 threads and start them
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            threads.add(new Thread(() -> {
                for (int j = 0; j < NUM_OF_CLIENTS; j++) { // MAX numbers of tries ... make this more modular
                    if (j % 3 != finalI) continue; // Dealing with the threads concurrency
                    Client client = null;
                    try {
                        client = new Client();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        assert client != null;
                        Method privateMethod = client.getClass().getDeclaredMethod("serverAuthenticate", String.class, String.class, String.class);
                        privateMethod.setAccessible(true);
                        int returnCode = (int) privateMethod.invoke(client, "l", "l", "0");
                        //Assertions.assertEquals(1, returnCode);
                        returnCodes.add(returnCode);
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }));
        }

        // start all threads
        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        for (int returnCode : returnCodes) {
            Assertions.assertEquals(1, returnCode);
        }
        Assertions.assertEquals(0, returnCodes.size());
        GameConfig.instance = new GameConfig(); // resetting the game.config to development environment
        ClientHandler.DEBUG_MODE = true;
    }*/

    /*
    @Test
    public void authInRow() throws IOException {

        // This test is a bit slow, has the auth is updating the users.txt file over and over again


        GameConfig.instance = new GameConfig(); // resetting the game.config to production environment
        ClientHandler.DEBUG_MODE = false;


        for (int i = 0; i < 500; i++) {
            Client client = new Client();
            try {
                Method privateMethod = client.getClass().getDeclaredMethod("serverAuthenticate", String.class, String.class, String.class);
                privateMethod.setAccessible(true);
                int returnCode = (int) privateMethod.invoke(client, "l", "l", "0");
               // Assertions.assertEquals(0, returnCode);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        GameConfig.instance = new GameConfig(); // resetting the game.config to development environment
        ClientHandler.DEBUG_MODE = true;
    }
    */

}

