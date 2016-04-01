package reddit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Josh on 31/03/2016.
 */
public class RedditTrawler implements Runnable {
    private final RedditApiCaller api;
    private final AssociativeParser associativeParser;
    private final BlockingQueue<WeightedUrl> queue;
    private final Set<String> completed;

    public RedditTrawler(RedditApiCaller api, AssociativeParser associativeParser, BlockingQueue<WeightedUrl> queue, Set<String> completed) {
        System.out.println("Reddit Trawler " + Thread.currentThread().getId() + " initialising.");
        this.api = api;
        this.associativeParser = associativeParser;
        this.queue = queue;
        this.completed = completed;
    }

    @Override
    public void run() {
        try {
            System.out.println("Reddit Trawler " + Thread.currentThread().getId() + " started.");
            while (true) {
                lookup(queue.take());
            }
        } catch (Exception e) {
            System.out.println("Reddit Trawler " + Thread.currentThread().getId() + " threw an exception.");
            e.printStackTrace();
        }
    }

    private void trawl(JsonObject node) {
        String kind = node.get("kind").getAsString();
        node = node.get("data").getAsJsonObject();

        if (kind.equals("t3")) {
            String author = node.get("author").getAsString();
            String fullname = node.get("name").getAsString();
            if (completed.contains(fullname)) return;
            completed.add(fullname);

            associativeParser.parse(author, node.get("title").getAsString());
            double weight = associativeParser.parse(author, node.get("selftext").getAsString());


            if (queue.size() > 1000) {
                Double lowerWeight = queue.stream().skip(500).findFirst().get().weight;
                queue.removeIf(el -> el.weight <= lowerWeight && queue.size() > 500);
                System.out.println("Cleared down to weight " + lowerWeight + " in queue: " + queue.size());
            }
            offer(weight, "/user/" + author + "/comments");
            offer(weight, "/user/" + author + "/submitted");
            offer(weight, "/comments/" + node.get("id").getAsString());
            offer(weight, "/r/" + node.get("subreddit").getAsString());
        } else if (kind.equals("t1")) {
            String author = node.get("author").getAsString();
            String fullname = node.get("name").getAsString();
            if (completed.contains(fullname)) return;
            completed.add(fullname);

            associativeParser.parse(author, node.get("body").getAsString());
            if (node.get("replies").isJsonObject()) trawl(node.get("replies").getAsJsonObject());
        } else if (kind.equals("Listing")) {
            JsonArray children = node.get("children").getAsJsonArray();
            for (int i = 0; i < children.size(); i++) {
                JsonObject child = children.get(i).getAsJsonObject();
                trawl(child);
            }
        } else if (kind.equals("more")) {

        } else {
            System.err.println("Unknown kind " + kind);
        }
    }

    private void offer(double weight, String url) {
        if (completed.contains(url)) return;
        if (queue.stream().anyMatch(el -> el.url.equals(url))) return;
        queue.offer(new WeightedUrl(weight, url));
    }

    private void lookup(WeightedUrl weightedUrl) throws Exception {
        System.out.println("Trawler " + Thread.currentThread().getId() + " processing " + weightedUrl.url + " [" + weightedUrl.weight + "]");
        JsonElement json = api.call(weightedUrl.url);
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            if (obj.get("error") == null) {
                trawl(obj);
            } else {
                System.err.println(obj);
            }
        } else if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                trawl(array.get(i).getAsJsonObject());
            }
        } else {
            System.err.println("Invalid hierarchy returned");
        }
    }
}
