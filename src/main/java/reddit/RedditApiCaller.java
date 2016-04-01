package reddit;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class RedditApiCaller {
	private String accessToken = "";
	private String tokenType = "";
	private static final String UA = "Reddit Trawler 0.01";
	private final CloseableHttpClient client = HttpClients.createDefault();
	private final RateLimiter rateLimiter = RateLimiter.create(0.5);
	
	public RedditApiCaller(String username, String password, String clientid, String secret) throws Exception {
		int status = 0;
		do {
			HttpPost post = new HttpPost("https://www.reddit.com/api/v1/access_token");

			List<BasicNameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("grant_type", "password"));
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("password", password));
			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			post.setHeader("Authorization", "Basic " + Base64.encodeBase64String((clientid + ":" + secret).getBytes()));
			CloseableHttpResponse result = client.execute(post);
			status = result.getStatusLine().getStatusCode();
			String content = IOUtils.toString(result.getEntity().getContent());
			System.out.println(status + " " + result.getStatusLine().getReasonPhrase());

			if (status == 200) {
				JsonElement json = new JsonParser().parse(content);
				accessToken = json.getAsJsonObject().get("access_token").getAsString();
				tokenType = json.getAsJsonObject().get("token_type").getAsString();
			} else if (status == 429) {
				Thread.sleep(5000);
			}
		} while (status == 429);
	}

	public JsonElement call(String endpoint) throws Exception {
		HttpGet get = new HttpGet("https://oauth.reddit.com" + endpoint);
		get.setHeader("Authorization", tokenType + " " + accessToken);
		get.setHeader("User-Agent", UA);
		int CONNECTION_TIMEOUT = 10 * 1000; // timeout in millis
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(CONNECTION_TIMEOUT)
				.setConnectTimeout(CONNECTION_TIMEOUT)
				.setSocketTimeout(CONNECTION_TIMEOUT)
				.build();
		get.setConfig(requestConfig);

		double wait = rateLimiter.acquire();
		CloseableHttpResponse result = client.execute(get);
		if (wait < 2.0) System.out.println("Rate can be higher: " + wait);
		get.completed();
		int status = result.getStatusLine().getStatusCode();
		String phrase = result.getStatusLine().getReasonPhrase();
		String content = IOUtils.toString(result.getEntity().getContent());
		result.close();
		if (status != 200) return new JsonParser().parse("{status: " + status + ", error: '" + phrase + "'}").getAsJsonObject();
		return new JsonParser().parse(content);
	}
}
