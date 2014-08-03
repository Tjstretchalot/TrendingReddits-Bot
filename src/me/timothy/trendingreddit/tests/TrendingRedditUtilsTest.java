package me.timothy.trendingreddit.tests;

import static org.junit.Assert.*;

import me.timothy.trendingreddit.TrendingRedditUtils;

import org.junit.Test;

public class TrendingRedditUtilsTest {

	@Test
	public void testGetSubredditFromUtils() {
		final String[][] testCases = new String[][] {
				{
					"Just the subreddit in title",
					"/r/happycrowds",
					"happycrowds"
				},
				{
					"Something before subreddit in title",
					"[TRENDING] /r/happycrowds",
					"happycrowds"
				},
				{
					"Something after subreddit in title",
					"/r/banana is getting pretty popular",
					"banana"
				},
				{
					"Multiple subreddits",
					"Today, /r/alakazam and /r/wtf have a contest",
					"alakazam"
				},
				{
					"Empty subreddit followed by real subreddit",
					"Today, /r/ and /r/archers did well.",
					"archers"
				},
				{
					"Null title",
					null,
					null
				},
				{
					"Title without subreddit",
					"[META] Nothing to see here",
					null
				},
				{
					"Missing subreddit after /r/",
					"[META] Please prefix with /r/",
					null
				}
		};
		
		for(String[] test : testCases) {
			String result = TrendingRedditUtils.getSubredditFromTitle(test[1]);
			assertEquals(test[0], test[2], result);
		}
	}
	
	@Test
	public void testGetCommentText() {
		final String[][] testCases = new String[][] {
				{
					"Basic use-case",
					"Link: /r/%subreddit%",
					"banana",
					"Link: /r/banana"
				}
		};
		
		for(String[] test : testCases) {
			String result = TrendingRedditUtils.getCommentString(test[1], test[2]);
			assertEquals(test[0], test[3], result);
		}
	}

}
