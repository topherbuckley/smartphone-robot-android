package jp.oist.abcvlib.core.outputs;

import jp.oist.abcvlib.core.learning.ActionSet;
import jp.oist.abcvlib.core.learning.CommActionSet;
import jp.oist.abcvlib.core.learning.MotionActionSet;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;

public class StepHandler {
    private final int maxTimeStepCount;
    private boolean lastEpisode = false; // Use to trigger MainActivity to stop generating episodes
    private boolean lastTimestep = false; // Use to trigger MainActivity to stop generating timesteps for a single episode
    private int reward = 0;
    private final int maxReward;
    private final int maxEpisodecount;
    private int episodeCount = 0;
    private final CommActionSet commActionSet;
    private final MotionActionSet motionActionSet;
    private ActionSelector actionSelector;

    public StepHandler(int maxTimeStepCount, int maxReward, int maxEpisodeCount,
                       CommActionSet commActionSet, MotionActionSet motionActionSet){
        this.maxTimeStepCount = maxTimeStepCount;
        this.maxReward = maxReward;
        this.maxEpisodecount = maxEpisodeCount;
        this.motionActionSet = motionActionSet;
        this.commActionSet = commActionSet;
    }

    public static class StepHandlerBuilder {
        private int maxTimeStepCount = 100;
        private int maxReward = 100;
        private int maxEpisodeCount = 3;
        private CommActionSet commActionSet = new CommActionSet();
        private MotionActionSet motionActionSet = new MotionActionSet();

        public StepHandlerBuilder(){}

        public StepHandler buildStepHandler(){
            return new StepHandler(maxTimeStepCount, maxReward, maxEpisodeCount,
                    commActionSet, motionActionSet);
        }

        public StepHandlerBuilder setMaxTimeStepCount(int maxTimeStepCount){
            this.maxEpisodeCount = maxTimeStepCount;
            return this;
        }

        public StepHandlerBuilder setMaxReward(int maxReward){
            this.maxReward = maxReward;
            return this;
        }

        public StepHandlerBuilder setMaxEpisodeCount(int maxEpisodeCount){
            this.maxEpisodeCount = maxEpisodeCount;
            return this;
        }

        public StepHandlerBuilder setCommActionSet(CommActionSet commActionSet){
            this.commActionSet = commActionSet;
            return this;
        }

        public StepHandlerBuilder setMotionActionSet(MotionActionSet moctionActionSet){
            this.motionActionSet = moctionActionSet;
            return this;
        }
    }

    public void setActionSelector(ActionSelector actionSelector){
        this.actionSelector = actionSelector;
    }

    public ActionSet forward(TimeStepDataBuffer.TimeStepData data, int timeStepCount){
        return this.actionSelector.forward(data, timeStepCount);
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public boolean isLastEpisode() {
        return lastEpisode;
    }

    public boolean isLastTimestep() {
        return lastTimestep;
    }

    public int getMaxEpisodecount() {
        return maxEpisodecount;
    }

    public int getMaxTimeStepCount() {
        return maxTimeStepCount;
    }

    public int getReward() {
        return reward;
    }

    public int getMaxReward() {
        return maxReward;
    }

    public void incrementEpisodeCount() {
        this.episodeCount = episodeCount++;
    }

    public void setLastEpisode(boolean lastEpisode) {
        this.lastEpisode = lastEpisode;
    }

    public MotionActionSet getMotionActionSet() {
        return motionActionSet;
    }

    public CommActionSet getCommActionSet() {
        return commActionSet;
    }

    public void setLastTimestep(boolean lastTimestep) {
        this.lastTimestep = lastTimestep;
    }
}
