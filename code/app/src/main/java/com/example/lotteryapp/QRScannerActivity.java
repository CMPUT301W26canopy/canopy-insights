package com.example.lotteryapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class QRScannerActivity extends AppCompatActivity {

    // New Activity Result launcher
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                decodeQRCodeFromImage(imageUri);
                            }
                        } else {
                            Toast.makeText(this, "Image selection canceled.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Launch the image picker
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void decodeQRCodeFromImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            String eventId = QRCodeHelper.decodeQRCode(bitmap); // your helper function
            if (eventId != null) {
                Intent intent = new Intent(QRScannerActivity.this, EventActivity.class);
                intent.putExtra("EVENT_ID", eventId);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "No QR code found in image.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}