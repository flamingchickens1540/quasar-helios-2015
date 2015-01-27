package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeOneTote extends AutonomousModeBase {
    protected FloatInputPoll toteDistance;
    protected FloatInputPoll autoZoneDistance;
    protected FloatInputPoll returnDistance;

    public AutonomousModeOneTote() {
        super("One Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        collectTote();
        turn(90);
        drive(autoZoneDistance.get());
        ejectTotes();
        drive(-returnDistance.get());
    }

    public void loadSettings(TuningContext context) {
        this.toteDistance = context.getFloat("auto-single-tote-distance", 2.0f);
        this.autoZoneDistance = context.getFloat("auto-single-auto-zone-distance", 2.0f);
        this.returnDistance = context.getFloat("auto-single-return-distance", 2.0f);
    }

}
