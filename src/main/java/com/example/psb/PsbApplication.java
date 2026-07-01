package com.example.psb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PsbApplication {

    public static void main(String[] args) {
        System.out.println(PsbApplication.class);


        SpringApplication.run(PsbApplication.class, args);
    }
}
