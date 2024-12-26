package com.example.app.service;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceTrigger {
    private static final Logger log = LoggerFactory.getLogger(ServiceTrigger.class);

    @Value("${spring.profiles.active}")
    private String environment;

    public ServiceTrigger(){

    }

    public void TriggerService(){
        //Initialization Logs
        log.info("The Active Environment is set to: " + environment);
        log.info("Begining to Collect Contents of Fun Fact form S3 Bucket");

        //Trigger Services

        //2. Get refresh Token


        //3. Authenticate with Youtube


        //4. Pull Data save to Json file, return Json file

        //5. Save the file to raw s3 bucket

        //Log Completion
        log.info("The Service has successfully complete, new data has been landed in the landing bucket!");
        log.info("Final: The Lambda has triggered successfully and the data is now saved!");

        //TODO Task 6: implement unit testing

        //TODO Figure out how to havethis log to logging bucket without causing a failure 
    }
}