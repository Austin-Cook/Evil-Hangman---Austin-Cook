package hangman;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EvilHangmanGame implements IEvilHangmanGame {
    private SortedSet<Character> guessedLetters;
    private Set<String> remainingWords;
    private int wordLength;
    private String wordGroupAggregate;
    private String feedbackFromLastTurn;

    public EvilHangmanGame() {
        guessedLetters = new TreeSet<>();
        remainingWords = new HashSet<>();
        wordLength = 0;
        wordGroupAggregate = "";
        feedbackFromLastTurn = "";
    }


    @Override
    public void startGame(File dictionary, int wordLength) throws IOException, EmptyDictionaryException {
        // initialize wordLength
        this.wordLength = wordLength;
        guessedLetters.clear();
        remainingWords.clear();
        wordGroupAggregate = "";
        feedbackFromLastTurn = "";

        // initialize wordGroupAgregate
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < wordLength; i++) {
            builder.append('-');
        }
        wordGroupAggregate = builder.toString();

        // check if file is empty
        Scanner scanner = new Scanner(dictionary);
        if(!scanner.hasNext()) {
            throw new EmptyDictionaryException();
        }

        // read the whole dictionary in
        while(scanner.hasNext()) {
            String word = scanner.next().toLowerCase();
            // only add if the word is the chosen length
            if(word.length() == wordLength) {
                remainingWords.add(word);
            }
        }

        if(remainingWords.size() == 0) {
            throw new EmptyDictionaryException();
        }
    }

    @Override
    public Set<String> makeGuess(char guess) throws GuessAlreadyMadeException {
        guess = Character.toLowerCase(guess);
        String winningWordGroup = "";
        Map<String, Set<String>> remainingWordsMap = new HashMap<>();
        StringBuilder builder = new StringBuilder();

        // check if guess already made
        if(guessedLetters.contains(guess)) {
            throw new GuessAlreadyMadeException();
        } else {
            guessedLetters.add(guess);
        }

        // create wordGroups and map them using remainingWordsMap
        for(String word : remainingWords) {
            // create the wordGroup
            builder.setLength(0);
            for(int letterIndex = 0; letterIndex < word.length(); letterIndex++) {
                if(word.charAt(letterIndex) == guess) {
                    builder.append(guess);
                } else {
                    builder.append('-');
                }
            }
            String wordGroup = builder.toString();

            // map the wordGroup to the word
            // if the wordGroup is in the map
            if (remainingWordsMap.containsKey(wordGroup)) {
                Set<String> updatedWordSet = new HashSet<>(remainingWordsMap.get(wordGroup));
                updatedWordSet.add(word);
                remainingWordsMap.put(wordGroup, updatedWordSet);
            } else {
                // else add it and a new set
                Set<String> wordSet = new HashSet<>();
                wordSet.add(word);
                remainingWordsMap.put(wordGroup, wordSet);
            }
        }

        // the winningWordGroup is the one with the most words in its set
        int maxCountWordGroup = 0;
        int numWordGroupsWithMaxCount = 0;
        for(Set<String> wordGroupSet : remainingWordsMap.values()) {
            if(wordGroupSet.size() > maxCountWordGroup) {
                maxCountWordGroup = wordGroupSet.size();
                numWordGroupsWithMaxCount = 1;
            } else if(wordGroupSet.size() == maxCountWordGroup) {
                numWordGroupsWithMaxCount++;
            }
        }

        // Get the word groups that tie for the highest count
        Set<String> wordGroupsWithMaxCount = new HashSet<>(); // set of word groups that tie for mapping to the highest num words
        Set<String> wordGroupsToRemove = new HashSet<>();
        for(Map.Entry<String, Set<String>> entry : remainingWordsMap.entrySet()) {
            if(entry.getValue().size() == maxCountWordGroup) {
                wordGroupsWithMaxCount.add(entry.getKey());
            } else {
                // remove the entry from map
                wordGroupsToRemove.add(entry.getKey());
            }
        }
        // remove entries from map that don't have highest count
        for(String wordGroup : wordGroupsToRemove) {
            remainingWordsMap.remove(wordGroup);
        }

        // if the is no tie for the wordGroup with the highest count, choose that one
        if(numWordGroupsWithMaxCount == 1) {
            winningWordGroup = wordGroupsWithMaxCount.stream().findAny().get();
        } else {
            // there was a tie so do the tiebreakers

            // (1) choose the wordGroup where the letter does NOT appear - ex: '-----'
            builder.setLength(0);
            for(int i = 0; i < wordLength; i++) {
                builder.append('-');
            }
            String wordGroupNoMatches = builder.toString();

            if(remainingWordsMap.containsKey(wordGroupNoMatches)) {
                winningWordGroup = wordGroupNoMatches;
            } else {
                // (2) choose the wordGroup with the fewest letters
                int minCountLetterFound = 1000;
                int numWordGroupsWithMinCount = 0;
                for(String wordGroup : remainingWordsMap.keySet()) {    // FIXME is the state good here?
                    int count = 0;
                    for(int i = 0; i < wordLength; i++) {
                        if(wordGroup.charAt(i) != '-') {
                            count++;
                        }
                    }

                    if(count < minCountLetterFound) {
                        minCountLetterFound = count;
                        numWordGroupsWithMinCount = 1;
                    } else if(count == minCountLetterFound) {
                        numWordGroupsWithMinCount++;
                    }
                }

                // get all wordGroups with min count
                Set<String> wordGroupsWithMinCount = new HashSet<>();
                for(String wordGroup : remainingWordsMap.keySet()) {    // FIXME is the state good here?
                    int count = 0;
                    for (int i = 0; i < wordLength; i++) {
                        if (wordGroup.charAt(i) != '-') {
                            count++;
                        }
                    }
                    if(count == minCountLetterFound) {
                        wordGroupsWithMinCount.add(wordGroup);
                    }
                }

                // if there is no tie for word groups with min count, that wordGroup wins
                if(numWordGroupsWithMinCount == 1) {
                    winningWordGroup = wordGroupsWithMinCount.stream().findFirst().get();
                } else {
                    // (3) rightmost guessed letter
                    Set<String> wordGroupsWithCharAtIndex;
                    for(int letterIndex = wordLength - 1; letterIndex >= 0; letterIndex--) {
                        wordGroupsWithCharAtIndex = getWordGroupsWithCharAtIndex(remainingWordsMap.keySet(), letterIndex);

                        if(wordGroupsWithCharAtIndex.size() == 1) {
                            // there is at least one wordGroup with a char at the given index
                            winningWordGroup = wordGroupsWithCharAtIndex.stream().findFirst().get();
                            break;
                        } else if(wordGroupsWithCharAtIndex.size() > 1) {
                            // there are multiple, so remove all others from the map
                            wordGroupsToRemove.clear();
                            for(Map.Entry<String, Set<String>> entry : remainingWordsMap.entrySet()) {  // FIXME make sure this is narrowing it down
                                if(!wordGroupsWithCharAtIndex.contains(entry.getKey())) {
                                    wordGroupsToRemove.add(entry.getKey());
                                }
                            }
                            for(String wordGroup : wordGroupsToRemove) {
                                remainingWordsMap.remove(wordGroup);
                            }
                        } // else the size is 0 (doesn't narrow anything down, so keep iterating
                    }

                    // at this point the set of wordGroups will always be narrowed down to 1
                    //assert(remainingWordsMap.size() == 1);
                }
            }
        }

        // remove words that don't follow winningWordGroup
        Set<String> wordsToRemove = new TreeSet<>();
        for(String word : remainingWords) {
            boolean wordMatchesWordGroup = true;
            for(int i = 0; i < wordLength; i++) {
                if(winningWordGroup.charAt(i) == guess && word.charAt(i) != guess) {
                    wordMatchesWordGroup = false;
                }
                if(word.charAt(i) == guess && winningWordGroup.charAt(i) != guess) {
                    wordMatchesWordGroup = false;
                }
            }

            if(!wordMatchesWordGroup) {
                wordsToRemove.add(word);
            }
        }
        for(String word : wordsToRemove) {
            remainingWords.remove(word);
        }

        // update feedbackFromLastTurn and wordGroupAggregate
        builder.setLength(0);
        builder.append(wordGroupAggregate);
        int numLetterFound = 0;
        for(int i = 0; i < wordLength; i++) {
            if(winningWordGroup.charAt(i) != '-') {
                builder.setCharAt(i, guess);
                numLetterFound++;
            }
        }
        wordGroupAggregate = builder.toString();
        if(numLetterFound == 0) {
            feedbackFromLastTurn = "Sorry, there are no " + guess + "'s";
        } else {
            feedbackFromLastTurn = "Yes, there are " + numLetterFound + " " + guess + "'s";
        }

        return remainingWords;
    }

    private Set<String> getWordGroupsWithCharAtIndex(Set<String> wordGroups, int index) {
        Set<String> wordGroupsWithCharAtIndex = new TreeSet<>();

        for(String wordGroup : wordGroups) {
            if(wordGroup.charAt(index) != '-') {
                wordGroupsWithCharAtIndex.add(wordGroup);
            }
        }

        return wordGroupsWithCharAtIndex;
    }

    @Override
    public SortedSet<Character> getGuessedLetters() {
        return guessedLetters;
    }

    public String getWordGroupAggregate() {
        return wordGroupAggregate;
    }

    public String getFeedbackFromLastTurn() {
        return feedbackFromLastTurn;
    }
}
