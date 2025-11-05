package com.osu.textventures.configs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.io.IOException;

@Component
public class FirebaseInit {

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount = FirebaseInit.class.getClassLoader().getResourceAsStream(serviceAccountPath);
            if (serviceAccount == null) {
                throw new IOException("Firebase service account key not found at path: " + serviceAccountPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            System.out.println("Firebase initialized!");
        }
    }
}
