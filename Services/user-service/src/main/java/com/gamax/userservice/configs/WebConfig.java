package com.gamax.userservice.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/useruploads/**")
                .addResourceLocations("file:C:/Users/user/Desktop/Pi dev/projet dev gamemax/gestion-de-user-for-Pi-BACKEND--main/Services/user-service/uploads/");
    }
}