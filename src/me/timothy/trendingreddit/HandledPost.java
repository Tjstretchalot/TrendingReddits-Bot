package me.timothy.trendingreddit;

import org.json.simple.JSONObject;

public class HandledPost {
	public String fullId;
	public double timestamp;
	
	@SuppressWarnings("unchecked")
	public JSONObject asObject() {
		JSONObject result = new JSONObject();
		result.put("full_id", fullId);
		result.put("timestamp", timestamp);
		return result;
	}
}
