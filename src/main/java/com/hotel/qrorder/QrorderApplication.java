package com.hotel.qrorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QrorderApplication {
    public static void main(String[] args) {
        SpringApplication.run(QrorderApplication.class, args);
    }
}
