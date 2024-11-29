package com.dashboard.NMS.confiq;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Allow requests from localhost:3000 (or other domains as needed)
        registry.addMapping("/fetch/**")
                .allowedOrigins("http://localhost:3000")  // Change this to the allowed frontend origin
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // Allow specific methods
                .allowedHeaders("*");  // Allow all headers
    }
}
