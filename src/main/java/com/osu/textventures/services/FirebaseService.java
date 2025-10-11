package com.osu.textventures.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class FirebaseService {
    private final Firestore db = FirestoreClient.getFirestore();

    public void save(String id, String value) {
        db.collection("testCollection").document(id).set(new TestData(value));
    }

    public String get(String id) {
        DocumentReference docRef = db.collection("testCollection").document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                TestData data = document.toObject(TestData.class);
                return data != null ? data.value : null;
            } else {
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    public static class TestData {
        public String value;
        public TestData() {}
        public TestData(String value) { this.value = value; }
    }
}

