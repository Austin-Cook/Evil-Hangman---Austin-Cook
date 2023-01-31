package hangman;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class EvilHangman {

    public static void main(String[] args) {
        EvilHangmanGame evilHangmanGame = new EvilHangmanGame();
        String wordGroupAggregate = "";
        Set<String> possibleWords = new TreeSet<>();
        Scanner scanner =  new Scanner(System.in);

        String dictName = args[0];
        int wordLength = Integer.parseInt(args[1]);
        int numGuesses = Integer.parseInt(args[2]);
        File dictionary = new File(dictName);

        if(wordLength < 2) {
            System.err.println("Error, there must be at least 2 letters per word");
        }

        if(numGuesses < 1) {
            System.err.println("Error, there must be at least 1 guess");
        }

        try {
            evilHangmanGame.startGame(dictionary, wordLength);
        }
        catch (IOException e) {
            System.err.println("Error: File couldn't be read");
        } catch (EmptyDictionaryException e) {
            System.err.println("Error: Empty dictionary or no words of given word length");
        }

        // this loop represents a turn
        while(numGuesses > 0) {
            // print game info
            System.out.println("\nYou have " + numGuesses + " guesses left");
            System.out.print("Used letters: ");
            for(Character letter : evilHangmanGame.getGuessedLetters()) {
                System.out.print(letter + " ");
            }
            System.out.println("\nWord: " + evilHangmanGame.getWordGroupAggregate());

            // get a valid char for input
            String input = "";
            char inputAsChar = '.';
            // loops until valid input is given
            while(true) {
                System.out.print("Enter Guess: ");
                input = scanner.next().toLowerCase();

                boolean isValid = true;
                if(input.length() != 1) {
                    isValid = false;
                } else {
                    inputAsChar = input.charAt(0);
                    if(!Character.isLetter(inputAsChar))
                    isValid = false;
                }

                if(isValid) {
                    break;
                } else {
                    System.out.print("Invalid input! ");
                }
            }

            // make the guess
            try {
                possibleWords = evilHangmanGame.makeGuess(inputAsChar);

                // end of turn - print feedback
                String feedbackFromGuess = evilHangmanGame.getFeedbackFromLastTurn();
                System.out.println(feedbackFromGuess);
                if(feedbackFromGuess.contains("Sorry")) {
                    numGuesses--;
                }

                // check for end of game
                if(evilHangmanGame.getWordGroupAggregate().indexOf('-') == -1) {
                    numGuesses = 0;
                }
            } catch(GuessAlreadyMadeException e) {
                System.out.println("Guess already made! ");
            }
        }

        // check if won or lost
        if(evilHangmanGame.getWordGroupAggregate().indexOf('-') == -1) {
            System.out.println("You win! You guessed the word: " + evilHangmanGame.getWordGroupAggregate());
        } else {
            System.out.println("Sorry, you lost! The word was: " + possibleWords.stream().findAny().get());
        }
    }
}