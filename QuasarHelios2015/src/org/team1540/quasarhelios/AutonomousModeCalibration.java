package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeCalibration extends AutonomousModeBase {
    protected FloatInputPoll driveDistance;

    public AutonomousModeCalibration() {
        super("Calibration");
    }

    private BooleanInputPoll stopButton = Igneous.joystick1.getButtonChannel(3);

    @Override
    protected void runAutonomous() throws InterruptedException, AutonomousModeOverException {
        Autonomous.desiredAngle.set(HeadingSensor.absoluteYaw.get());
        DriveCode.rightMotors.set(.5f);
        DriveCode.leftMotors.set(-.5f);
        waitUntil(stopButton);
        straightening.set(true);
        waitUntil(stopButton);
    }

    public void loadSettings(TuningContext context) {
        return;
    }
}