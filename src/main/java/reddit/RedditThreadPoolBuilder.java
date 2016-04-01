package reddit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Josh on 31/03/2016.
 */
public class RedditThreadPoolBuilder {
    private RedditApiCaller api;
    private int threads;
    private List<String> trackedWords;
    private BlockingQueue<WeightedUrl> urlQueue;
    private Set<String> completedFullnameSet;
    private String prefix;
    private int threadCount = 0;
    private List<String> antiTrackedWords;

    public RedditThreadPoolBuilder withApi(RedditApiCaller api) {
        this.api = api;
        return this;
    }

    public RedditThreadPoolBuilder withThreads(int threads) {
        this.threads = threads;
        return this;
    }

    public RedditThreadPoolBuilder withTrackedWords(List<String> trackedWords) {
        this.trackedWords = trackedWords;
        return this;
    }

    public RedditThreadPoolBuilder withAntiTrackedWords(List<String> antiTrackedWords) {
        this.antiTrackedWords = antiTrackedWords;
        return this;
    }

    public RedditThreadPoolBuilder withUrlQueue(BlockingQueue<WeightedUrl> urlQueue) {
        this.urlQueue = urlQueue;
        return this;
    }

    public RedditThreadPoolBuilder withCompletedFullnameSet(Set<String> completedFullnameSet) {
        this.completedFullnameSet = completedFullnameSet;
        return this;
    }

    public RedditThreadPoolBuilder withPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public Set<AssociativeParser> build() {
        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> new Thread(r, prefix + " " + threadCount++));

        Set<AssociativeParser> parsers = new HashSet<>();
        for (int i = 0; i < threads * 2; i++) {
            AssociativeParser parser = new AssociativeParser(trackedWords, antiTrackedWords);
            parsers.add(parser);
            pool.submit(new RedditTrawler(api, parser, urlQueue, completedFullnameSet));
        }
        return parsers;
    }
}
