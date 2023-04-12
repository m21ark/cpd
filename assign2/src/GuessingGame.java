import java.util.Random;
import java.util.Scanner;

public class GuessingGame {
    public static void main(String[] args) {
        Random random = new Random();
        int numberToGuess = random.nextInt(1000) + 1;
        int numberOfGuesses = 0;
        Scanner scanner = new Scanner(System.in);

        int closestGuess = 0;

        while (true) {
            System.out.print("Guess a number between 1 and 1000 (" + (10 - numberOfGuesses) + " guesses left): ");
            int guess = scanner.nextInt();
            numberOfGuesses++;


            // Keep track of the closest guess to get a final score
            if (numberOfGuesses == 1) closestGuess = guess;
            else if (Math.abs(numberToGuess - guess) < Math.abs(numberToGuess - closestGuess)) closestGuess = guess;

            if (numberOfGuesses == 10) {
                System.out.println("You have exceeded the maximum number of guesses. The number was " + numberToGuess + ".");
                break;
            } else if (guess == numberToGuess) {
                System.out.println("Congratulations, you guessed the number in " + numberOfGuesses + " guesses!");
                break;
            } else if (guess < numberToGuess) {
                System.out.println("The number is higher.");
            } else { // guess > numberToGuess
                System.out.println("The number is lower.");
            }
        }

        System.out.println("Your final score is " + (1000 - Math.abs(numberToGuess - closestGuess)) + ".");
    }
}

