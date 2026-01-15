package slib.com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LibrarianConfig {

    /**
     * RestTemplate bean cho LibrarianService
     * Dùng để gọi Supabase Auth API
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
