package com.campuslands.incidents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IncidentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentsApplication.class, args);
    }
}
