package game.server;

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

    public int authenticate(String username, String password) {

        // Check if the username and password match any existing entry
        for (String line : persistantUsers) {
            String[] fields = line.split(",");
            if (fields[0].equals(username) && fields[1].equals(password))
                return 1; // Credentials match an existing entry
            else if (fields[0].equals(username)) return 2; // Username exists but password is incorrect
        }


        return 0; // Username doesn't exist
/*
            // If the function didn't return yet, the username doesn't exist
            // Ask the user if they want to add a new entry
            Scanner scanner = new Scanner(System.in);
            System.out.println("Username not found. Do you want to add a new user? (y/n)");
            String answer = scanner.nextLine().toLowerCase();

            if (answer.equals("y")) {

                // Ask the user for the password again, this time with asterisks
                System.out.print("Repeat Password: ");
                String newPassword = new String(System.console().readPassword());

                if (!password.equals(newPassword)) return 2; // Password repetition didn't match

                // Append the new entry to the users.txt file
                String newEntry = username + "," + newPassword;
                FileWriter writer = new FileWriter(persistantUsersFile, true); // Append mode
                writer.write(newEntry + System.lineSeparator()); // Add new line separator
                writer.close();

                return 3; // New entry added successfully
            }

            return 0; // User doesn't want to add a new entry
*/
    }

    @Override
    public void run() {

        // Authenticate client
        InputStream input = null;
        int authResult = 0;

        try {
            input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String username = reader.readLine();
            String password = reader.readLine();
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
        if (authResult == 0 || authResult == 2) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        // generate token
        String token = generateRandomToken();
        System.out.println("Client connected with token : " + token);

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
}
