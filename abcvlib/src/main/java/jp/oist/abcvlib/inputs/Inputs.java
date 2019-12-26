package jp.oist.abcvlib.inputs;

import android.content.SharedPreferences;
import android.media.MediaRecorder;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import jp.oist.abcvlib.AbcvlibActivity;
import jp.oist.abcvlib.inputs.audio.MicrophoneInput;
import jp.oist.abcvlib.inputs.vision.Vision;

import static android.content.Context.MODE_PRIVATE;

public class Inputs implements CameraBridgeViewBase.CvCameraViewListener2 {

    public MotionSensors motionSensors; // Doesn't need thread since handled by sensorManager or SensorService
    public Vision vision; // Doesn't need thread since this is started by CameraBridgeViewBase
    public QuadEncoders quadEncoders; // Doesnt need thread since AbcvlibLooper is handling this already
    public JSONObject stateVariables;
    public MicrophoneInput micInput;

    public Inputs(AbcvlibActivity abcvlibActivity){

        if (abcvlibActivity.switches.motionSensorApp){
            motionSensors = new MotionSensors(abcvlibActivity);
        }

        if (abcvlibActivity.switches.quadEncoderApp) {
            quadEncoders = new QuadEncoders();
        }

        if (abcvlibActivity.switches.cameraApp) {
            vision = new Vision(abcvlibActivity, 400, 240);
        }

        if (abcvlibActivity.switches.micApp){
            micInput = new MicrophoneInput(abcvlibActivity);
            micInput.start();
        }

        stateVariables = initializeStateVariables();

    }

    private JSONObject initializeStateVariables(){

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("timeAndroid", 0.0);
            jsonObject.put("theta", 0.0);
            jsonObject.put("thetaDot", 0.0);
            jsonObject.put("wheelCountL", 0.0);
            jsonObject.put("wheelCountR", 0.0);
            jsonObject.put("distanceL", 0.0);
            jsonObject.put("distanceR", 0.0);
            jsonObject.put("wheelSpeedL", 0.0);
            jsonObject.put("wheelSpeedR", 0.0);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }
    @Override
    public void onCameraViewStopped() {

    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return null;
    }

}
