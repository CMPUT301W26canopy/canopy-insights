package com.example.lotteryapp;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRCodeHelper {

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