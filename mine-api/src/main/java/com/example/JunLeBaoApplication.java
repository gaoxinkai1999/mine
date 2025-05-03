package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableScheduling // 启用定时任务
public class JunLeBaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(JunLeBaoApplication.class, args);
    }

    /**
     * 配置 WebClient.Builder Bean，供 UpdateService 注入使用。
     * @return WebClient.Builder 实例
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }


}
