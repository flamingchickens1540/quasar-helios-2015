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
        strafe(STRAFE_LEFT, leftStrafeTime.get());
        DriveCode.octocanumShifting.set(false);
        drive(autoZoneDistance.get());
        depositContainer(0.0f);
        drive(-autoZoneDistance.get());
    }

    @Override
    public void loadSettings(TuningContext context) {
        this.nudge = context.getFloat("Auto Mode One Container Nudge +A", 5.0f);
        this.leftStrafeTime = context.getFloat("Auto Mode One Container Strafe Time +A", 0.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode One Container Auto Zone Distance +A", 10.0f);
    }
}
