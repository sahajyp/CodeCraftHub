package com.codecrafthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the CodeCraftHub API.
 * {@code @SpringBootApplication} turns on component scanning for the
 * controller, model, service, exception and config packages below this one.
 */
@SpringBootApplication
public class CodeCraftHubApplication {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("CodeCraftHub API is starting...");
        System.out.println("============================================================");
        System.out.println("Data will be stored in: courses.json");
        System.out.println("API will be available at: http://localhost:8080");
        System.out.println("============================================================");
        System.out.println("\nPress CTRL+C to stop the server\n");

        SpringApplication.run(CodeCraftHubApplication.class, args);
    }
}
