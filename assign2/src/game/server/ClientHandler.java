package game.server;

import game.protocols.CommunicationProtocol;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class ClientHandler implements Runnable {
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
                if (persistantUsersFile.createNewFile()) System.out.println("Created new users.txt file.");
                else System.out.println("Failed to create users.txt file.");
            }

            // Read the lines from the users.txt file
            Path path = Paths.get(dir);
            this.persistantUsers = Files.readAllLines(path);

        } catch (IOException e) {
            System.out.println("Could not intialize users.txt file.");
        }
    }

    private String generateRandomToken() {
        return "token" + Math.random() + socket.getLocalPort() + socket.getPort();
    }

    public int authenticate(String username, String password) throws IOException {

        // TODO: falta um timout para o caso de o cliente nao responder

        // Check if the username and password match any existing entry
        for (String line : persistantUsers) {
            String[] fields = line.split(",");
            if (fields[0].equals(username) && fields[1].equals(password))
                return 1; // Credentials match an existing entry
            else if (fields[0].equals(username)) return 2; // Username exists but password is incorrect
        }

        return 0; // Username doesn't exist


    }

    @Override
    public void run() {

        // Authenticate client
        InputStream input = null;
        int authResult = 0;
        String username = "", password = "";
        boolean newUser = false;

        try {
            input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            username = reader.readLine();
            password = reader.readLine();
            System.out.println("Client connected with username : " + username + " and password : " + password);
            authResult = authenticate(username, password);
            System.out.println("Authentication result : " + authResult);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Respond to client
        OutputStream output = null;
        try {
            output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(authResult);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // if auth fails, close socket for this client
        if (authResult == 2) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        } else if (authResult == 0) {

            // client will awnser if they want to add a new entry
            String passwordConf;
            try {
                input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                passwordConf = reader.readLine().strip();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Client wants to add a new entry. Password confirmation : |" + passwordConf+ "| .");

            if (passwordConf.equals("CANCEL_NEW_USER")) { // TODO: Change this to a proper enum
                System.out.println("User doesn't want to add a new entry.");
                authResult = 2;
            } else if (!password.equals(passwordConf)) {
                System.out.println("Passwords don't match. Expected |" + password + "| but got |" + passwordConf + "| .");
                authResult = 2;
            } else {
                System.out.println("Password was confirmed.");
                newUser = true;
                authResult = 1;
            }

            // Respond to client
            output = null;
            try {
                output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                writer.println(authResult);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (authResult != 1) {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            // New entry added successfully
        }

        // generate token
        String token = generateRandomToken();
        System.out.println("Client connected with token : " + token);

        // if new user, add to persistant storage
        if (newUser) {
            System.out.println("New user detected. Adding to persistant storage.");
            addNewUserToPersistantStorage(username, password, token);
        } else {
            // If the client exists, but the token is different, update the token
            System.out.println("User already exists. Updating token.");
            updateToken(username, token);
        }

        // add client to the server's list
        GameServer.clients.put(token, socket); //TODO: lock here --> we are writting

        // write to client
        output = null;
        try {
            output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(token);
            // GameServer.playingServer.queueGame(
            //         new PlayingServer.WrappedPlayerSocket(
            //                 new GamePlayer("Player", 1), //tem de vir da autenticaçao
            //                 socket)
            // );
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void updateToken(String username, String token) {
        // TODO: ADD LOCK HERE TO WRITE TO FILE

        for (String line : persistantUsers) {
            String[] fields = line.split(",");
            fields[2] = token;
            break;
        }
        System.out.println("Updated volatile token for user " + username);

        // TODO: this is updating the list, but not the file
    }

    private void addNewUserToPersistantStorage(String username, String passwordConf, String token) {
        // TODO: ADD LOCK HERE TO WRITE TO FILE

        // Append the new entry to the users.txt file
        String newEntry = username + "," + passwordConf + "," + token;
        FileWriter writer = null; // Append mode

        try {
            writer = new FileWriter(persistantUsersFile, true);
            writer.write(newEntry + System.lineSeparator()); // Add new line separator
            writer.close();
            System.out.println("New user added to persistant storage.");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
