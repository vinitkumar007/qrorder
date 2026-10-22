package com.hotel.qrorder.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Upload ki hui dish photos /uploads/** se serve hoti hain. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Upload folder nahi ban paya: " + dir, e);
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(dir.toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(7)));
    }
}
