package jp.oist.abcvlib.core.inputs.phone.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.oist.abcvlib.core.inputs.phone.vision.YuvToRgbConverter;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;
import jp.oist.abcvlib.util.ImageOps;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;

public class ImageData{

    private final TimeStepDataBuffer timeStepDataBuffer;
    private final YuvToRgbConverter yuvToRgbConverter;
    private ImageAnalysis imageAnalysis;
    private final ExecutorService imageExecutor;
    private boolean isRecording = false;

    public ImageData(Context context, TimeStepDataBuffer timeStepDataBuffer){
        this.timeStepDataBuffer = timeStepDataBuffer;
        yuvToRgbConverter = new YuvToRgbConverter(context);

        imageExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "imageAnalysis"));
        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(20)
                        .build();
        imageAnalysis.setAnalyzer(imageExecutor, this);
    }

    @androidx.camera.core.ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image image = null;
        if (isRecording()){
            image = imageProxy.getImage();
        } else {
            imageProxy.close();
            return;}
        if (image != null) {
            int width = image.getWidth();
            int height = image.getHeight();
            long timestamp = image.getTimestamp();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            yuvToRgbConverter.yuvToRgb(image, bitmap);

            ByteArrayOutputStream webpByteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 0, webpByteArrayOutputStream);
            byte[] webpBytes = webpByteArrayOutputStream.toByteArray();
            Bitmap webpBitMap = ImageOps.generateBitmap(webpBytes);

            timeStepDataBuffer.getWriteData().getImageData().add(timestamp, width, height, webpBitMap, webpBytes);
//            Log.v("flatbuff", "Wrote image to timeStepDataBuffer");
        }
        imageProxy.close();
    }

    public synchronized void setRecording(boolean recording) {
        isRecording = recording;
    }

    public synchronized boolean isRecording() {
        return isRecording;
    }

    public ImageAnalysis getImageAnalysis() {
        return imageAnalysis;
    }
}
