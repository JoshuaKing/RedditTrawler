package reddit;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by Josh on 31/03/2016.
 */
public class RedditRobot {
    private static final List<String> TRACK_WORDS = Arrays.asList("bomb.*", "inshallah", "islam.*", "murder.*", "nuke.*", "revolution", "explosive.*", "tnt", "killotonne", "megatonne", ".*hydrox.*", "ammoni.*", "emp", "ied", "nuclear", "acet.*", ".*oxide.*");
    private static final List<String> ANTI_TRACK_WORDS = Arrays.asList("game.*", "gaming", "magic.*", "medkit");

    private final int NUM_THREADS = 2;

    public RedditRobot() throws Exception{
        RedditApiCaller api = new RedditApiCaller("throwaway954745", "123456", "-1p2_-SqDgv2Cw", "I1MI_Y-vFE26izVSzXDuitfHcJw");

        Set<String> completedFullnames = new ConcurrentSkipListSet<>();
        BlockingQueue<WeightedUrl> urlQueue = new PriorityBlockingQueue<>(1000, Collections.reverseOrder());
        urlQueue.add(new WeightedUrl(1.0, "/r/all"));

        Set<AssociativeParser> parsers = new RedditThreadPoolBuilder()
                .withApi(api)
                .withThreads(NUM_THREADS)
                .withPrefix("Reddit Trawler")
                .withUrlQueue(urlQueue)
                .withCompletedFullnameSet(completedFullnames)
                .withTrackedWords(TRACK_WORDS)
                .withAntiTrackedWords(ANTI_TRACK_WORDS)
                .build();

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate((Runnable) () -> {
            Map<String, Map<String, Integer>> authors = new HashMap<>();
            Map<String, Integer> totalWords = new HashMap<>();
            Map<String, Integer> associatedWordsCount = new HashMap<>();

            parsers.stream().forEach(parser -> {
                parser.getAuthorWordCount().forEach((author, wordMap) -> {
                    authors.putIfAbsent(author, new HashMap<>());

                    wordMap.forEach((k, v) -> {
                        authors.get(author).merge(k, v, Integer::sum);
                        totalWords.merge(k, v, Integer::sum);
                    });
                });

                parser.getAssociatedWordCount().forEach((k, v) -> {
                    associatedWordsCount.merge(k, v, Integer::sum);
                });
            });

            TreeMap<Double, String> rankedWordsSorted = new TreeMap<>();
            HashMap<String, Double> rankedWords = new HashMap<>();

            associatedWordsCount.forEach((k, v) -> {
                if (v > 10) {
                    double score = v.doubleValue() / totalWords.get(k);
                    rankedWordsSorted.put(score, k);
                    rankedWords.put(k, score);
                }
            });

            System.out.println("Starting data dump");

            try (PrintWriter out = new PrintWriter("authorWords.txt")) {
                authors.forEach((author, wordMap) -> {
                    out.println(author);
                    wordMap.forEach((k, v) -> out.println("   " + k + ": " + v));
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (PrintWriter out = new PrintWriter("totalWords.txt")) {
                totalWords.forEach((k, v) -> out.println(k + ": " + v));
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (PrintWriter out = new PrintWriter("associatedWords.txt")) {
                associatedWordsCount.forEach((k, v) -> out.println(k + ": " + v));
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Sorting words.");
            try (PrintWriter out = new PrintWriter("rankedWords.txt")) {
                rankedWordsSorted.descendingMap().forEach((k,v) -> out.println(v + ": " + k));
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Sorting authors.");
            try (PrintWriter out = new PrintWriter("rankedAuthors.txt")) {
                PrintWriter outUnscaled = new PrintWriter("rankedUnscaledAuthors.txt");
                TreeMap<Double, String> sorted = new TreeMap<>();
                TreeMap<Double, String> sortedUnscaled = new TreeMap<>();
                authors.forEach((author, wordMap) -> {
                    final int total = wordMap.values().stream().reduce(0, Integer::sum);
                    if (total > 20) {
                        double score = wordMap
                                .keySet()
                                .stream()
                                .mapToDouble(k -> wordMap.get(k).doubleValue() * rankedWords.getOrDefault(k, 0.0).doubleValue())
                                .sum();
                        sorted.put(score / total, author);
                        sortedUnscaled.put(score, author);
                    }
                });
                sorted.descendingMap().forEach((k,v) -> out.println(v + ": " + k));
                sortedUnscaled.descendingMap().forEach((k,v) -> outUnscaled.println(v + ": " + k));
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Finished data dump. Queue State: " + urlQueue.size());
        }, 5, 7, TimeUnit.SECONDS);
    }
}
