package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.FloatInputPoll;
import ccre.cluck.Cluck;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeCalibration extends AutonomousModeBase {
    protected FloatInputPoll driveDistance;

    public AutonomousModeCalibration() {
        super("Calibration");
        Cluck.publish("Auto Mode Calibration Stop", stopButton);
    }

    private BooleanStatus stopButton = new BooleanStatus();

    @Override
    protected void runAutonomous() throws InterruptedException, AutonomousModeOverException {
        Autonomous.desiredAngle.set(HeadingSensor.absoluteYaw.get());
        DriveCode.rightMotors.set(1);
        DriveCode.leftMotors.set(-1);
        waitUntil(stopButton);
        DriveCode.allMotors.set(0);
        straightening.set(true);
        waitUntilNot(stopButton);
    }

    public void loadSettings(TuningContext context) {
    }
}