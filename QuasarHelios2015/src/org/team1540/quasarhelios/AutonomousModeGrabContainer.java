package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeGrabContainer extends AutonomousModeBase {
    @Tunable(2.0f)
    private FloatInputPoll attachWaitingTime;
    @Tunable(1.0f)
    private FloatInputPoll strafeTime1;
    @Tunable(0.1f)
    private FloatInputPoll strafeTime2;
    @Tunable(-0.5f)
    private FloatInputPoll strafeSpeed;

    public AutonomousModeGrabContainer() {
        super("Grab Container");
    }

    @Override
    protected void runAutonomous() throws InterruptedException, AutonomousModeOverException {
        ContainerGrabber.containerGrabberSolenoid.set(true);
        waitForTime(attachWaitingTime);
        straightening.set(false);
        DriveCode.strafe.set(strafeSpeed.get());
        waitForTime(strafeTime1);
        DriveCode.strafe.set(-strafeSpeed.get());
        waitForTime(strafeTime2);
        DriveCode.strafe.set(0.0f);
        ContainerGrabber.containerGrabberSolenoid.set(false);
    }
}
