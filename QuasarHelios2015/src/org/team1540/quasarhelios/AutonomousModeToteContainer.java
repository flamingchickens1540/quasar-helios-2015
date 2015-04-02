package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;
import ccre.util.Utils;

public class AutonomousModeToteContainer extends AutonomousModeBaseEnsurable {
    protected FloatInputPoll toteDistance, autoZoneDistance, secondDistance,
            nudge;

    private FloatInputPoll containerTurnTime, autoZoneTime, toteCollectTime;
    private FloatInputPoll autoZoneAngle, autoZoneSpeed;
    private BooleanInputPoll shake;

    private FloatInputPoll turn1, turn2, var1, var2;

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
        collectTote(shake.get(), (int) (toteCollectTime.get() * 1000));
        // Pickup container.
        setClampHeight(0.0f);
        startEnsureBlock();
        singleSideTurn((long) (containerTurnTime.get() * 1000), true);
        pickupContainer(nudge.get());
        endEnsureBlockAngleOnly(turn1.get(), var1.get());

        // Motion
        float nextAngle = HeadingSensor.absoluteYaw.get();
        startEnsureBlock();
        turnAbsolute(startAngle, -autoZoneAngle.get(), true);
        waitForTime(500);
        endEnsureBlockAngleOnly(turn2.get(), var2.get());
        float curAngle = HeadingSensor.absoluteYaw.get();
        Logger.info("Actual angle: " + (curAngle - startAngle) + " based on " + startAngle + "/" + nextAngle + "/" + curAngle);
        float now = Utils.getCurrentTimeSeconds();
        Logger.info("About to drive: " + DriveCode.leftEncoder.get() + " and " + autoZoneDistance.get());
        drive(autoZoneDistance.get(), autoZoneSpeed.get());
        Rollers.closed.set(false);
        driveForTime((long) (autoZoneTime.get() * 1000), autoZoneSpeed.get());
        Logger.info("Finished: " + (Utils.getCurrentTimeSeconds() - now) + ": " + DriveCode.leftEncoder.get());
    }

    public void loadSettings(TuningContext context) {
        this.turn1 = context.getFloat("Auto Mode Single Tote Turn 1", 20);
        this.var1 = context.getFloat("Auto Mode Single Tote Turn 1 Variance", 25);
        this.turn2 = context.getFloat("Auto Mode Single Tote Turn 2", 115);
        this.var2 = context.getFloat("Auto Mode Single Tote Turn 2 Variance", 30);
        this.toteDistance = context.getFloat("Auto Mode Single Tote Tote Distance +A", 28.0f);
        this.secondDistance = context.getFloat("Auto Mode Single Tote Second Distance +A", 24.0f);
        this.nudge = context.getFloat("Auto Mode Single Tote Nudge +A", 12.0f);
        this.containerTurnTime = context.getFloat("Auto Mode Single Tote Container Turn Time +A", 0.5f);
        this.toteCollectTime = context.getFloat("Auto Mode Single Tote Collect Time +A", 3.5f);
        this.autoZoneAngle = context.getFloat("Auto Mode Single Tote Auto Zone Angle +A", 100.0f);
        this.autoZoneSpeed = context.getFloat("Auto Mode Single Tote Auto Zone Speed +A", 1.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Single Tote Auto Zone Distance (1) +A", 60.0f);
        this.autoZoneTime = context.getFloat("Auto Mode Single Tote Auto Zone Time (2) +A", 1.15f);
        this.shake = context.getBoolean("Auto Mode Single Tote Shake +A", false);
    }
}
