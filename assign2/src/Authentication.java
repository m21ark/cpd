import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Authentication {
    private static final String USERS_FILE = "users.txt";

    public static int authenticate(String username, String password) {
        try {
            // Check if the users.txt file exists
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                // If it doesn't exist, create it
                if (file.createNewFile())
                    System.out.println("Created new users.txt file.");
                else
                    System.out.println("Failed to create users.txt file.");
            }

            // Read the lines from the users.txt file
            Path path = Paths.get(USERS_FILE);
            List<String> lines = Files.readAllLines(path);

            // Check if the username and password match any existing entry
            for (String line : lines) {
                String[] fields = line.split(",");
                if (fields[0].equals(username) && fields[1].equals(password))
                    return 1; // Credentials match an existing entry
                else if (fields[0].equals(username))
                    return 2; // Username exists but password is incorrect
            }

            // If the function didn't return yet, the username doesn't exist
            // Ask the user if they want to add a new entry
            Scanner scanner = new Scanner(System.in);
            System.out.println("Username not found. Do you want to add a new user? (y/n)");
            String answer = scanner.nextLine().toLowerCase();

            if (answer.equals("y")) {

                // Ask the user for the password again, this time with asterisks
                System.out.print("Repeat Password: ");
                String newPassword = new String(System.console().readPassword());

                if (!password.equals(newPassword))
                    return 2; // Password repetition didnt match

                // Append the new entry to the users.txt file
                String newEntry = username + "," + newPassword;
                FileWriter writer = new FileWriter(file, true); // Append mode
                writer.write(newEntry + System.lineSeparator()); // Add new line separator
                writer.close();

                return 3; // New entry added successfully
            }

            return 0; // User doesn't want to add a new entry

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            return 0; // Failed to read or write the users.txt file
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = new String(System.console().readPassword());

        int result = authenticate(username, password);

        switch (result) {
            case 1 -> System.out.println("Login successful!");
            case 2 -> System.out.println("Incorrect password.");
            case 3 -> System.out.println("New user added.");
            default -> System.out.println("Login failed.");
        }

    }
}

