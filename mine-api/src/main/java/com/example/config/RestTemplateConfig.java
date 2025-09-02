package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 * 用于创建和配置RestTemplate实例，以支持HTTP请求
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * 创建RestTemplate Bean
     * 
     * @return 配置好的RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
