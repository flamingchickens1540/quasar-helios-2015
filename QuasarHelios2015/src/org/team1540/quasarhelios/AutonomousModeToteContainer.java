package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeToteContainer extends AutonomousModeBase {
    protected FloatInputPoll toteDistance;
    protected FloatInputPoll autoZoneDistance;
    protected FloatInputPoll secondDistance;
    
    private FloatInputPoll containerTurnTime;
    private FloatInputPoll autoZoneAngle;
    private FloatInputPoll autoZoneSpeed;
    private FloatInputPoll nudge;
    private FloatInputPoll containerHeight;

    public AutonomousModeToteContainer() {
        super("One Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        waitUntilNot(Clamp.waitingForAutoCalibration);
        collectTote();
        // Pickup container.
        setClampOpen(true);
        setClampHeight(0.0f);
        singleSideTurn((long) (containerTurnTime.get() * 1000), true);
        pickupContainer(nudge.get());

        // Motion
        turn(-autoZoneAngle.get(), true);
        waitForTime(500);
        DriveCode.octocanumShifting.set(true);
        drive(autoZoneDistance.get(), autoZoneSpeed.get());
        DriveCode.octocanumShifting.set(false);
    }

    public void loadSettings(TuningContext context) {
        this.toteDistance = context.getFloat("Auto Mode Single Tote Tote Distance +A", 28.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Single Tote Auto Zone Distance +A", 60.0f);
        this.secondDistance = context.getFloat("Auto Mode Single Tote Second Distance +A", 24.0f);
        this.nudge = context.getFloat("Auto Mode Single Tote Nudge +A", 12.0f);
        this.containerTurnTime = context.getFloat("Auto Mode Single Tote Container Turn Time +A", 0.5f);
        this.autoZoneAngle = context.getFloat("Auto Mode Single Tote Auto Zone Angle +A", 90.0f);
        this.containerHeight = context.getFloat("Auto Mode Single Tote Container Height +A", 0.0f);
        this.autoZoneSpeed = context.getFloat("Auto Mode Single Tote Auto Zone Speed", 1.0f);
    }

}
