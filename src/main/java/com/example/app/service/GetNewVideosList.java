package com.example.app.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class GetNewVideosList {
    private static final Logger log = LoggerFactory.getLogger(GetNewVideosList.class);
    @Autowired
    private S3Client s3Client;


    public List<String> newVideos(List<String> allVideos, String rawBucketName, String rawBucketKey){
        log.info("Begining to create list of only new videos");

        List<String> existingVideos = getExistingVideoIds(rawBucketName, rawBucketKey);
        List<String> newVideos = new ArrayList<>();

        for (String videoId : allVideos){
            if (!existingVideos.contains(videoId)){
                newVideos.add(videoId);
            } else {
                log.info("Video ID Already Exists in S3. We are skipping: ", videoId);
            }
        }
        log.info("Found {} new videos to save.", newVideos.size());
        log.info("The list of the new videos we are going to save is: " + newVideos);
        return newVideos;
    }


    private List<String> getExistingVideoIds(String rawBucketName, String rawBucketKey){
        List<String> existingVideos = new ArrayList<>();

        try{
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(rawBucketName)
                .prefix(rawBucketKey)
                .build();
            ListObjectsV2Response response;
            do {
                response = s3Client.listObjectsV2(listRequest);
                for (S3Object s3Object : response.contents()){
                    String key = s3Object.key();
                    String videoId = extractedVideoIdFromKey(key, rawBucketKey);
                    if (videoId != null) {
                        existingVideos.add(videoId);
                    }
                }
                listRequest = listRequest.toBuilder().continuationToken(response.nextContinuationToken()).build();
            } while (response.isTruncated()); 
        } catch (Exception e){
            log.error("Failed to get existing video IDs from S3", e);
        }
        return existingVideos;
    }

    private String extractedVideoIdFromKey(String key, String rawBuckeyKey){
        if (key.startsWith(rawBuckeyKey)){
            String fileName = key.substring(rawBuckeyKey.length());
            int dashIndex = fileName.indexOf("-");
            if(dashIndex > 0){
                return fileName.substring(0, dashIndex);
            }
        }
        return null;
    }



}
