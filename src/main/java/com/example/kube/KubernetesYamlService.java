package com.example.kube;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import okhttp3.*;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KubernetesYamlService {
    public static void main(String[] args) {
        SpringApplication.run(KubernetesYamlService.class, args);
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }
}
