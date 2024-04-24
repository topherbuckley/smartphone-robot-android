package jp.oist.abcvlib.util;

import android.content.Context;
import android.util.Log;
import com.hoho.android.usbserial.driver.SerialTimeoutException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;


public class SerialCommManager {
    /*
    Class to manage request-response patter between Android phone and USB Serial connection
    over a separate thread.
    send()
    onReceive()
     */

    private UsbSerial usbSerial;
    // fifoQueue is used to store the commands that are sent from the mcu to be executed
    // on the Android phone

    // Preallocated bytebuffer to write motor levels to
    private AndroidToRP2040Packet androidToRP2040Packet = new AndroidToRP2040Packet();
    private RP2040State rp2040State;
    private boolean shutdown = false;
    private Runnable android2PiWriter;
    private SerialReadyListener serialReadyListener;

    long startTimeAndroid;
    int cnt = 0;
    long durationAndroid;

    // Constructor to initialize SerialCommManager
    public SerialCommManager(UsbSerial usbSerial,
                             Runnable android2PiWriter,
                             SerialReadyListener serialReadyListener,
                             BatteryData batteryData,
                             WheelData wheelData) {
        this.usbSerial = usbSerial;
        this.serialReadyListener = serialReadyListener;
        if (android2PiWriter == null){
            this.android2PiWriter = defaultAndroid2PiWriter;
            Log.w("serial", "android2PiWriter was null. Using default rather than custom");
        }else{
            this.android2PiWriter = android2PiWriter;
        }
        if (batteryData == null || wheelData == null){
            Log.w("serial", "batteryData or wheelData was null. " +
                    "Ignoring all rp2040 state values. You must initialize both to use rp2040 state");
            rp2040State = null;
        }else{
            rp2040State = new RP2040State(batteryData, wheelData);
        }
    }

    public SerialCommManager(UsbSerial usbSerial,
                             Runnable android2PiWriter,
                             SerialReadyListener serialReadyListener){
        this(usbSerial, android2PiWriter, serialReadyListener, null, null);
    }

    public SerialCommManager(UsbSerial usbSerial,
                             SerialReadyListener serialReadyListener,
                             BatteryData batteryData,
                             WheelData wheelData){
        this(usbSerial, null, serialReadyListener, batteryData, wheelData);
    }

    private final Runnable defaultAndroid2PiWriter = new Runnable() {
        @Override
        public void run() {
            while (!shutdown) {
                //TODO
            }
        }
    };

    // Start method to start the thread
    public void start() {
        ProcessPriorityThreadFactory serialCommManager_Android2Pi_factory =
                new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY,
                        "SerialCommManager_Android2Pi");
        Executors.newSingleThreadScheduledExecutor(serialCommManager_Android2Pi_factory).
                scheduleWithFixedDelay(android2PiWriter, 0, 10, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void stop() {
        shutdown = true;
    }

    //TODO paseFifoPacket() should call the various SerialResponseListener methods.
    protected int parseFifoPacket() {
        int result = 0;
        FifoQueuePair fifoQueuePair;
        byte[] packet = null;
        // Run the command on a thread handler to allow the queue to keep being added to
        synchronized (usbSerial.fifoQueue) {
            fifoQueuePair = usbSerial.fifoQueue.poll();
        }
            // Check if there is a packet in the queue (fifoQueue
        if (fifoQueuePair != null) {
            packet = fifoQueuePair.getByteArray();

            // Log packet as an array of hex bytes
            Log.i(Thread.currentThread().getName(), "Received packet: " + HexBinConverters.bytesToHex(packet));

            // The first byte after the start mark is the command
            AndroidToRP2040Command command = fifoQueuePair.getAndroidToRP2040Command();
            Log.i(Thread.currentThread().getName(), "Received " + command + " from pi");
            if (command == null){
                Log.e("Pi2AndroidReader", "Command not found");
                return -1;
            }
            switch (command) {
                case GET_LOG:
                    parseLog(packet);
                    result = 1;
                    break;
                case SET_MOTOR_LEVELS:
                case RESET_STATE:
                    parseStatus(packet);
                    result = 1;
                    break;
                case NACK:
                    onNack(packet);
                    Log.w("Pi2AndroidReader", "Nack issued from device");
                    result = -1;
                    break;
                case ACK:
                    onAck(packet);
                    result = 1;
                    Log.d("Pi2AndroidReader", "parseAck");
                    break;
                case START:
                    Log.e("Pi2AndroidReader", "parseStart. Start should never be a command");
                    result = -1;
                    break;
                case STOP:
                    Log.e("Pi2AndroidReader", "parseStop. Stop should never be a command");
                    result = -1;
                    break;
                default:
                    Log.e("Pi2AndroidReader", "parsePacket. Command not found");
                    result = -1;
                    break;
            }
        }
        else {
            Log.i(Thread.currentThread().getName(), "No packet in queue");
            result = 0;
        }
        return result;
    }


    /**
     * Do not use this method unless you are very familiar with the protocol on both the rp2040 and
     * Android side. This method is used to send raw bytes to the rp2040. It is recommended to use
     * the wrapped methods for doing higher level commands such as setMotorLevels. Sending the wrong
     * bytes to the rp2040 can cause it to crash and require a reset or worse.
     * @param bytes The raw bytes to be sent to the rp2040
     * @return 0 if successful, -1 if mResponse is not large enough to hold all of response and the stop mark,
     * -2 if SerialTimeoutException on send
     */
    private int sendPacket(byte[] bytes) {
        if (bytes.length != AndroidToRP2040Packet.packetSize) {
            throw new IllegalArgumentException("Input byte array must have a length of " + AndroidToRP2040Packet.packetSize);
        }
        try {
            this.usbSerial.send(bytes, 1000);
        } catch (SerialTimeoutException e){
            Log.e("serial", "SerialTimeoutException on send");
            return -2;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private int recievePacket() {
        int recievedStatus = usbSerial.awaitPacketReceived(1000);
        if (recievedStatus == 1){
            //Note this is actually calling the functions like parseLog, parseStatus, etc.
            int result = parseFifoPacket();
        }
        return recievedStatus;
    }

    //-------------------------------------------------------------------///
    // ---- API function calls for requesting something from the mcu ----///
    //-------------------------------------------------------------------///
    private void sendAck() throws IOException {
        byte[] ack = new byte[]{AndroidToRP2040Command.ACK.getHexValue()};
        sendPacket(ack);
    }

    /*
    parameters:
    float: left [-1,1] representing full speed backward to full speed forward
    float: right (same as left)
     */
    public void setMotorLevels(float left, float right, boolean leftBrake, boolean rightBrake) {
        if (cnt == 0) {
            // start timer
            startTimeAndroid = System.nanoTime();
        }
        androidToRP2040Packet.clear();
        androidToRP2040Packet.setCommand(AndroidToRP2040Command.SET_MOTOR_LEVELS);

        final float LOWER_LIMIT = 0.49f;

        // Normalize [-1,1] to [-5.06,5.06] as this is the range accepted by the chip
        left = left * 5.06f;
        right = right * 5.06f;

        byte DRV8830_IN1_BIT = 0;
        byte DRV8830_IN2_BIT = 1;

        float[] voltages = new float[]{left, right};
        float[] abs_voltages = new float[2];
        byte[] control_values = new byte[2];
        boolean[] brakes = new boolean[]{leftBrake, rightBrake};

        for (int i = 0; i < voltages.length; i++) {
            float voltage = voltages[i]; // Get the current voltage
            control_values[i] = 0; // Reset the control value
            // Exclude or truncate voltages between -0.48V and 0.48V to 0V
            // Changing to 0.49 as the scaling function would result in 0x05h for 0.48V and
            // cause the rp2040 to perform unexpectely as it is a reserved register value
            if (voltage >= -LOWER_LIMIT && voltage <= LOWER_LIMIT) {
                voltages[i] = 0.0f; // Update the value in the array
            }else{
                // Clamp the voltage within the valid range
                // Need to clamp here rather than at byte representation to prevent overflow
                if (voltages[i] < -5.06) {
                    voltages[i] = -5.06f; // Update the value in the array
                }
                else if (voltages[i] > 5.06) {
                    voltages[i] = 5.06f; // Update the value in the array
                }

                abs_voltages[i] = Math.abs(voltages[i]);
                // Convert voltage to control value (-0x3F to 0x3F)
                control_values[i] = (byte)(((64 * abs_voltages[i]) / (4 * 1.285)) - 1);
                // voltage is defined by bits 2-7. Shift the control value to the correct position
                control_values[i] = (byte) (control_values[i] << 2);
            }


            // Set the IN1 and IN2 bits based on the sign of the voltage
            if (brakes[i]) {
                control_values[i] |= (1 << DRV8830_IN1_BIT);
                control_values[i] |= (1 << DRV8830_IN2_BIT);
            }else{
                if (voltage < 0) {
                    control_values[i] |= (1 << DRV8830_IN1_BIT);
                    control_values[i] &= ~(1 << DRV8830_IN2_BIT);
                } else if (voltage > 0) {
                    control_values[i] |= (1 << DRV8830_IN2_BIT);
                    control_values[i] &= ~(1 << DRV8830_IN1_BIT);
                } else {
                    // Standby/Coast: Both IN1 and IN2 set to 0
                    control_values[i] = 0;
                }
            }
            androidToRP2040Packet.payload.put(control_values[i]);
        }

        // Just to check for error in return packet
        if (rp2040State != null){
            rp2040State.motorsState.controlValues.left = control_values[0];
            rp2040State.motorsState.controlValues.right = control_values[1];
        }

        byte[] commandData = androidToRP2040Packet.packetTobytes();
        if (sendPacket(commandData) != 0){
            Log.e("Android2PiWriter", "Error sending packet");
        }else{
            Log.d("Android2PiWriter", "Packet sent");
            if (recievePacket() != 0){
                Log.e("Android2PiWriter", "Error receiving packet");
            }else{
                Log.d("Android2PiWriter", "Packet received");
            }
        }
        cnt++;
        durationAndroid = durationAndroid + (System.nanoTime() - startTimeAndroid);
        if (cnt == 100) {
            cnt = 0;
            durationAndroid = durationAndroid / 100;
            // convert from nanoseconds to milliseconds
            Log.i("AndroidSide", "Average time per command: " + durationAndroid / 1000 + "us");
        }
    }

    public void getLog(){
        androidToRP2040Packet.clear();
        androidToRP2040Packet.setCommand(AndroidToRP2040Command.GET_LOG);
        byte[] commandData = androidToRP2040Packet.packetTobytes();
        if (sendPacket(commandData) != 0){
            Log.e("Android2PiWriter", "Error sending packet");
        }else{
            Log.d("Android2PiWriter", "Packet sent");
        }
    }

    //----------------------------------------------------------///
    // ---- Handlers for when data is returned from the mcu ----///
    // ---- Override these defaults with your own handlers -----///
    //----------------------------------------------------------///
    private void parseLog(byte[] bytes) {
        Log.d("serial", "parseLogs");
        String string = new String(bytes, StandardCharsets.US_ASCII);
        String[] lines = string.split("\\r?\\n");
        for (String line : lines) {
            Log.i("rp2040Log", line);
        }
    }
    private void parseStatus(byte[] bytes) {
        Log.d("serial", "parseStatus");
        if (rp2040State != null){
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            if (rp2040State.motorsState.controlValues.left != byteBuffer.get()){
                Log.e("serial", "Left control value mismatch");
            }
            if (rp2040State.motorsState.controlValues.right != byteBuffer.get()){
                Log.e("serial", "Right control value mismatch");
            }
            rp2040State.motorsState.faults.left = byteBuffer.get();
            rp2040State.motorsState.faults.right = byteBuffer.get();
            Log.v("serial", "left motor fault: " + rp2040State.motorsState.faults.left);
            Log.v("serial", "right motor fault: " + rp2040State.motorsState.faults.right);
            rp2040State.motorsState.encoderCounts.left = byteBuffer.getInt();
            rp2040State.motorsState.encoderCounts.right = byteBuffer.getInt();
            Log.v("serial", "Left encoder count: " + rp2040State.motorsState.encoderCounts.left);
            Log.v("serial", "Right encoder count: " + rp2040State.motorsState.encoderCounts.right);
            rp2040State.batteryDetails.voltage = byteBuffer.getShort();
            rp2040State.batteryDetails.safety_status = byteBuffer.get();
            rp2040State.batteryDetails.temperature = byteBuffer.getShort();
            rp2040State.batteryDetails.state_of_health = byteBuffer.get();
            rp2040State.batteryDetails.flags = byteBuffer.getShort();
            Log.v("serial", "Battery voltage: " + rp2040State.batteryDetails.voltage);
            Log.v("serial", "Battery voltage in V: " + rp2040State.batteryDetails.getVoltage());
            rp2040State.chargeSideUSB.max77976_chg_details = byteBuffer.getInt();
            rp2040State.chargeSideUSB.ncp3901_wireless_charger_attached = byteBuffer.get() == 1;
            Log.v("serial", "ncp3901_wireless_charger_attached: " + rp2040State.chargeSideUSB.ncp3901_wireless_charger_attached);
            rp2040State.chargeSideUSB.usb_charger_voltage = byteBuffer.getShort();
            //Log.v("serial", "usb_charger_voltage: " + rp2040State.chargeSideUSB.usb_charger_voltage);
            rp2040State.updatePublishers();
        }
    }
    private void onNack(byte[] bytes) {
        Log.d("serial", "parseNack");
    }
    private void onAck(byte[] bytes) {
        Log.d("serial", "parseAck");
    }


}
