package com.example.lotteryapp;

import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public final class NavigationHelper {

    private NavigationHelper() {}

    public static void openHistory(AppCompatActivity activity) {
        DeviceData deviceData = DeviceData.getInstance(activity);
        if (!deviceData.isLoggedIn()) {
            Toast.makeText(activity, "Please sign in to view history", Toast.LENGTH_SHORT).show();
            if (!(activity instanceof LoginActivity)) {
                activity.startActivity(new Intent(activity, LoginActivity.class));
            }
            return;
        }

        Intent intent = new Intent(activity, HistoryActivity.class);
        intent.putExtra("accountID", deviceData.getAccountID());
        activity.startActivity(intent);
    }
}
