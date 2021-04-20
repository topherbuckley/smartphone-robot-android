package jp.oist.abcvlib.core.learning;

import android.util.Log;

import jp.oist.abcvlib.core.AbcvlibActivity;

public class ActionSelector implements Runnable{

    private AbcvlibActivity abcvlibActivity;
    private double reward;
    private Thread mThread;
    private final String TAG = this.getClass().getName();

    public ActionSelector(AbcvlibActivity abcvlibActivity){
        this.abcvlibActivity = abcvlibActivity;
    }

    public void start() {
        mThread = new Thread(this);
        mThread.start();
    }

    public void stop() {
        try {
            mThread.join(10);
        } catch (InterruptedException e) {
            Log.v("abcvlib", "InterruptedException.", e);
        }
    }

    public void run() {

        while (!abcvlibActivity.appRunning){
            try {
                Log.i("abcvlib", this.toString() + "Waiting for appRunning to be true");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG,"Error", e);
            }
        }

        while(abcvlibActivity.appRunning) {

            abcvlibActivity.determineReward();

            Thread.yield();
        }
    }

    public double getReward(){
        return reward;
    }
}
