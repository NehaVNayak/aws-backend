package com.example.s3microservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.net.ServerSocket;

@SpringBootApplication
public class S3MicroserviceApplication {

    public static void main(String[] args) {
        loadEnvironmentVariables();
        resolveServerPort();
        SpringApplication.run(S3MicroserviceApplication.class, args);
    }

    private static void loadEnvironmentVariables() {
        // Try parent directory first (repo root), then current directory
        String[] possiblePaths = {"../", "./"};
        for (String path : possiblePaths) {
            File envFile = new File(path, ".env");
            if (envFile.exists()) {
                try {
                    Dotenv dotenv = Dotenv.configure()
                            .directory(path)
                            .ignoreIfMissing()
                            .load();
                    dotenv.entries().forEach(entry -> {
                        if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                            System.setProperty(entry.getKey(), entry.getValue());
                        }
                    });
                    System.out.println(">>> Loaded .env file from: " + envFile.getAbsolutePath());
                    break;
                } catch (Exception e) {
                    System.err.println("Notice: Could not load .env from " + path + ": " + e.getMessage());
                }
            }
        }
    }

    private static void resolveServerPort() {
        String envPort = System.getProperty("SERVER_PORT");
        int chosenPort = 8085;

        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                int requested = Integer.parseInt(envPort.trim());
                if (isPortAvailable(requested)) {
                    chosenPort = requested;
                } else {
                    System.out.println(">>> [Port Notice]: Port " + requested + " is currently in use by another system service (such as Jenkins). Auto-selecting port 8085 for the S3 microservice.");
                    chosenPort = 8085;
                }
            } catch (NumberFormatException e) {
                chosenPort = 8085;
            }
        }

        System.setProperty("server.port", String.valueOf(chosenPort));
        System.out.println(">>> S3 Microservice listening on port: " + chosenPort);
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket ss = new ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
