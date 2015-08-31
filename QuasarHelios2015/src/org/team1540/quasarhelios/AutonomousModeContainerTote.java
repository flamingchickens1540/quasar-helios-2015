package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.FloatInput;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;

public class AutonomousModeContainerTote extends AutonomousModeBaseEnsurable {

    @Tunable(65.0f)
    private FloatInput autoZoneDistance;
    @Tunable(0.2f)
    private FloatInput closeClampTime;
    @Tunable(0.6f)
    private FloatInput containerTurnTime;
    @Tunable(10f)
    private FloatInput containerDriveTime;
    @Tunable(100.0f)
    private FloatInput autoZoneAngle;
    @Tunable(1.0f)
    private FloatInput autoZoneSpeed;
    @Tunable(0.75f)
    private FloatInput topClampHeight;
    @Tunable(valueBoolean = false)
    private BooleanInput shake;
    @Tunable(-90.0f)
    private FloatInput finishAngle;

    @Tunable(25)
    private FloatInput turn1;
    @Tunable(110)
    private FloatInput turn2;
    @Tunable(25)
    private FloatInput var1;
    @Tunable(20)
    private FloatInput var2;

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
