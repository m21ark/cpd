import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class GuessingGameServer {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(4444);
        System.out.println("Waiting for clients to connect...");
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected!");

        Random random = new Random();
        int numberToGuess = random.nextInt(1000) + 1;
        int numberOfGuesses = 0;

        int closestGuess = 0;

        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String inputLine;

        //System.out.println("Entering while loop");
        while (true) {
            out.println("Guess a number between 1 and 1000 (" + (10 - numberOfGuesses) + " guesses left): ");
            //System.out.println("Sent guess request to client");
            if ((inputLine = in.readLine()) == null) break;

            int guess = Integer.parseInt(inputLine);
            numberOfGuesses++;

            // Keep track of the closest guess to get a final score
            if (numberOfGuesses == 1) closestGuess = guess;
            else if (Math.abs(numberToGuess - guess) < Math.abs(numberToGuess - closestGuess)) closestGuess = guess;

            if (numberOfGuesses == 10) {
                out.println("You have exceeded the maximum number of guesses. The number was " + numberToGuess + ".");
                break;
            } else if (guess == numberToGuess) {
                out.println("Congratulations, you guessed the number in " + numberOfGuesses + " guesses!");
                break;
            } else if (guess < numberToGuess) {
                out.println("The number is higher.");
            } else { // guess > numberToGuess
                out.println("The number is lower.");
            }
            out.flush();
            //System.out.println("End of loop iteration");
        }

        out.println("Your final score is " + (1000 - Math.abs(numberToGuess - closestGuess) - 1) + ".");

        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }
}
