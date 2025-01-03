package com.example.app.service;


import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceTrigger {
    private static final Logger log = LoggerFactory.getLogger(ServiceTrigger.class);
    private AccessToken AccessToken;
    private GetAllVideoIds GetAllVideoIds;
    private GetDataSaveToS3 getDataSaveToS3;

//TODO Host this on AWS ECS if we plan on selling this service
//Lambda will only get us like 2k records

    @Value("${spring.profiles.active}")
    private String environment;

    @Value("${google.api.clientid}")
    private String clientId;

    @Value("${google.api.clientsecret}")
    private String clientSecret;

    @Value("${google.api.authuri}")
    private String authUri;

    @Value("${google.api.tokenuri}")
    private String tokenUri;

    @Value("${google.api.certurl}")
    private String certUrl;

    @Value("${google.api.redirecturi}")
    private String redirectUri;

    @Value("${google.api.token}")
    private String LongLivedToken;

    @Value("${youtube.channel.channelid}")
    private String channelId;

    @Value("${youtube.data.api.endpoint}")
    private String endpoint;

    @Value("${aws.s3.bucket.raw}")
    String rawBucketName;

    @Value("${aws.s3.key.raw}")
    String rawBucketKey;

    @Value("${youtube.data.api.videoendpoint}")
    private String videoEndpoint;

    public ServiceTrigger(AccessToken AccessToken, GetAllVideoIds GetAllVideoIds, GetDataSaveToS3 getDataSaveToS3){
        this.AccessToken = AccessToken;
        this.GetAllVideoIds = GetAllVideoIds;
        this.getDataSaveToS3 = getDataSaveToS3;
    }

    public void TriggerService(){
        //Initialization Logs
        log.info("The Active Environment is set to: " + environment);
        log.info("Begining to download the Youtube Video Data");
        try{

        //Trigger Services
        //1. Get refresh Token
        String accessToken = AccessToken.getAccessToken(clientId, clientSecret, authUri, tokenUri, LongLivedToken);

        //2. Get List of all video ID's
        List<String> allVideos = GetAllVideoIds.fetchVideoID(accessToken, channelId, endpoint);

        //3. Make Request to video endpoint for each video and save to S3 as a new file in format<videoId>.json
        getDataSaveToS3.saveVideos(allVideos, accessToken, videoEndpoint, rawBucketName, rawBucketKey);

        //Log Completion
        log.info("The total number of videos saved is: " + allVideos.size());
        log.info("The Service has successfully complete, new data has been landed in the landing bucket!");
        log.info("Final: The Lambda has triggered successfully and the data is now saved!");

        //TODO Task 6: implement unit testing

        } catch (Exception e){
            log.error("Error Triggering Service:", e);
            //TODO s3LoggingService.logMessageToS3("Succcess: Success occured at: " + LocalDateTime.now() + " On: youtube-service-5" + ",");
        }
    }
}