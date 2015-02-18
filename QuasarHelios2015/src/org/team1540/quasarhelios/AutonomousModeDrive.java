package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeDrive extends AutonomousModeBase {
    protected FloatInputPoll driveDistance, turnAngle;

    public AutonomousModeDrive() {
        super("Drive");
    }

    @Override
    protected void runAutonomous() throws InterruptedException, AutonomousModeOverException {
        drive(driveDistance.get());
        waitForTime(1000);
        if (turnAngle.get() != 0) {
            turn(turnAngle.get());
        }
    }

    public void loadSettings(TuningContext context) {
        this.driveDistance = context.getFloat("Auto Mode Drive Distance +A", 2.0f);
        this.turnAngle = context.getFloat("Auto Mode Drive Angle +A", 0.0f);
    }
}
