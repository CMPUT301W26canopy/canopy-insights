package com.example.lotteryapp;

import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CountAccountsTest {
    @Test
    public void countAccounts() throws Exception {
        QuerySnapshot snapshot = Tasks.await(FirestoreHelper.getDb().collection("accounts").get());
        Log.d("FIREBASE_ACCOUNTS", "Total accounts: " + snapshot.size());
        for (QueryDocumentSnapshot doc : snapshot) {
            Log.d("FIREBASE_ACCOUNTS", "ID: " + doc.getId() + " => Data: " + doc.getData());
        }
    }
}