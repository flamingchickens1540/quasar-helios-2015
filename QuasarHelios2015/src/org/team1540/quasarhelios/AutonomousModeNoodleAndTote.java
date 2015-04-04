package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeNoodleAndTote extends AutonomousModeBaseEnsurable {
    @Tunable(7.0f)
    private FloatInputPoll nudge;

    @Tunable(0.5f)
    private FloatInputPoll containerTurnTime;
    @Tunable(valueBoolean=false)
    private BooleanInputPoll shake;
    @Tunable(3.5f)
    private FloatInputPoll toteCollectTime;
    @Tunable(-210.f)
    private FloatInputPoll noodleAngle;
    @Tunable(0.80f)
    private FloatInputPoll containerHeight;

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
