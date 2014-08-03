package me.timothy.trendingreddit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is where the logic of the program might be editted in the future.
 * Everything in this class has a JTest
 * 
 * @author Timothy
 */
public class TrendingRedditUtils {
	private static final Pattern SUBREDDIT_NAME_PATTERN = Pattern.compile("/r/[^\\s]+");
	/**
	 * Gets the subreddit, if there is one, from a title . Subreddit name
	 * must be prefixed with /r/ and have no spaces. If there are two
	 * subreddits, only the first one is returned
	 * 
	 * @param title The title to search
	 * @return The subreddit or null
	 */
	public static String getSubredditFromTitle(String title) {
		if(title == null)
			return null;
		Matcher matcher = SUBREDDIT_NAME_PATTERN.matcher(title);
		while(matcher.find()) {
			String sub = matcher.group();
			if(sub.trim().length() > 3)
				return sub.substring(3);
		}
		return null;
	}
	
	/**
	 * Returns the appropriate comment for the subreddit
	 * @param format The format to use, where %subreddit% stands for the subreddit
	 * @param subreddit the subreddit to replace
	 * @return The full comment text
	 */
	public static String getCommentString(String format, String subreddit) {
		return format.replace("%subreddit%", subreddit);
	}
}
