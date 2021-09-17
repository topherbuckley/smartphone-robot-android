package jp.oist.abcvlib.core.learning;

import android.util.Log;

import com.google.flatbuffers.FlatBufferBuilder;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.learning.fbclasses.AudioTimestamp;
import jp.oist.abcvlib.core.learning.fbclasses.BatteryData;
import jp.oist.abcvlib.core.learning.fbclasses.ChargerData;
import jp.oist.abcvlib.core.learning.fbclasses.Episode;
import jp.oist.abcvlib.core.learning.fbclasses.IndividualWheelData;
import jp.oist.abcvlib.core.learning.fbclasses.OrientationData;
import jp.oist.abcvlib.core.learning.fbclasses.RobotAction;
import jp.oist.abcvlib.core.learning.fbclasses.SoundData;
import jp.oist.abcvlib.core.learning.fbclasses.TimeStep;
import jp.oist.abcvlib.core.learning.fbclasses.WheelData;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;
import jp.oist.abcvlib.util.SocketConnectionManager;
import jp.oist.abcvlib.util.SocketListener;

/**
 * Enters data from TimeStepDataBuffer into a flatbuffer
 */
public class FlatbufferAssembler {

    private FlatBufferBuilder builder;
    private final int[] timeStepVector;
    private final Trial myTrial;
    private final TimeStepDataBuffer timeStepDataBuffer;
    private final ScheduledExecutorServiceWithException executor;
    private final InetSocketAddress inetSocketAddress;
    private final SocketListener socketListener;
    private ByteBuffer episode;
    private int robotID;

    public FlatbufferAssembler(Trial myTrial,
                                 InetSocketAddress inetSocketAddress,
                                 SocketListener socketListener,
                                 TimeStepDataBuffer timeStepDataBuffer,
                               int robotID){
        this.socketListener = socketListener;
        this.inetSocketAddress = inetSocketAddress;
        this.timeStepDataBuffer = timeStepDataBuffer;
        this.robotID = robotID;

        int threads = 5;
        executor = new ScheduledExecutorServiceWithException(threads, new ProcessPriorityThreadFactory(1, "flatbufferAssembler"));

        this.myTrial = myTrial;
        this.timeStepVector = new int[myTrial.getMaxTimeStepCount() + 1];
    }

    public TimeStepDataBuffer getTimeStepDataBuffer() {
        return timeStepDataBuffer;
    }

    public void startEpisode(){
        builder = new FlatBufferBuilder(1024);
        Log.v("flatbuff", "starting New Episode");
    }

    public void addTimeStep(){

        int _wheelData = addWheelData();
        int _orientationData = addOrientationData();
        int _chargerData = addChargerData();
        int _batteryData = addBatteryData();
        int _soundData = addSoundData();
        int _imageData = addImageData();
        int _actionData = addActionData();

        TimeStep.startTimeStep(builder);
        TimeStep.addWheelData(builder, _wheelData);
        TimeStep.addOrientationData(builder, _orientationData);
        TimeStep.addChargerData(builder, _chargerData);
        TimeStep.addBatteryData(builder, _batteryData);
        TimeStep.addSoundData(builder, _soundData);
        TimeStep.addImageData(builder, _imageData);
        TimeStep.addActions(builder, _actionData);
        int ts = TimeStep.endTimeStep(builder);
        timeStepVector[myTrial.getTimeStep()]  = ts;
    }

    private int addWheelData(){
        TimeStepDataBuffer.TimeStepData.WheelData.IndividualWheelData leftData =
                timeStepDataBuffer.getReadData().getWheelData().getLeft();
        Log.v("flatbuff", "STEP wheelCount TimeStamps Length: " +
                leftData.getTimeStamps().length);
        int timeStampsLeft = IndividualWheelData.createTimestampsVector(builder,
                leftData.getTimeStamps());
        int countsLeft = IndividualWheelData.createCountsVector(builder,
                leftData.getCounts());
        int distancesLeft = IndividualWheelData.createDistancesVector(builder,
                leftData.getDistances());
        int speedsLeftInstant = IndividualWheelData.createSpeedsInstantaneousVector(builder,
                leftData.getSpeedsInstantaneous());
        int speedsLeftBuffered = IndividualWheelData.createSpeedsBufferedVector(builder,
                leftData.getSpeedsBuffered());
        int speedsLeftExpAvg = IndividualWheelData.createSpeedsExpavgVector(builder,
                leftData.getSpeedsExpAvg());
        int leftOffset = IndividualWheelData.createIndividualWheelData(builder, timeStampsLeft,
                countsLeft, distancesLeft, speedsLeftInstant, speedsLeftBuffered, speedsLeftExpAvg);

        TimeStepDataBuffer.TimeStepData.WheelData.IndividualWheelData rightData =
                timeStepDataBuffer.getReadData().getWheelData().getRight();
        Log.v("flatbuff", "STEP wheelCount TimeStamps Length: " +
                rightData.getTimeStamps().length);
        int timeStampsRight = IndividualWheelData.createTimestampsVector(builder,
                rightData.getTimeStamps());
        int countsRight = IndividualWheelData.createCountsVector(builder,
                rightData.getCounts());
        int distancesRight = IndividualWheelData.createDistancesVector(builder,
                rightData.getDistances());
        int speedsRightInstant = IndividualWheelData.createSpeedsInstantaneousVector(builder,
                rightData.getSpeedsInstantaneous());
        int speedsRightBuffered = IndividualWheelData.createSpeedsBufferedVector(builder,
                rightData.getSpeedsBuffered());
        int speedsRightExpAvg = IndividualWheelData.createSpeedsExpavgVector(builder,
                rightData.getSpeedsExpAvg());
        int rightOffset = IndividualWheelData.createIndividualWheelData(builder, timeStampsRight,
                countsRight, distancesRight, speedsRightInstant, speedsRightBuffered, speedsRightExpAvg);

        return WheelData.createWheelData(builder, leftOffset, rightOffset);
    }

    private int addOrientationData(){
        Log.v("flatbuff", "STEP orientationData TimeStamps Length: " +
                timeStepDataBuffer.getReadData().getOrientationData().getTimeStamps().length);
        int ts = OrientationData.createTimestampsVector(builder,
                timeStepDataBuffer.getReadData().getOrientationData().getTimeStamps());
        int tiltAngles = OrientationData.createTiltangleVector(builder,
                timeStepDataBuffer.getReadData().getOrientationData().getTiltAngle());
        int tiltVelocityAngles = OrientationData.createTiltvelocityVector(builder,
                timeStepDataBuffer.getReadData().getOrientationData().getAngularVelocity());
        return OrientationData.createOrientationData(builder, ts, tiltAngles, tiltVelocityAngles);
    }

    private int addChargerData(){
        Log.v("flatbuff", "STEP chargerData TimeStamps Length: " +
                timeStepDataBuffer.getReadData().getChargerData().getTimeStamps().length);
        int ts = ChargerData.createTimestampsVector(builder,
                timeStepDataBuffer.getReadData().getChargerData().getTimeStamps());
        int voltage = ChargerData.createVoltageVector(builder,
                timeStepDataBuffer.getReadData().getChargerData().getChargerVoltage());
        return ChargerData.createChargerData(builder, ts, voltage);
    }

    private int addBatteryData(){
        Log.v("flatbuff", "STEP batteryData TimeStamps Length: " +
                timeStepDataBuffer.getReadData().getBatteryData().getTimeStamps().length);
        int ts = BatteryData.createTimestampsVector(builder,
                timeStepDataBuffer.getReadData().getBatteryData().getTimeStamps());
        int voltage = BatteryData.createVoltageVector(builder,
                timeStepDataBuffer.getReadData().getBatteryData().getVoltage());
        return ChargerData.createChargerData(builder, ts, voltage);
    }

    private int addSoundData(){

        TimeStepDataBuffer.TimeStepData.SoundData soundData = timeStepDataBuffer.getReadData().getSoundData();

        Log.v("flatbuff", "Sound Data TotalSamples: " +
                soundData.getTotalSamples());
        Log.v("flatbuff", "Sound Data totalSamplesCalculatedViaTime: " +
                soundData.getTotalSamplesCalculatedViaTime());

        int _startTime = AudioTimestamp.createAudioTimestamp(builder,
                soundData.getStartTime().framePosition,
                soundData.getStartTime().nanoTime);
        int _endTime = AudioTimestamp.createAudioTimestamp(builder,
                soundData.getStartTime().framePosition,
                soundData.getStartTime().nanoTime);
        int _levels = SoundData.createLevelsVector(builder,
                timeStepDataBuffer.getReadData().getSoundData().getLevels());

        SoundData.startSoundData(builder);
        SoundData.addStartTime(builder, _startTime);
        SoundData.addEndTime(builder, _endTime);
        SoundData.addTotalTime(builder, soundData.getTotalTime());
        SoundData.addSampleRate(builder, soundData.getSampleRate());
        SoundData.addTotalSamples(builder, soundData.getTotalSamples());
        SoundData.addLevels(builder, _levels);

        return SoundData.endSoundData(builder);
    }

    private int addImageData(){
        TimeStepDataBuffer.TimeStepData.ImageData imageData = timeStepDataBuffer.getReadData().getImageData();

        // Offset for all image data to be returned from this method
        int _imageData = 0;

        int numOfImages = imageData.getImages().size();

        Log.v("flatbuff", numOfImages + " images gathered");
        Log.v("flatbuff", "Step:" + myTrial.getTimeStep());

        int[] _images = new int[numOfImages];

        for (int i = 0; i < numOfImages ; i++){
            TimeStepDataBuffer.TimeStepData.ImageData.SingleImage image = imageData.getImages().get(i);

            int _webpImage = jp.oist.abcvlib.core.learning.fbclasses.Image.createWebpImageVector(builder, image.getWebpImage());
            jp.oist.abcvlib.core.learning.fbclasses.Image.startImage(builder);
            jp.oist.abcvlib.core.learning.fbclasses.Image.addWebpImage(builder, _webpImage);
            jp.oist.abcvlib.core.learning.fbclasses.Image.addTimestamp(builder, image.getTimestamp());
            jp.oist.abcvlib.core.learning.fbclasses.Image.addHeight(builder, image.getHeight());
            jp.oist.abcvlib.core.learning.fbclasses.Image.addWidth(builder, image.getWidth());
            int _image = jp.oist.abcvlib.core.learning.fbclasses.Image.endImage(builder);

            _images[i] = _image;
        }

        int _images_offset = jp.oist.abcvlib.core.learning.fbclasses.ImageData.createImagesVector(builder, _images);
        jp.oist.abcvlib.core.learning.fbclasses.ImageData.startImageData(builder);
        jp.oist.abcvlib.core.learning.fbclasses.ImageData.addImages(builder, _images_offset);
        _imageData = jp.oist.abcvlib.core.learning.fbclasses.ImageData.endImageData(builder);

        return _imageData;
    }

    private int addActionData(){
        CommAction ca = timeStepDataBuffer.getReadData().getActions().getCommAction();
        MotionAction ma = timeStepDataBuffer.getReadData().getActions().getMotionAction();
        Log.v("flatbuff", "CommAction : " + ca.getActionByte());
        Log.v("flatbuff", "MotionAction : " + ma.getActionName());

        return RobotAction.createRobotAction(builder, (byte) ca.getActionByte(), (byte) ma.getActionByte());
    }

    // End episode after some reward has been acheived or maxtimesteps has been reached
    public void endEpisode() {

        int ts = Episode.createTimestepsVector(builder, timeStepVector); //todo I think I need to add each timestep when it is generated rather than all at once? Is this the leak?
        Episode.startEpisode(builder);
        Episode.addRobotid(builder, robotID);
        Episode.addTimesteps(builder, ts);
        int ep = Episode.endEpisode(builder);
        builder.finish(ep);

        episode = builder.dataBuffer();

//             The following is just to check the contents of the flatbuffer prior to sending to the server.
//             You should comment this out if not using it as it doubles the required memory.
//            Also it seems the getRootAsEpisode modifes the episode buffer itself, thus messing up later processing.
//            Therefore I propose only using this as an inline debugging step or if you don't want
//            To evaluate anything past this point for a given run.

//            Episode episodeTest = Episode.getRootAsEpisode(episode);
//            Log.d("flatbuff", "TimeSteps Length: "  + String.valueOf(episodeTest.timestepsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 0 Length: "  + String.valueOf(episodeTest.timesteps(0).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 1 Length: "  + String.valueOf(episodeTest.timesteps(1).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 2 Length: "  + String.valueOf(episodeTest.timesteps(2).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 3 Length: "  + String.valueOf(episodeTest.timesteps(3).wheelCounts().timestampsLength()));
//            Log.d("flatbuff", "WheelCounts TimeStep 3 idx 0: "  + String.valueOf(episodeTest.timesteps(3).wheelCounts().timestamps(0)));
//            Log.d("flatbuff", "Levels Length TimeStep 100: "  + String.valueOf(episodeTest.timesteps(100).soundData().levelsLength()));
//            Log.d("flatbuff", "SoundData ByteBuffer Length TimeStep 100: "  + String.valueOf(episodeTest.timesteps(100).soundData().getByteBuffer().capacity()));
//            Log.d("flatbuff", "ImageData ByteBuffer Length TimeStep 100: "  + String.valueOf(episodeTest.timesteps(100).imageData().getByteBuffer().capacity()));


//            float[] soundFloats = new float[10];
//            episodeTest.timesteps(1).soundData().levelsAsByteBuffer().asFloatBuffer().get(soundFloats);
//            Log.d("flatbuff", "Sound TimeStep 1 as numpy: "  + Arrays.toString(soundFloats));
    }

    private class CyclicBarrierHandler implements Runnable {

        public CyclicBarrierHandler(){
        }

        @Override
        public void run() {
            builder.clear();
            builder = null;
            startEpisode();
        }
    }

    protected void sendToServer() throws BrokenBarrierException, InterruptedException {
        CyclicBarrier doneSignal = new CyclicBarrier(2,
                new CyclicBarrierHandler());
        Log.d("SocketConnection", "New executor deployed creating new SocketConnectionManager");
        if (inetSocketAddress != null && socketListener != null){
            executor.execute(new SocketConnectionManager(socketListener, inetSocketAddress, episode, doneSignal));
            doneSignal.await();
        }else {
            executor.execute(() -> {
                try {
                    doneSignal.await();
                } catch (BrokenBarrierException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
