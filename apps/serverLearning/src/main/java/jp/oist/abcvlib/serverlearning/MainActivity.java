package jp.oist.abcvlib.serverlearning;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioTimestamp;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.util.Size;
import jp.oist.abcvlib.core.learning.flatbuffers.*;
import com.google.flatbuffers.FlatBufferBuilder;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.audio.MicrophoneInput;
import jp.oist.abcvlib.core.inputs.vision.YuvToRgbConverter;
import jp.oist.abcvlib.core.outputs.SocketListener;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;

public class MainActivity extends AbcvlibActivity implements SocketListener {

    private TimeStepDataBuffer timeStepDataBuffer;
    private MicrophoneInput microphoneInput;

    ScheduledExecutorService executor;
    ExecutorService imageExecutor;
    ScheduledExecutorService imageAnalysisExecutor;
    ImageAnalysis imageAnalysis;
    ScheduledFuture<?> wheelDataGathererFuture;
    ScheduledFuture<?> chargerDataGathererFuture;
    ScheduledFuture<?> batteryDataGathererFuture;
    ScheduledFuture<?> timeStepDataAssemblerFuture;
    WheelDataGatherer wheelDataGatherer;
    ChargerDataGatherer chargerDataGatherer;
    BatteryDataGatherer batteryDataGatherer;
    TimeStepDataAssembler timeStepDataAssemblerExecutor;

    java.nio.ByteBuffer Fbuf;
    byte[] byteBuff;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup a live preview of camera feed to the display. Remove if unwanted.
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

        switches.pythonControlledPIDBalancer = true;
        switches.cameraXApp = true;

        timeStepDataBuffer = new TimeStepDataBuffer(3);

        int threads = 2;
        executor = Executors.newScheduledThreadPool(threads, new ProcessPriorityThreadFactory(1, "dataGatherer"));
        imageExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(10, "imageAnalysis"));

        microphoneInput = new MicrophoneInput(this);

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(2)
                        .build();
        imageAnalysis.setAnalyzer(imageExecutor, new ImageDataGatherer());

        //todo I guess the imageAnalyzerActivity Interface is uncessary
        initialzer(this, "192.168.28.233", 3000, null, this, this);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onSetupFinished(){
//        testFlatBuffers();
        wheelDataGathererFuture = executor.scheduleAtFixedRate(new WheelDataGatherer(), 0, 100, TimeUnit.MILLISECONDS);
        chargerDataGathererFuture = executor.scheduleAtFixedRate(new ChargerDataGatherer(), 0, 100, TimeUnit.MILLISECONDS);
        batteryDataGathererFuture = executor.scheduleAtFixedRate(new BatteryDataGatherer(), 0, 100, TimeUnit.MILLISECONDS);
        timeStepDataAssemblerFuture = executor.scheduleAtFixedRate(new TimeStepDataAssembler(), 0,500, TimeUnit.MILLISECONDS);
        microphoneInput.start();
    }

//    private void testFlatBuffers(){
//        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
//
//        int ra1 = RobotAction.createRobotAction(builder, (byte) 5, (byte) 16);
//        int[] ra = {ra1};
//        int aV = TimeStep.createActionsVector(builder, ra);
//
//        int ts1 = TimeStep.createTimeStep(builder, aV);
//        int[] ts = {ts1};
//        int tsV = Episode.createTimestepsVector(builder, ts);
//
//        Episode.startEpisode(builder);
//        long[] timestamps = new long[]{1, 2, 3};
//        double[] left = new double[]{1,2,3};
//        double[] right = new double[]{1,2,3};
//
//        int ts = WheelCounts.createTimestampsVector(builder, timestamps);
//        int l = WheelCounts.createLeftVector(builder, left);
//        int r = WheelCounts.createRightVector(builder, right);
//        WheelCounts.createWheelCounts(builder, ts, l, r);
//        int episode = Episode.endEpisode(builder);
//
//        builder.finish(episode); // You could also call `Monster.finishMonsterBuffer(builder, orc);`.
//        // This must be called after `finish()`.
//        byteBuff = builder.sizedByteArray();
//        Log.i("alskdjasd", "I'm a breakpoint!");
//    }

    class WheelDataGatherer implements Runnable{
        ArrayList<Long> timestamps = new ArrayList<>();
        ArrayList<Double> left = new ArrayList<>();
        ArrayList<Double> right = new ArrayList<>();

        @Override
        public void run() {
            timestamps.add(System.nanoTime());
            left.add(inputs.quadEncoders.getWheelCountL());
            right.add(inputs.quadEncoders.getWheelCountR());
        }
        public long[] getTimeStamps(){
            long[] timestampslong = new long[timestamps.size()];
            for (int i=0 ; i <= timestamps.size() ; i++){
                timestampslong[i] = timestamps.get(i);
            }
            return timestampslong;
        }
    }

    class ChargerDataGatherer implements Runnable{
        @Override
        public void run() {
            timeStepDataBuffer.writeData.chargerData.put(inputs.battery.getVoltageCharger());
        }
    }

    class BatteryDataGatherer implements Runnable{
        @Override
        public void run() {
            timeStepDataBuffer.writeData.batteryData.put(inputs.battery.getVoltageBatt());
        }
    }

    class ImageDataGatherer implements ImageAnalysis.Analyzer{

        YuvToRgbConverter yuvToRgbConverter = new YuvToRgbConverter(getApplicationContext());

        @androidx.camera.core.ExperimentalGetImage
        public void analyze(@NonNull ImageProxy imageProxy) {
            Image image = imageProxy.getImage();
            if (image != null && timeStepDataBuffer.writeData.imageData.images.size() < 1) {
                int width = image.getWidth();
                int height = image.getHeight();
                long timestamp = image.getTimestamp();

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                yuvToRgbConverter.yuvToRgb(image, bitmap);

                int[] intFrame = new int[width * height];
                bitmap.getPixels(intFrame, 0, width, 0, 0, width, height);

                // convert bitmap to three byte[] with rgb.
                Bitmap2RGBVectors bitmap2RGBVectors = new Bitmap2RGBVectors(bitmap);
                int[][] rgbVectors = bitmap2RGBVectors.getRGBVectors();

                // todo this is causing a memory leak and crashing.
                timeStepDataBuffer.writeData.imageData.add(timestamp, width, height, rgbVectors);
            }
            imageProxy.close();
        }
    }

    static class Bitmap2RGBVectors {

        int[][] rgbVectors;

        public Bitmap2RGBVectors(Bitmap bitmap){

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int size = width * height;

            int[] r = new int[size];
            int[] g = new int[size];
            int[] b = new int[size];

            for(int y = 0; y < height; y++){
                for(int x = 0 ; x < width ; x++){
                    int pixel = bitmap.getPixel(x,y);
                    r[(x + (y * width))] = Color.red(pixel);
                    g[(x + (y * width))] = Color.green(pixel);
                    b[(x + (y * width))] = Color.blue(pixel);
                }
            }
            rgbVectors = new int[3][size];

            rgbVectors[0] = r;
            rgbVectors[1] = g;
            rgbVectors[2] = b;
        }

        public int[][] getRGBVectors(){
            return rgbVectors;
        }
    }

    class TimeStepDataAssembler implements Runnable{

        private int timeStep = 0;
        private JsonWriter writer;
//        private Gson gson = new GsonBuilder().create();
        private FileOutputStream fileOutputStream;
        private OutputStreamWriter outputStreamWriter;
        private int maxTimeStep = 5;

        @Override
        public void run() {

            MyStepHandler myStepHandler = new MyStepHandler(timeStepDataBuffer.writeData);
            myStepHandler.foward();

            FlatBufferBuilder builder = new FlatBufferBuilder(1024);
            WheelCounts.createTimestampsVector(builder, wheelDataGatherer.getTimeStamps());

            Log.i("datagatherer", "start of logger run");

            // Don't put these inline, else you will pass by reference rather than value and references will continue to update
            AudioTimestamp startTime = microphoneInput.getStartTime();
            AudioTimestamp endTime = microphoneInput.getEndTime();
            int sampleRate = microphoneInput.getSampleRate();
            timeStepDataBuffer.writeData.soundData.setMetaData(sampleRate, startTime, endTime);

            microphoneInput.setStartTime();

            timeStepDataBuffer.nextTimeStep();


            Log.i("datagatherer", "1");
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Log.i("datagatherer", "2");
                File file = new File(getExternalFilesDir(null), "test.json");
                Log.i("datagatherer", "3");
                try {
                    if (file.exists() && timeStep == 0) {
                        Log.i("datagatherer", "4");
                        file.delete();
                        file.createNewFile();
                        Log.i("datagatherer", "4.1");
                        fileOutputStream = new FileOutputStream(file, true);
                        outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                        writer = new JsonWriter(outputStreamWriter);
                        writer.beginArray();
                    }
                    if (file.exists() && file.canRead()) {
                        Log.i("datagatherer", "5");
                        TimeStepDataBuffer.TimeStepData data = timeStepDataBuffer.readData;
                        Gson gson = new Gson();
                        gson.toJson(data, outputStreamWriter);
                        gson = null;
                        Log.i("datagatherer", "6");
                        if (timeStep != maxTimeStep) {
                            outputStreamWriter.append(",");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (timeStep == maxTimeStep){
                try {
                    writer.endArray();
                    writer.close();
                    closeall();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                timeStepDataAssemblerFuture.cancel(true);
            }
            timeStep++;
        }

        public void closeall(){
            wheelDataGathererFuture.cancel(true);
            chargerDataGathererFuture.cancel(true);
            batteryDataGathererFuture.cancel(true);
            imageAnalysis.clearAnalyzer();
            microphoneInput.stop();
            microphoneInput.close();
        }
    }

    // Passes custom ImageAnalysis object to core CameraX lib to bind to lifecycle, and other admin functions
    @Override
    public ImageAnalysis getAnalyzer() {
        return imageAnalysis;
    }


    @Override
    protected void newAudioData(float[] audioData, int numSamples){
        timeStepDataBuffer.writeData.soundData.add(audioData, numSamples);

    }

    @Override
    public void onServerReadSuccess(JSONObject msgFromServer) {
        // Parse Message from Server
        // ..
        Log.i("server", msgFromServer.toString());

        // Send return message
        sendToServer();
    }

    /**
     * Assemble message to server and send.
     */
    private void sendToServer(){
        this.outputs.socketClient.writeFlatBufferToServer(byteBuff);
    }
}
