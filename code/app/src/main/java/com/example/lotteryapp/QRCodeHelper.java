package com.example.lotteryapp;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * Creates QR bitmap images for public event links.
 */
public class QRCodeHelper {

    /**
     * Generates a QR code bitmap for a given event ID.
     *
     * @param eventId The ID of the event to encode into the QR code.
     * @return A Bitmap containing the generated QR code, or null if generation fails.
     */
    public static Bitmap generateQRCode(String eventId) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.encodeBitmap(
                    eventId,
                    BarcodeFormat.QR_CODE,
                    400,
                    400
            );
        } catch (Exception e) {
            Log.e("QR_GENERATION", "QR generation failed", e);
            return null;
        }
    }

    /**
     * Decodes a QR code from a given bitmap image.
     *
     * @param bitmap The bitmap image containing the QR code to decode.
     * @return The decoded string content (e.g., event ID), or null if decoding fails.
     */
    public static String decodeQRCode(Bitmap bitmap) {
        if (bitmap == null) return null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        com.google.zxing.RGBLuminanceSource source = new com.google.zxing.RGBLuminanceSource(width, height, pixels);
        com.google.zxing.BinaryBitmap binaryBitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
        com.google.zxing.qrcode.QRCodeReader reader = new com.google.zxing.qrcode.QRCodeReader();
        try {
            com.google.zxing.Result result = reader.decode(binaryBitmap);
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
