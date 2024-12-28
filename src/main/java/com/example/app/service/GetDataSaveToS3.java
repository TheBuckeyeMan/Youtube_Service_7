package com.example.app.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import okhttp3.*;
import com.google.common.net.MediaType;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class GetDataSaveToS3 {
    private static final Logger log = LoggerFactory.getLogger(GetDataSaveToS3.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final S3Client s3Client;

    public GetDataSaveToS3(S3Client s3Client){
        this.s3Client = s3Client;
    }

    //Method to orchestrate save of video to s3 bucket - the method called By ServiceTrigger
    public void saveVideos(List<String> videoIds, String accessToken, String videoEndpoint, String rawBucketName, String rawBucketKey){
        log.info("Begining to Save Videos to S3 Bucket");
        videoIds.forEach(videoId -> {
            try{
                //Get Video Data
                String videoDataJson = fetchVideoData(accessToken, videoId, videoEndpoint);

                //Flatten Video Data
                JsonObject flattenedData = flattenVideoData(videoDataJson, videoId);
                log.info("Serialized data for video {}: {}", videoId, flattenedData.toString());


                //Save contents to S3
                SaveToS3(videoId, flattenedData, rawBucketName, rawBucketKey);
                log.info("Video: " + videoId + " has been saved to the raw S3 bucket!");
            } catch (Exception e){
                log.error("Error saving video: " + videoId + " to the raw S3 bucket", e.getMessage());
            }
        });
        log.info("All New Videos have Successfully been saved to the S3 Bucket!");
    }

    //This method is responsable for making the request to the Youtube API for video and returns video data 
    private String fetchVideoData(String accessToken, String videoId, String videoEndpoint){
        log.info("Begining to fetch video data for video: " + videoId + " from Youtube API");

        OkHttpClient client = new OkHttpClient();

        //Build the request URL
        HttpUrl.Builder urlBuilder = HttpUrl.parse(videoEndpoint).newBuilder();
        urlBuilder.addQueryParameter("part", "snippet,statistics");
        urlBuilder.addQueryParameter("id", videoId);

        //Build the request itself
        Request request = new Request.Builder()
        .url(urlBuilder.build())
        .get()
        .addHeader("Authorization", "Bearer " + accessToken)
        .build();
        
        //Execute the request
        try (Response response = client.newCall(request).execute()){
            if (!response.isSuccessful()){
                throw new RuntimeException("Failed to fetch video data for video: " + videoId + " HTTP Code: " + response.code());
            }
            log.info("Successfully fetched data for video: " + videoId);
            //log.info("Response Body Is: " + response.body().string());
            return response.body().string();
        } catch (Exception e){
            throw new RuntimeException("Error fetching video data for video: " + videoId, e);
        }
    }

    //Method is responsable for flattening the video data and returning a jsonObject
    private JsonObject flattenVideoData(String videoDataJson, String videoId){
        log.info("Begining to flatten the video data for: " + videoId);
        try{
        JsonObject jsonResponse = JsonParser.parseString(videoDataJson).getAsJsonObject();
        JsonArray items = jsonResponse.getAsJsonArray("items");

        if (items.size() == 0){
            log.warn("No video data found in response");
            return null;
        }
        JsonObject video = items.get(0).getAsJsonObject();
        JsonObject snippet = video.getAsJsonObject("snippet");
        JsonObject statistics = video.getAsJsonObject("statistics");

        //Build the flattened object to save to S3 bucket
        JsonObject flattened = new JsonObject();
        flattened.addProperty("videoId", video.get("id").getAsString());
        flattened.addProperty("publishedAt", snippet.get("publishedAt").getAsString());
        flattened.addProperty("channelId", snippet.get("channelId").getAsString());
        flattened.addProperty("title", snippet.get("title").getAsString());
        flattened.addProperty("description", snippet.get("description").getAsString());
        flattened.addProperty("channelTitle", snippet.get("channelTitle").getAsString());
        flattened.add("tags", snippet.get("tags"));
        flattened.addProperty("categoryId", snippet.get("categoryId").getAsInt());
        flattened.addProperty("viewCount", statistics.get("viewCount").getAsInt());
        flattened.addProperty("likeCount", statistics.get("likeCount").getAsInt());
        flattened.addProperty("dislikeCount", statistics.get("dislikeCount").getAsInt());
        flattened.addProperty("commentCount", statistics.get("commentCount").getAsInt());

        log.info("Successfully Flattened the Json Data!");
        return flattened;
    } catch (Exception e){
        log.error("Error flattening video data for video: " + videoId, e);
        return null;
    }
    }

    //This method is responsable for taking out flatted json object and saving to s3 bucket
    private void SaveToS3(String videoId, Object flattenedData, String rawBucketName, String rawBucketKey){
        log.info("Saving: " + videoId + " to the Raw S3 Bucket");

        //Configure the File name
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = String.format("%s~%s.json", videoId, timestamp);
        String s3Key = rawBucketKey + fileName;

        try{
            //Create Put Object Request
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(rawBucketName)
            .key(s3Key)
            .build();

            //Upload file to S3 bucket
            s3Client.putObject(putObjectRequest,software.amazon.awssdk.core.sync.RequestBody.fromString(flattenedData.toString(), StandardCharsets.UTF_8));
        } catch (Exception e){
            log.error("Unable to upload video: " + videoId + " to the S3 Bucket", e.getMessage(), e);
        }
    }
}
