package com.osu.textventures;

import com.osu.textventures.services.FirebaseService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class FirebaseServiceTest {

    @Autowired
    private FirebaseService firebaseService;

    @Test
    public void testFirebaseConnection() {
        firebaseService.save("testId", "testValue");
        String value = firebaseService.get("testId");
        assertEquals("testValue", value);
    }
}
