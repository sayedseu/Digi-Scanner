package com.dot.digiscanner.utils;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;

public class OCRUtils {

    public static String getTextFromBitmap( final Context context, Bitmap bitmap ) throws InterruptedException{

//        FirebaseApp.initializeApp(context);
//        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
//        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
//
//        Task<FirebaseVisionText> result =
//                detector.processImage(image)
//                        .addOnFailureListener(
//                                new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        e.printStackTrace();
//                                        Toast.makeText( context, "Could not make text vision work", Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//
//        while(! result.isComplete() ){
//            Thread.sleep(300);
//        }
//
//        return result.isSuccessful() ? result.getResult().getText() : null;

        return null;
    }
}
