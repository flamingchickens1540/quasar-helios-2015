package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;

public class AutonomousModeContainerTote extends AutonomousModeBase {
    protected FloatInputPoll toteDistance;
    protected FloatInputPoll autoZoneDistance;
    protected FloatInputPoll secondDistance;

    private FloatInputPoll closeClampTime;
    private FloatInputPoll containerTurnTime;
    private FloatInputPoll containerDriveTime;
    private FloatInputPoll autoZoneAngle;
    private FloatInputPoll autoZoneSpeed;
    private FloatInputPoll topClampHeight;
    private BooleanInputPoll shake;
    private FloatInputPoll finishAngle;

    public AutonomousModeContainerTote() {
        super("Container, then Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        float startAngle = HeadingSensor.absoluteYaw.get();
        setClampOpen(false);
        waitUntilNot(Clamp.waitingForAutoCalibration);

        // Pickup container.
        if (Clamp.heightReadout.get() < topClampHeight.get()) {
            waitForTime(closeClampTime);
            Logger.info("Starting clamp move...");
            setClampHeight(topClampHeight.get());
        }
        singleSideTurn((long) (containerTurnTime.get() * 1000), false);
        waitForTime(500);
        drive(containerDriveTime.get());

        collectTote(shake.get(), -1);

        // Motion.
        turnAbsolute(startAngle, autoZoneAngle.get(), true);
        waitForTime(500);
        drive(autoZoneDistance.get(), autoZoneSpeed.get());
        turnAbsolute(HeadingSensor.absoluteYaw.get(), finishAngle.get(), true);
    }

    public void loadSettings(TuningContext context) {
        this.toteDistance = context.getFloat("Auto Mode Container Tote Tote Distance +A", 28.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Container Tote Auto Zone Distance +A", 65.0f);
        this.secondDistance = context.getFloat("Auto Mode Container Tote Second Distance +A", 24.0f);
        this.closeClampTime = context.getFloat("Auto Mode Container Tote Clamp Close Time +A", 0.2f);
        this.containerTurnTime = context.getFloat("Auto Mode Container Tote Container Turn Time +A", 0.6f);
        this.containerDriveTime = context.getFloat("Auto Mode Container Tote Container Drive Distance +A", 10f);
        this.autoZoneAngle = context.getFloat("Auto Mode Container Tote Auto Zone Angle +A", 100.0f);
        this.autoZoneSpeed = context.getFloat("Auto Mode Container Tote Auto Zone Speed +A", 1.0f);
        this.topClampHeight = context.getFloat("Auto Mode Container Tote Top Clamp Height +A", 0.75f);
        this.shake = context.getBoolean("Auto Mode Container Tote Shake +A", false);
        this.finishAngle = context.getFloat("Auto Mode Container Tote Final Angle +A", -90.0f);
    }
}
