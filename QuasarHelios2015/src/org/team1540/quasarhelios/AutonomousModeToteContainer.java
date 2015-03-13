package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;
import ccre.util.Utils;

public class AutonomousModeToteContainer extends AutonomousModeBase {
    protected FloatInputPoll toteDistance;
    protected FloatInputPoll autoZoneDistance, autoZoneTime;
    protected FloatInputPoll secondDistance;
    
    private FloatInputPoll containerTurnTime;
    private FloatInputPoll autoZoneAngle;
    private FloatInputPoll autoZoneSpeed;
    private FloatInputPoll nudge;

    public AutonomousModeToteContainer() {
        super("One Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        float startAngle = HeadingSensor.absoluteYaw.get();
        setClampOpen(true);
        waitUntilNot(Clamp.waitingForAutoCalibration);
        startSetClampHeight(0.4f);
        collectTote(false, 4000);
        // Pickup container.
        setClampHeight(0.0f);
        singleSideTurn((long) (containerTurnTime.get() * 1000), true);
        pickupContainer(nudge.get());

        // Motion
        float nextAngle = HeadingSensor.absoluteYaw.get();
        turnAbsolute(startAngle, -autoZoneAngle.get(), true);
        waitForTime(500);
        float curAngle = HeadingSensor.absoluteYaw.get();
        Logger.info("Actual angle: " + (curAngle - startAngle) + " based on " + startAngle + "/" + nextAngle + "/" + curAngle);
        float now = Utils.getCurrentTimeSeconds();
        Logger.info("Setting to mechanum: " + DriveCode.leftEncoder.get() + " and " + autoZoneDistance.get());
        DriveCode.octocanumShifting.set(true);
        drive(autoZoneDistance.get(), autoZoneSpeed.get());
        Rollers.closed.set(false);
        driveForTime((long) (autoZoneTime.get() * 1000), autoZoneSpeed.get());
        Logger.info("Setting to traction: " + (Utils.getCurrentTimeSeconds() - now) + ": " + DriveCode.leftEncoder.get());
        DriveCode.octocanumShifting.set(false);
    }

    public void loadSettings(TuningContext context) {
        this.toteDistance = context.getFloat("Auto Mode Single Tote Tote Distance +A", 28.0f);
        this.secondDistance = context.getFloat("Auto Mode Single Tote Second Distance +A", 24.0f);
        this.nudge = context.getFloat("Auto Mode Single Tote Nudge +A", 12.0f);
        this.containerTurnTime = context.getFloat("Auto Mode Single Tote Container Turn Time +A", 0.5f);
        this.autoZoneAngle = context.getFloat("Auto Mode Single Tote Auto Zone Angle +A", 100.0f);
        this.autoZoneSpeed = context.getFloat("Auto Mode Single Tote Auto Zone Speed +A", 1.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Single Tote Auto Zone Distance (1) +A", 60.0f);
        this.autoZoneTime = context.getFloat("Auto Mode Single Tote Auto Zone Time (2) +A", 1.2f);
    }
}
