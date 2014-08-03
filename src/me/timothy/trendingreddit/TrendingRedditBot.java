package me.timothy.trendingreddit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.github.jreddit.submissions.Submission;
import com.github.jreddit.submissions.Submissions;
import com.github.jreddit.submissions.Submissions.Popularity;
import com.github.jreddit.user.User;
import com.github.jreddit.utils.restclient.HttpRestClient;
import com.github.jreddit.utils.restclient.RestClient;

/**
 * Trending reddit bot - Takes posts from /r/Trendingreddits finds
 * the first subreddit mentions and posts something about it as a
 * comment in the user-defined form
 * 
 * @author Timothy
 */
public class TrendingRedditBot {
	public static final String USER_INFO_FILE_JSON = "user.json";
	public static final String COMMENT_FORMAT_TEXT = "comment_format.txt";
	public static final String REMEMBERED_POSTS_JSON = "remembered_posts.json";
	public static final String SUBREDDIT = "trendingreddits";

	private static final String USER_INFO_FORMAT_EXAMPLE = "{\n" +
			"  \"user\": \"username of bot\",\n" +
			"  \"password\": \"password of bot\"\n" +
			"}";
	private static final Comparator<HandledPost> HANDLED_POSTS_COMPARATOR = new Comparator<HandledPost>() {

		@Override
		public int compare(HandledPost o1, HandledPost o2) {
			if(o1.timestamp == o2.timestamp)
				return 0;
			if(o1.timestamp > o2.timestamp)
				return 1;
			return -1;
		}

	};

	private Logger logger;
	private String username;
	private String password;
	private String commentFormat;

	private List<HandledPost> alreadyHandledPosts;

	public static void main(String[] args) {
		TrendingRedditBot trb = new TrendingRedditBot();

		trb.begin();
	}

	public TrendingRedditBot() {
		logger = LogManager.getLogger();
	}

	public void begin() {
		logger.info("Loading configuration...");
		loadConfiguration();
		logger.info("Configuration successfully loaded");

		logger.info("Loading remembered posts database...");
		loadRememberedPosts();
		logger.info("Remembered posts database successfully loaded (" + alreadyHandledPosts.size() + " posts remembered)");


		RestClient client = new HttpRestClient();
		client.setUserAgent("/r/trendingreddits bot by /u/Tjstretchalot");

		User user = new User(client, username, password);
		try {
			user.connect();
		}catch(Exception ex) {
			logger.error("Failed to authenticate as " + username + ": " + ex.getMessage());
			logger.throwing(ex);
			System.exit(1);
		}

		logger.info("Successfully authd as " + username);
		sleepFor(2000);

		Submissions submissionsLoader = new Submissions(client);
		List<Submission> toHandle = new ArrayList<>();
		while(true) {
			logger.info("Scanning for new posts...");

			List<Submission> submissions = null;
			try {
				submissions = submissionsLoader.getSubmissions(SUBREDDIT, Popularity.NEW, null, user);
			} catch (IOException | ParseException e1) {
				logger.catching(e1);
				sleepFor(10000);
				continue;
			}
			toHandle.clear();
			for(Submission sm : submissions) {
				boolean handled = false;
				for(int i = alreadyHandledPosts.size() - 1; i >= 0; i--) {
					if(alreadyHandledPosts.get(i).fullId.equals(sm.getFullName())) {
						handled = true;
						break;
					}else if(alreadyHandledPosts.get(i).timestamp < sm.createdUTC() - 60) { // Give 60 second leeway incase reddit does something funky
						break;
					}
				}

				if(!handled) {
					toHandle.add(sm);
				}
			}

			logger.info("Found " + toHandle.size() + " new posts.");
			if(toHandle.size() > 0) {
				for(Submission sm : toHandle) {
					String subreddit = null, title = null;
					try {
						subreddit = TrendingRedditUtils.getSubredditFromTitle(title = sm.getTitle());
					} catch (IOException | ParseException e) {
						logger.error("This should never happen; subreddit title should be cached");
						logger.throwing(e);
						System.exit(1);
					}
					if(subreddit == null) {
						logger.warn("Ignoring " + sm + "; No subreddit in title");
					}else {
						logger.info("Responding to \"" + title + "\"");
						String text = TrendingRedditUtils.getCommentString(commentFormat, subreddit);
						
						int exponential = 1;
						while(true) {
//							try {
								sm.setRestClient(client);
//								sm.comment(user, text);
								logger.info("Succesfully responded");
								break;
//							} catch (IOException | ParseException e) {
//								logger.catching(e);
//								int timeSeconds = (int) (5 * (Math.pow(2, exponential)));
//								logger.warn("Retrying in " + timeSeconds + " seconds");
//								sleepFor(timeSeconds * 1000);
//								exponential++;
//							}
						}
					}

					HandledPost hp = new HandledPost();
					hp.fullId = sm.getFullName();
					hp.timestamp = sm.createdUTC();
					alreadyHandledPosts.add(hp);
					Collections.sort(alreadyHandledPosts, HANDLED_POSTS_COMPARATOR); // Pretty much instant for a fully sorted array
					saveRememberedPosts();
					sleepFor(10000);
				}
				
				logger.info("Done responding to posts");
			}else {
				logger.info("Sleeping for 30 seconds while waiting for new posts");
				sleepFor(30000);
			}
		}
	}

	protected void loadConfiguration() {
		File userInfoFile = new File(USER_INFO_FILE_JSON);
		if(!userInfoFile.exists()) {
			logger.error("Could not locate " + USER_INFO_FILE_JSON + "; please create this file in a format similiar to:\n" + USER_INFO_FORMAT_EXAMPLE);
			System.exit(1);
		}
		try(FileReader fr = new FileReader(userInfoFile)) {
			JSONObject jObj = (JSONObject) (new JSONParser().parse(fr));

			if(!jObj.containsKey("username")) {
				logger.error(USER_INFO_FILE_JSON + " is valid json but does not contain username!");
				System.exit(1);
			}

			if(!jObj.containsKey("password")) {
				logger.error(USER_INFO_FILE_JSON + " is valid json but does not contain password!");
				System.exit(1);
			}

			username = jObj.get("username").toString();
			password = jObj.get("password").toString();
		} catch (IOException e) {
			logger.error("An i/o related exception occurred, this shouldn't happen.");
			logger.throwing(e);
			System.exit(1);
		} catch (ParseException e) {
			logger.error("Invalid format for " + USER_INFO_FILE_JSON + " (" + e.getMessage() + "); it should be similiar to:\n" + USER_INFO_FORMAT_EXAMPLE);
			System.exit(1);
		}

		File formatFile = new File(COMMENT_FORMAT_TEXT);
		if(!formatFile.exists()) {
			logger.warn(COMMENT_FORMAT_TEXT + " does not exist. If you would like to modify the comments text from the default (Subreddit: /r/%subreddit%), " +
					"create " + COMMENT_FORMAT_TEXT + " with the format of the response. Use %subreddit% in place of the name of the subreddit referred to " +
					"in the post");
			commentFormat = "Subreddit: /r/%subreddit%";
		}else {
			try(BufferedReader br = new BufferedReader(new FileReader(formatFile))) {
				commentFormat = br.readLine();
				String ln;
				while((ln = br.readLine()) != null) {
					commentFormat += "\n" + ln;
				}
			} catch (IOException e) {
				logger.error("An i/o related exception occurred (2), this shouldn't happen.");
				logger.throwing(e);
				System.exit(1);
			}

			if(!commentFormat.contains("%subreddit%")) {
				logger.warn("Comments will always be the same! Use %subreddit% for the subreddit being referred to, e.g. \"Subreddit: /r/%subreddit%\"");
			}
		}
	}

	protected void loadRememberedPosts() {
		alreadyHandledPosts = new ArrayList<>();

		File remPostsJson = new File(REMEMBERED_POSTS_JSON);
		if(!remPostsJson.exists())
			return;

		try(FileReader fr = new FileReader(remPostsJson)) {
			JSONArray jArr = (JSONArray) (new JSONParser().parse(fr));
			for(Object o : jArr) {
				if(!(o instanceof JSONObject)) {
					logger.error("Unexpected object type: " + o.getClass().getCanonicalName());
					System.exit(1);
				}

				JSONObject jObj = (JSONObject) o;

				HandledPost hp = new HandledPost();
				hp.fullId = (String) jObj.get("full_id");
				hp.timestamp = ((Number) jObj.get("timestamp")).doubleValue();

				alreadyHandledPosts.add(hp);
			}
		} catch (IOException e) {
			logger.error("An i/o related exception occurred (3), this shouldn't happen.");
			logger.throwing(e);
			System.exit(1);
		} catch (ParseException | ClassCastException | NullPointerException e) {
			logger.error("The remembered posts database is NOT valid json/format! What have you done!");
			logger.throwing(e);
			System.exit(1);
		}

		Collections.sort(alreadyHandledPosts, HANDLED_POSTS_COMPARATOR);
	}

	@SuppressWarnings("unchecked")
	protected void saveRememberedPosts() {
		File remPostsJson = new File(REMEMBERED_POSTS_JSON);
		JSONArray arr = new JSONArray();
		for(HandledPost hp : alreadyHandledPosts) {
			arr.add(hp.asObject());
		}
		try(FileWriter fw = new FileWriter(remPostsJson)) {
			JSONArray.writeJSONString(arr, fw);
		} catch (IOException e) {
			logger.error("An i/o related exception occurred (4), this shouldn't happen.");
			logger.throwing(e);
			System.exit(1);
		}
	}

	private void sleepFor(int ms) {
		try { Thread.sleep(ms); } catch(InterruptedException ie) { ie.printStackTrace(); System.exit(0); }
	}
}
