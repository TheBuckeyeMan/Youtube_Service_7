package com.example.app.service;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.google.auth.oauth2.UserCredentials;

//Refresh token is access token
@Service
public class AccessToken {
    private static final Logger log = LoggerFactory.getLogger(AccessToken.class);
   // private S3LoggingService s3LoggingService;

    public AccessToken(){
        //this.s3LoggingService = s3LoggingService;
    }

    public String getAccessToken(String clientId, String clientSecret, String authUri, String tokenUri, String LongLivedToken) throws Exception{
        log.info("Attempting to get Refresh Token...");

        if(LongLivedToken == null){
            log.error("LongLivedAccessToken not present, please generate a long lived access token and store in an environment variable" );
            //TODO s3LoggingService.logMessageToS3("Error: LongLivedToken does not exist. Line 30 on AccessToken.java: " + LocalDate.now() + " On: youtube-service-5" + ",");
        }   
        try{
            UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(LongLivedToken)
                .setTokenServerUri(new java.net.URI(tokenUri))
                .build();

                String newToken = credentials.refreshAccessToken().getTokenValue();
                if(newToken == null){
                    log.error("Error: The Refresh Token returned null: Error Line 48 of AccessToken.java");
                  //  s3LoggingService.logMessageToS3("Error: The Refresh Token returned null: Error Line 48 of AccessToken.java: " + LocalDate.now() + " On: youtube-service-5" + ",");
                    throw new RuntimeException("Failed to refresh access token. Token is null or empty.");
                }
                log.info("New AccessToken has successfully been retrieved: " + newToken);
                return newToken;

        } catch (Exception e) {
            log.error("Error on line 48", e);
            //TODO s3LoggingService.logMessageToS3("Error: LongLivedToken does not exist. Line 30 on AccessToken.java: " + LocalDate.now() + " On: youtube-service-5" + ",");
            throw e;
        }
    }
}