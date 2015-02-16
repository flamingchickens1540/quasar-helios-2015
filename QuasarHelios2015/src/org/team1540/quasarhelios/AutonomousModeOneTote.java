package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeOneTote extends AutonomousModeBase {
    protected FloatInputPoll toteDistance;
    protected FloatInputPoll autoZoneDistance;
    protected FloatInputPoll returnDistance;
    
    private BooleanInputPoll collectContainer;
    private FloatInputPoll nudge;
    private FloatInputPoll strafeTime;

    public AutonomousModeOneTote() {
        super("One Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        collectTote();
        if (collectContainer.get()) {
            setClampHeight(0.0f);
            setClampOpen(true);
            drive(nudge.get());
            setClampOpen(false);
        }
        turn(90);
        drive(autoZoneDistance.get());
        ejectTotes();
        if (collectContainer.get()) {
            setClampHeight(0.0f);
            setClampOpen(true);
            setClampHeight(1.0f);
            DriveCode.octocanumShifting.set(true);
            strafe(STRAFE_RIGHT, strafeTime.get());
        }
        drive(-returnDistance.get());
    }

    public void loadSettings(TuningContext context) {
        this.toteDistance = context.getFloat("auto-single-tote-distance", 2.0f);
        this.autoZoneDistance = context.getFloat("auto-single-auto-zone-distance", 2.0f);
        this.returnDistance = context.getFloat("auto-single-return-distance", 2.0f);
        this.collectContainer = context.getBoolean("auto-single-collect-container", false);
        this.nudge = context.getFloat("auto-single-nudge", 1.0f);
        this.strafeTime = context.getFloat("auto-single-strafe-time", 1.0f);
    }

}
