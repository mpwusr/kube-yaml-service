package com.example.kube;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import okhttp3.*;
import org.springframework.context.annotation.Bean;

/**
 * Main Spring Boot application entry point.
 *
 * This class bootstraps the Spring application context and exposes required beans
 * such as the OkHttpClient used for interacting with the Kubernetes API.
 */
@SpringBootApplication // Combines @Configuration, @EnableAutoConfiguration, and @ComponentScan
public class KubernetesYamlService {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args Command-line arguments (if any).
     */
    public static void main(String[] args) {
        SpringApplication.run(KubernetesYamlService.class, args);
    }

    /**
     * Defines a singleton OkHttpClient bean to be used across the application.
     * This is injected wherever OkHttpClient is required (e.g., in controllers or services).
     *
     * @return OkHttpClient instance.
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }
}
