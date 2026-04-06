package com.example.lotteryapp;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Helper class for Firestore database operations.
 */
public class FirestoreHelper {

    private static FirebaseFirestore db;

    /**
     * Returns the singleton instance of FirebaseFirestore.
     * @return The {@link FirebaseFirestore} instance.
     */
    public static FirebaseFirestore getDb() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }
}
