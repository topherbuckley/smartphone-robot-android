package jp.oist.abcvlib.basic;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.List;

import jp.oist.abcvlib.AbcvlibActivity;

/**
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    ObjectDetector objectDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //switches.cameraApp = true;

        // Initalizes various objects in parent class.
        initialzer(this);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        // Live detection and tracking
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                        .build();

        objectDetector = ObjectDetection.getClient(options);

        YourAnalyzer yourAnalyzer = new YourAnalyzer();

    }

    private class YourAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(ImageProxy imageProxy) {
            @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                // Pass image to an ML Kit Vision API
                // ...
                objectDetector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<DetectedObject>>() {
                                    @Override
                                    public void onSuccess(List<DetectedObject> detectedObjects) {
                                        // Task completed successfully
                                        objectHandler(detectedObjects);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Log.d(TAG, "objectDetector failed to process image");
                                    }
                                });

            }
        }

        private void objectHandler(List<DetectedObject> detectedObjects){
            for (DetectedObject detectedObject : detectedObjects) {
                Rect boundingBox = detectedObject.getBoundingBox();
                Integer trackingId = detectedObject.getTrackingId();
                }
            }

        }


}

