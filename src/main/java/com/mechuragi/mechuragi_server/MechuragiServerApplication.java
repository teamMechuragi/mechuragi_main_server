package com.mechuragi.mechuragi_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MechuragiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MechuragiServerApplication.class, args);
    }

}
