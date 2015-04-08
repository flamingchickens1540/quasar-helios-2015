package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;

public class AutonomousModeContainerTote extends AutonomousModeBaseEnsurable {

    @Tunable(65.0f)
    private FloatInputPoll autoZoneDistance;
    @Tunable(0.2f)
    private FloatInputPoll closeClampTime;
    @Tunable(0.6f)
    private FloatInputPoll containerTurnTime;
    @Tunable(10f)
    private FloatInputPoll containerDriveTime;
    @Tunable(100.0f)
    private FloatInputPoll autoZoneAngle;
    @Tunable(1.0f)
    private FloatInputPoll autoZoneSpeed;
    @Tunable(0.75f)
    private FloatInputPoll topClampHeight;
    @Tunable(valueBoolean = false)
    private BooleanInputPoll shake;
    @Tunable(-90.0f)
    private FloatInputPoll finishAngle;

    @Tunable(25)
    private FloatInputPoll turn1;
    @Tunable(110)
    private FloatInputPoll turn2;
    @Tunable(25)
    private FloatInputPoll var1;
    @Tunable(20)
    private FloatInputPoll var2;

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

        startEnsureBlock();
        singleSideTurn((long) (containerTurnTime.get() * 1000), false);
        waitForTime(500);
        endEnsureBlockAngleOnly(turn1.get(), var1.get());
        drive(containerDriveTime.get());

        collectTote(shake.get(), -1);

        // Motion.
        startEnsureBlock();
        turnAbsolute(startAngle, autoZoneAngle.get(), true);
        waitForTime(500);
        endEnsureBlockAngleOnly(turn2.get(), var2.get());
        drive(autoZoneDistance.get(), autoZoneSpeed.get());
        turnAbsolute(HeadingSensor.absoluteYaw.get(), finishAngle.get(), true);
    }
}
