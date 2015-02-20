package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;

public class AutonomousModeDrive extends AutonomousModeBase {
    protected FloatInputPoll driveDistance, turnAngle;

    public AutonomousModeDrive() {
        super("Drive");
    }

    @Override
    protected void runAutonomous() throws InterruptedException, AutonomousModeOverException {
        if (driveDistance.get() != 0) {
            float distance0 = DriveCode.leftEncoder.get();
            drive(driveDistance.get());
            float distance1 = DriveCode.leftEncoder.get();
            waitForTime(1000);
            float distance2 = DriveCode.leftEncoder.get();
            Logger.info("Wanted to drive " + driveDistance.get() + " / originally went " + (distance1 - distance0) + " / finally went " + (distance2 - distance0));
        }
        if (turnAngle.get() != 0) {
            float angle0 = HeadingSensor.absoluteYaw.get();
            turn(turnAngle.get(), true);
            float angle1 = HeadingSensor.absoluteYaw.get();
            waitForTime(1000);
            float angle2 = HeadingSensor.absoluteYaw.get();
            Logger.info("Wanted to turn " + turnAngle.get() + " / originally went " + (angle1 - angle0) + " / finally went " + (angle2 - angle0));
        }
    }

    public void loadSettings(TuningContext context) {
        this.driveDistance = context.getFloat("Auto Mode Drive Distance +A", 0.0f);
        this.turnAngle = context.getFloat("Auto Mode Drive Angle +A", 90.0f);
    }
}
