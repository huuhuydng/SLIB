package slib.com.example.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Configuration
 * Initializes Firebase Admin SDK for sending push notifications
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.config.path:firebase-service-account.json}")
    private String firebaseConfigPath;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:}")
    private String googleApplicationCredentials;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = resolveServiceAccountStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            }
        } catch (IOException e) {
            log.warn("Firebase initialization failed: {}", e.getMessage());
            log.warn("Push notifications will not work until a valid Firebase service account is provided");
        }
    }

    private InputStream resolveServiceAccountStream() throws IOException {
        if (StringUtils.hasText(googleApplicationCredentials)) {
            log.info("Loading Firebase credentials from GOOGLE_APPLICATION_CREDENTIALS: {}", googleApplicationCredentials);
            return new FileInputStream(googleApplicationCredentials);
        }

        log.info("Loading Firebase credentials from firebase.config.path: {}", firebaseConfigPath);
        return new ClassPathResource(firebaseConfigPath).getInputStream();
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (FirebaseApp.getApps().isEmpty()) {
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
