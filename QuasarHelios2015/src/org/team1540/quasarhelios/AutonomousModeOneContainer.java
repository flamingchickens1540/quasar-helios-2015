package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeOneContainer extends AutonomousModeBase  {
    private FloatInputPoll nudge;
    private FloatInputPoll leftStrafeTime;
    private FloatInputPoll autoZoneDistance;

    public AutonomousModeOneContainer() {
        super("One Container");
    }
    
    @Override
    protected void runAutonomous() throws InterruptedException, AutonomousModeOverException {
        pickupContainer(nudge.get());
        DriveCode.octocanumShifting.set(true);
        strafe(STRAFE_RIGHT, leftStrafeTime.get());
        DriveCode.octocanumShifting.set(false);
        drive(autoZoneDistance.get());
        depositContainer();
        drive(-autoZoneDistance.get());
    }

    @Override
    public void loadSettings(TuningContext context) {
        this.nudge = context.getFloat("auto-one-container-nudge", 5.0f);
        this.leftStrafeTime = context.getFloat("auto-one-container-left-strafe-time", 0.0f);
        this.autoZoneDistance = context.getFloat("auto-one-container-auto-zone-distance", 10.0f);
    }
}
