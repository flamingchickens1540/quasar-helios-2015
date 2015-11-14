package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.FloatInput;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeNoodleAndTote extends AutonomousModeBaseEnsurable {
    @Tunable(7.0f)
    private FloatInput nudge;

    @Tunable(0.5f)
    private FloatInput containerTurnTime;
    @Tunable(valueBoolean = false)
    private BooleanInput shake;
    @Tunable(3.5f)
    private FloatInput toteCollectTime;
    @Tunable(-210.f)
    private FloatInput noodleAngle;
    @Tunable(0.80f)
    private FloatInput containerHeight;

    public AutonomousModeNoodleAndTote() {
        super("Noodle and Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException, AutonomousModeOverException {
        setClampOpen(true);
        waitUntilNot(Clamp.waitingForAutoCalibration);
        startSetClampHeight(0.4f);
        collectTote(shake.get(), (int) (toteCollectTime.get() * 1000));
        // Pickup container.
        setClampHeight(0.0f);
        singleSideTurn((long) (containerTurnTime.get() * 1000), true);
        pickupContainer(nudge.get());

        // Move to proper height.
        startSetClampHeight(containerHeight.get());
        // Motion
        turn(noodleAngle.get(), true);
    }
}
