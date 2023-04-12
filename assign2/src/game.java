import java.util.Random;
import java.util.Scanner;

public class GuessingGame {
    public static void main(String[] args) {
        Random random = new Random();
        int numberToGuess = random.nextInt(100) + 1;
        int numberOfGuesses = 0;
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Guess a number between 1 and 100: ");
            int guess = scanner.nextInt();
            numberOfGuesses++;

            if (guess == numberToGuess) {
                System.out.println("Congratulations, you guessed the number in " + numberOfGuesses + " guesses!");
                break;
            } else if (guess < numberToGuess) {
                System.out.println("The number is higher.");
            } else {
                System.out.println("The number is lower.");
            }
        }
    }
}

