package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;

public class AutonomousModeGrabContainer extends AutonomousModeBase {
    protected FloatInputPoll attachWaitingTime, strafeTime, strafeSpeed;

    public AutonomousModeGrabContainer() {
        super("Grab Container");
    }

    @Override
    protected void runAutonomous() throws InterruptedException, AutonomousModeOverException {
        ContainerGrabber.containerGrabberSolenoid.set(true);
        waitForTime(attachWaitingTime);
        straightening.set(false);
        DriveCode.strafe.set(strafeSpeed.get());
        waitForTime(strafeTime);
        DriveCode.strafe.set(0.0f);
        ContainerGrabber.containerGrabberSolenoid.set(false);
    }

    public void loadSettings(TuningContext context) {
        attachWaitingTime = context.getFloat("Auto Mode Grab Container Wait Time +A", 1.0f);
        strafeTime = context.getFloat("Auto Mode Grab Container Strafe Time +A", 1.0f);
        strafeSpeed = context.getFloat("Auto Mode Grab Container Strafe Speed +A", -0.5f);
    }
}
