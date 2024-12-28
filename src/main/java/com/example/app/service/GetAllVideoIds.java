package com.example.app.service;

import okhttp3.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GetAllVideoIds {
    private static final Logger log = LoggerFactory.getLogger(GetAllVideoIds.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public List<String> fetchVideoID(String accessToken, String channelId, String endpoint) {
        log.info("Beginning to gather new YouTube videos");
        List<String> videoIds = new ArrayList<>();

        try {
            OkHttpClient client = new OkHttpClient();

            // Build the request URL
            HttpUrl.Builder urlBuilder = HttpUrl.parse(endpoint).newBuilder();
            urlBuilder.addQueryParameter("part", "id");
            urlBuilder.addQueryParameter("channelId", channelId);
            urlBuilder.addQueryParameter("type", "video");
            urlBuilder.addQueryParameter("maxResults", "50");

            // Build the request
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            // Execute the request
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                log.error("Failed to fetch video IDs. HTTP Code: {}", response.code());
                log.error("Response Message: {}", response.body().string());
                return videoIds;
            }

            // Parse the response body
            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray items = jsonResponse.getAsJsonArray("items");

            // Extract video IDs
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                JsonObject idObject = item.getAsJsonObject("id");
                String videoId = idObject.get("videoId").getAsString();
                videoIds.add(videoId);
            }
            log.info("Successfully fetched {} video IDs.", videoIds.size());
        } catch (Exception e) {
            log.error("Error fetching YouTube video IDs", e);
        }

        return videoIds;
    }
}
