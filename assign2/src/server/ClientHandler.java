package game.server;

import game.config.GameConfig;
import game.protocols.CommunicationProtocol;
import game.protocols.TokenState;
import game.utils.Logger;
import game.utils.SocketUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class ClientHandler implements Runnable {
    public static boolean DEBUG_MODE = false;
    private final Socket socket;
    private File persistantUsersFile;
    private List<String> persistantUsers; // Format: username:password:token:score
    private boolean isAReturningUser = false;

    // should every handler have its own version of the file?

    public ClientHandler(Socket accept) {
        this.socket = accept;

        loadPersistantStorage();
    }

    public static void saveNewTokenToFile(String username, String newToken) {
        try {
            // TODO : lock here
            RandomAccessFile raf = new RandomAccessFile("database/users.txt", "rw");

            // Read the file line by line
            String line;
            while ((line = raf.readLine()) != null) {

                // Split the line into its components
                String[] parts = line.split(",");

                // Check if the username matches
                if (parts[0].equals(username)) {
                    // Update the token and leave the loop
                    long pointer = raf.getFilePointer();
                    raf.seek(pointer - line.length() - 1);
                    raf.writeBytes(line.replace(parts[2], newToken));
                    break;
                }
            }
            raf.close();
            Logger.info("Updated token for user '" + username + "' to " + newToken);
        } catch (IOException e) {
            Logger.error("Could not save new token to file.");
        }
    }

    public static boolean isTokenStillValid(String token) {
        // Check if the token is valid
        if (token.length() != 42) return false;
        String expiration = token.substring(32);
        Instant expirationDate = Instant.ofEpochSecond(Long.parseLong(expiration));
        return Instant.now().isBefore(expirationDate);
    }

    private void loadPersistantStorage() {
        try {

            // Check if the database/users.txt file exists
            String dir = "database/users.txt";
            persistantUsersFile = new File(dir);

            if (!persistantUsersFile.exists()) {
                // If it doesn't exist, create it
                if (persistantUsersFile.createNewFile()) Logger.warning("Created new database/users.txt file.");
                else Logger.warning("Failed to create database/users.txt file.");
            }

            // Read the lines from the database/users.txt file
            Path path = Paths.get(dir);
            this.persistantUsers = Files.readAllLines(path);

        } catch (IOException e) {
            Logger.error("Could not intialize database/users.txt file.");
        }
    }

    private String generateRandomToken() {
        UUID uuid = UUID.randomUUID();
        Instant expiration = Instant.now().plus(GameConfig.getInstance().getTokenLifeSpan(), ChronoUnit.SECONDS);
        return uuid.toString().replace("-", "") + String.format("%010d", expiration.getEpochSecond());
    }

    public String authenticate(String username, String password) {
        // TODO: falta um timout para o caso de o cliente nao responder
        // Check if the username and password match any existing entry
        for (String line : persistantUsers) {
            String[] fields = line.split(",");
            if (fields[0].equals(username) && fields[1].equals(password))
                return "1," + fields[3]; // Credentials match an existing entry
            else if (fields[0].equals(username)) return "2,0";// Username exists but password is incorrect
        }
        return "0,0"; // Username doesn't exist
    }

    @Override
    public void run() {
        String token;

        if (DEBUG_MODE) {
            token = generateRandomToken();
            SocketUtils.writeData(socket, token);
        } else {
            token = authenticateUser();
            if (token.equals("")) return; // Authentication failed
        }

        Logger.info("Token sent. Adding client to the server's list...");

        if (isAReturningUser) dealWithReturningUser(token);
        else SocketUtils.sendToClient(socket, CommunicationProtocol.MENU_CONNECT);

        System.out.println("here 3 : " + GameServer.instance.clients.size());
        // GameServer.getInstance().clientsStates.put(token, new TokenState(null, TokenState.TokenStateEnum.QUEUED));
        GameServer.instance.clients.put(token, socket); //TODO: lock here --> we are writting
        System.out.println(GameServer.instance.clientsStates);
        System.out.println("here 4 : " + GameServer.instance.clients.size());
    }

    private void dealWithReturningUser(String token) {
        System.out.println("here -1");
        System.out.println(GameServer.getInstance().clientsStates);
        System.out.println("here0");
        GameServer gs = GameServer.getInstance();
        System.out.println("here0.4");
        var aux = gs.clientsStates.get(token);
        if (aux == null) {
            Logger.info("Couldn't find a state to recover.");
            SocketUtils.sendToClient(socket, CommunicationProtocol.MENU_CONNECT);
            return;
        }
        System.out.println(aux);
        System.out.println("here0.5");
        TokenState.TokenStateEnum ts = aux.getState();
        System.out.println("here1");
        switch (ts) {
            case QUEUED -> {
                Logger.info("Client was in the queue. Getting him back in the queue...");
                SocketUtils.sendToClient(socket, CommunicationProtocol.QUEUE_RECONNECT);
                // TODO: send the client the current queue position
            }
            case PLAYGROUND -> {
                Logger.info("Client was in the playground. Getting him back in the playground...");
                System.out.println("here lol 1");
                var aux2 = aux.getModel();
                System.out.println("here lol 1.5");
                System.out.println(aux2);
                if (aux2 == null) {
                    Logger.info("Couldn't find a model to recover to playground.");
                    SocketUtils.sendToClient(socket, CommunicationProtocol.MENU_CONNECT);
                    return;
                }
                String playerCount = String.valueOf(aux2.getCurrentPlayers()); // TODO: PROBLEM 1: CONCURRENT ACCESS LOCKED HERE
                System.out.println("here lol 2");
                String max_num_players = String.valueOf(GameConfig.getInstance().getMaxNrGuess());
                System.out.println("here 5");
                SocketUtils.sendToClient(socket, CommunicationProtocol.PLAYGROUND_RECONNECT, playerCount, max_num_players);
                System.out.println("here 6");
                // TODO: send the client the same playground
            }
            case PLAYING -> {
                Logger.info("Client was in a game. Getting him back in the game...");

                GameModel model = gs.clientsStates.get(token).getModel();

                if (model == null) {
                    Logger.info("Couldn't find a model to recover to game.");
                    SocketUtils.sendToClient(socket, CommunicationProtocol.MENU_CONNECT);
                    return;
                }

                model.upadtePlayerSocket(token, socket);

                String guessDirection = model.getGameWinner() - model.getBestGuess(token) > 0 ? "higher" : "lower";

                SocketUtils.sendToClient(socket, CommunicationProtocol.GAME_RECONNECT, // Protocol
                        String.valueOf(GameModel.MAX_NR_GUESSES), // Max number of guesses
                        String.valueOf(GameModel.NR_MAX_PLAYERS), // NUmber of players
                        String.valueOf(GameModel.MAX_GUESS), // Max guess
                        String.valueOf(model.getGuessesLeft(token)), // Guesses left
                        String.valueOf(model.getBestGuess(token)), // Best guess yet
                        guessDirection // If the guess is higher or lower
                );
            }
            case MENU -> {
                Logger.info("Client was in the menu. Sending him back to the menu...");
                SocketUtils.sendToClient(socket, CommunicationProtocol.MENU_CONNECT);
            }
            default -> {
                Logger.info("Unforseen state. Sending player to the main menu...");
                SocketUtils.sendToClient(socket, CommunicationProtocol.MENU_CONNECT);
            }
        }
        System.out.println("here2 ");
    }

    private String authenticateUser() {

        // Authenticate client
        String username, password, token;
        boolean newUser = false;

        // Read username, password, token from client and try to authenticate
        String username_password_tok = SocketUtils.readData(socket);

        if (username_password_tok == null) {
            Logger.error("Client closed connection or took long to respond.");
            return "";
        }

        username = username_password_tok.split(",")[0];
        password = username_password_tok.split(",")[1];
        token = username_password_tok.split(",")[2];

        Logger.info("Client connected with username : " + username + " and password : " + password);
        String authPair = authenticate(username, password);
        int authResult = Integer.parseInt(authPair.split(",")[0]);
        String rank = authPair.split(",")[1];
        Logger.info("Authentication result : " + authResult);

        if (authResult == 1) token = checkIfValidToken(username, token);
        else token = generateRandomToken();

        // Respond to client
        SocketUtils.writeData(socket, authResult + "," + token + "," + rank);

        // if auth fails, close socket for this client
        if (authResult == 2) {
            SocketUtils.closeSocket(socket);
            return "";
        } else if (authResult == 0) {
            if (registerNewUser(username, password)) newUser = true;
            else return "";
        }

        // if new user, add to persistant storage
        if (newUser) {
            Logger.info("New user detected. Adding to persistant storage.");
            addNewUserToPersistantStorage(username, password, token);
            SocketUtils.writeData(socket, "1");
        }

        // write to client
        Logger.info("Sending token to client: " + username + " <-> " + token);
        SocketUtils.writeData(socket, token);
        return token;
    }

    private String checkIfValidToken(String username, String token) {
        if (token.equals("0")) {
            Logger.info("Client has no token. Generating a new one.");
            token = generateRandomToken();
            saveNewTokenToFile(username, token);
        } else {
            Logger.info("Client has a token. Checking if it's valid.");
            if (isValidTok(username, token)) {
                /*GameServer.clients.containsKey(token)*/
                Logger.info("Client's token is valid.");
                this.isAReturningUser = true;
            } else {
                Logger.info("Client's token is invalid. Generating a new one.");
                token = generateRandomToken();
                saveNewTokenToFile(username, token);
            }
        }
        return token;
    }

    private boolean isValidTok(String username, String token) {
        if (!isTokenStillValid(token)) return false;
        // TODO: LOCK HERE
        for (String line : persistantUsers) {
            String[] fields = line.split(",");
            if (fields[0].equals(username) && fields[2].equals(token)) return true;
        }
        return false;
    }

    private boolean registerNewUser(String username, String password) {
        // client will answer if they want to add a new entry
        int authResult = 0;

        // Read password confirmation from client
        String passwordConf = SocketUtils.readData(socket);

        if (passwordConf == null) {
            Logger.error("Client closed connection.");
            return false;
        }

        Logger.info("Client wants to add a new entry. Password confirmation : |" + passwordConf + "| .");

        if (passwordConf.equals("CANCEL_NEW_USER")) { // TODO: Change this to a proper enum
            Logger.info("User doesn't want to add a new entry.");
            authResult = 2;
        } else if (!password.equals(passwordConf)) {
            Logger.info("Passwords don't match. Expected |" + password + "| but got |" + passwordConf + "| .");
            authResult = 2;
        } else {
            Logger.info("Password was confirmed.");
            authResult = 1;
            return true;
        }

        // Respond to client
        SocketUtils.writeData(socket, String.valueOf(authResult));
        return false;
    }


    private void addNewUserToPersistantStorage(String username, String passwordConf, String token) {
        // TODO: ADD LOCK HERE TO WRITE TO FILE

        // Append the new entry to the database/users.txt file
        // Format: username,password,token,rank
        String newEntry = username + "," + passwordConf + "," + token + "," + "00000";
        FileWriter writer = null; // Append mode

        try {
            writer = new FileWriter(persistantUsersFile, true);
            writer.write(newEntry + System.lineSeparator()); // Add new line separator
            writer.close();
            Logger.info("New user added to persistant storage.");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
