package game.server;

import game.utils.Logger;
import game.utils.SocketUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ClientHandler implements Runnable {
    public static boolean DEBUG_MODE = false;
    private final Socket socket;
    private File persistantUsersFile;
    private List<String> persistantUsers; // Format: username:password:token  (token is optional)

    // should every handler have its own version of the file?

    public ClientHandler(Socket accept) {
        this.socket = accept;

        loadPersistantStorage();
    }

    private void loadPersistantStorage() {
        try {

            // Check if the users.txt file exists
            String dir = "users.txt";
            persistantUsersFile = new File(dir);

            if (!persistantUsersFile.exists()) {
                // If it doesn't exist, create it
                if (persistantUsersFile.createNewFile()) Logger.warning("Created new users.txt file.");
                else Logger.warning("Failed to create users.txt file.");
            }

            // Read the lines from the users.txt file
            Path path = Paths.get(dir);
            this.persistantUsers = Files.readAllLines(path);

        } catch (IOException e) {
            Logger.error("Could not intialize users.txt file.");
        }
    }

    private String generateRandomToken() {
        return "token" + Math.random() + socket.getLocalPort() + socket.getPort();
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
        GameServer.clients.put(token, socket); //TODO: lock here --> we are writting
    }

    private String authenticateUser() {

        // Authenticate client
        String username, password;
        boolean newUser = false;

        // Read username and password from client and try to authenticate
        String username_password = SocketUtils.readData(socket);

        if (username_password == null) {
            Logger.warning("Client closed connection.");
            return "";
        }

        username = username_password.split(",")[0];
        password = username_password.split(",")[1];

        Logger.info("Client connected with username : " + username + " and password : " + password);
        String authPair = authenticate(username, password);
        int authResult = Integer.parseInt(authPair.split(",")[0]);
        String rank = authPair.split(",")[1];
        Logger.info("Authentication result : " + authResult);

        String token = generateRandomToken();

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
        } else Logger.warning("User already exists.");

        // write to client
        Logger.info("Sending token to client: " + username + " <-> " + token);
        SocketUtils.writeData(socket, token);
        return token;
    }

    private boolean registerNewUser(String username, String password) {
        // client will awnser if they want to add a new entry
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


    private void updateRank(String username, int rank) {
        // TODO: ADD LOCK HERE TO WRITE TO FILE

        for (String line : persistantUsers) {
            String[] fields = line.split(",");
            fields[3] = String.valueOf(rank);
            break;
        }

        Logger.info("Updated volatile rank for user " + username);

        // TODO: this is updating the list, but not the file
    }


    private void addNewUserToPersistantStorage(String username, String passwordConf, String token) {
        // TODO: ADD LOCK HERE TO WRITE TO FILE

        // Append the new entry to the users.txt file
        // Format: username,password,token,rank
        String newEntry = username + "," + passwordConf + "," + token + "," + 0;
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
