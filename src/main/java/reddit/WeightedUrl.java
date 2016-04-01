package reddit;

/**
 * Created by Josh on 1/04/2016.
 */
public class WeightedUrl implements Comparable {
    public final Double weight;
    public final String url;

    public WeightedUrl(double weight, String url) {
        this.url = url;
        this.weight = weight;
    }

    @Override
    public int compareTo(Object o) {
        return weight.compareTo(((WeightedUrl) o).weight);
    }
}
