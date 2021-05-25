package jp.oist.abcvlib.basic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.AbcvlibInput;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataListener;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataListener;
import jp.oist.abcvlib.core.inputs.phone.ImageData;
import jp.oist.abcvlib.core.inputs.phone.ImageDataListener;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneDataListener;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.inputs.phone.OrientationDataListener;

/**
 * Most basic Android application showing connection to IOIOBoard and Android Sensors
 * Shows basics of setting up any standard Android Application framework, and a simple log output of
 * theta and angular velocity via Logcat using onboard Android sensors.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements BatteryDataListener,
        OrientationDataListener, WheelDataListener, MicrophoneDataListener, ImageDataListener {

    TextView voltageBatt;
    TextView voltageCharger;
    TextView tiltAngle;
    TextView angularVelocity;
    TextView leftWheel;
    TextView rightWheel;
    TextView soundData;
    TextView frameRateText;
    long lastFrameTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        setContentView(R.layout.activity_main);
        voltageBatt = findViewById(R.id.voltageBattLevel);
        voltageCharger = findViewById(R.id.voltageChargerLevel);
        tiltAngle = findViewById(R.id.tiltAngle);
        angularVelocity = findViewById(R.id.angularVelcoity);
        leftWheel = findViewById(R.id.leftWheelCount);
        rightWheel = findViewById(R.id.rightWheelCount);
        soundData = findViewById(R.id.soundData);
        frameRateText = findViewById(R.id.frameRate);
        lastFrameTime = System.nanoTime();

        String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        checkPermissions(permissions);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
    }

    private void checkPermissions(String[] permissions){
        boolean permissionsGranted = false;
        for (String permission:permissions){
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED){
                permissionsGranted = true;
            }else{
                permissionsGranted = false;
            }
        }
        if (permissionsGranted) {
            start();
        } else {
            requestPermissionLauncher.launch(permissions);
        }
    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                Iterator<Map.Entry<String, Boolean>> iterator = isGranted.entrySet().iterator();
                boolean allGranted = false;
                while(iterator.hasNext()){
                    Map.Entry<String, Boolean> pair = iterator.next();
                    allGranted = pair.getValue();
                }
                if (allGranted) {
                    Log.i(TAG, "Permissions granted");
                    start();
                } else {
                    Log.i(TAG, "Permissions denied");
                }
            });

    private void start(){
        // Initalizes various objects in parent class.
        MicrophoneData microphoneData = new MicrophoneData(this);
        ImageData imageData = new ImageData(this);
        ArrayList<AbcvlibInput> inputArrayList = new ArrayList<>();
        inputArrayList.add(microphoneData);
        inputArrayList.add(imageData);
        initializer(this, null, null, inputArrayList, null);
        getInputs().getBatteryData().setBatteryDataListener(this);
        getInputs().getOrientationData().setOrientationDataListener(this);
        getInputs().getWheelData().setWheelDataListener(this);
        getInputs().getMicrophoneData().setMicrophoneDataListener(this);
        getInputs().getImageData().setImageDataListenerTest(this);
    }

    @Override
    public void onBatteryVoltageUpdate(double voltage, long timestamp) {
//        Log.i(TAG, "Battery Update: Voltage=" + voltage + " Timestemp=" + timestamp);
        DecimalFormat df = new DecimalFormat("#.00");
        runOnUiThread(() -> voltageBatt.setText(df.format(voltage)));
    }

    @Override
    public void onChargerVoltageUpdate(double voltage, long timestamp) {
//        Log.i(TAG, "Charger Update: Voltage=" + voltage + " Timestemp=" + timestamp);
        DecimalFormat df = new DecimalFormat("#.00");
        runOnUiThread(() -> voltageCharger.setText(df.format(voltage)));
    }

    @Override
    public void onOrientationUpdate(long timestamp, double thetaRad, double angularVelocityRad) {
//        Log.i(TAG, "Orientation Data Update: Timestamp=" + timestamp + " thetaRad=" + thetaRad
//                + " angularVelocity=" + angularVelocityRad);
//
        // You can also convert them to degrees using the following static utility methods.
        double thetaDeg = OrientationData.getThetaDeg(thetaRad);
        double angularVelocityDeg = OrientationData.getAngularVelocityDeg(angularVelocityRad);
        DecimalFormat df = new DecimalFormat("#.00");
        runOnUiThread(() -> {
                    tiltAngle.setText(df.format(thetaDeg));
                    angularVelocity.setText(df.format(angularVelocityDeg));
                }
        );
    }

    @Override
    public void onWheelDataUpdate(long timestamp, int countLeft, int countRight) {
//        Log.i(TAG, "Wheel Data Update: Timestamp=" + timestamp + " countLeft=" + countLeft +
//                " countRight=" + countRight);
//        double distanceLeft = WheelData.countsToDistance(countLeft);
        DecimalFormat df = new DecimalFormat("#.00");
        runOnUiThread(() -> {
            leftWheel.setText(df.format(countLeft));
            rightWheel.setText(df.format(countRight));
        });
    }

    @Override
    public void onMicrophoneDataUpdate(float[] audioData, int numSamples) {
        DecimalFormat df = new DecimalFormat("#.00");
        float[] arraySlice = Arrays.copyOfRange(audioData, 0, 9);
        String audioDataString = Arrays.toString(arraySlice);
//        Log.i(TAG, "Microphone Data Update: First 10 Samples=" + audioDataString +
//                 " of " + numSamples + " total samples");
        runOnUiThread(() -> soundData.setText(audioDataString));
    }

    @Override
    public void onImageDataUpdate(long timestamp, int width, int height, Bitmap bitmap, byte[] webpImage) {
//        Log.i(TAG, "Image Data Update: Timestamp=" + timestamp + " dims=" + width + " x "
//                + height);
        double frameRate = 1.0 / ((System.nanoTime() - lastFrameTime) / 1000000000.0);
        lastFrameTime = System.nanoTime();
        frameRate = Math.round(frameRate);
        String frameRateString = String.format(Locale.JAPAN,"%.0f", frameRate);
        runOnUiThread(() -> frameRateText.setText(frameRateString));
    }
}

