package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.FloatInput;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;
import ccre.util.Utils;

public class AutonomousModeToteContainer extends AutonomousModeBaseEnsurable {
    @Tunable(60.0f)
    private FloatInput autoZoneDistance;
    @Tunable(7.0f)
    private FloatInput nudge;

    @Tunable(0.5f)
    private FloatInput containerTurnTime;
    @Tunable(1.15f)
    private FloatInput autoZoneTime;
    @Tunable(3.5f)
    private FloatInput toteCollectTime;
    @Tunable(100.0f)
    private FloatInput autoZoneAngle;
    @Tunable(1.0f)
    private FloatInput autoZoneSpeed;
    @Tunable(valueBoolean = false)
    private BooleanInput shake;

    @Tunable(20)
    private FloatInput turn1;
    @Tunable(115)
    private FloatInput turn2;
    @Tunable(25)
    private FloatInput var1;
    @Tunable(30)
    private FloatInput var2;

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
}
