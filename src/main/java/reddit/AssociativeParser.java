package reddit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Josh on 8/02/2016.
 */
public class AssociativeParser {
    private final List<String> trackWords;
    private final List<String> antiTrackWords;
    private final Map<String, Integer> associatedWordCount;
    private final Map<String, Map<String, Integer>> authorWordCount;

    public AssociativeParser(List<String> trackWords, List<String> antiTrackWords) {
        this.trackWords = trackWords;
        this.antiTrackWords = antiTrackWords;
        this.associatedWordCount = new ConcurrentHashMap<>();
        this.authorWordCount = new ConcurrentHashMap<>();
    }

    public double parse(String author, String text) {
        authorWordCount.putIfAbsent(author, new HashMap<>());
        Map<String, Integer> authorWords = authorWordCount.get(author);
        List<String> words = Arrays.asList(text.toLowerCase().replaceAll("[^a-z ]", "")
                                .split(" "))
                                .stream()
                                .map(s -> s.trim())
                                .filter(s -> s.length() > 0)
                                .collect(Collectors.toList());

        double score = 1.0;
        for (String word : words) {
            increment(authorWords, word);
            for(String regex : trackWords) {
                if (word.matches(regex)) {
                    for (String associatedWord : words) {
                        increment(associatedWordCount, associatedWord);
                    }
                    break;
                }
            }
            for(String regex : antiTrackWords) {
                if (word.matches(regex)) {
                    //System.out.println("matched " + word);
                    for (String associatedWord : words) {
                        boolean found = false;
                        for (String trackRegex : trackWords) {
                            if (associatedWord.matches(trackRegex)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) decrement(associatedWordCount, associatedWord);
                    }
                    break;
                }
            }
            if (associatedWordCount.containsKey(word) && associatedWordCount.get(word) != 0 && authorWordCount.get(author).get(word) > 0) {
                score += authorWordCount.get(author).get(word).doubleValue() / associatedWordCount.get(word).doubleValue();
            }
        }
        return score;
    }

    private void increment(Map<String, Integer> map, String word) {
        map.putIfAbsent(word, 0);
        map.put(word, map.get(word) + 1);
    }

    private void decrement(Map<String, Integer> map, String word) {
        map.putIfAbsent(word, 0);
        map.put(word, map.get(word) - 1);
    }

    public Map<String, Integer> getAssociatedWordCount() {
        return associatedWordCount;
    }

    public Map<String, Map<String, Integer>> getAuthorWordCount() {
        return authorWordCount;
    }
}
