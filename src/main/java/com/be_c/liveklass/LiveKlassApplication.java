package com.be_c.liveklass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LiveKlassApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveKlassApplication.class, args);
    }
}