package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeNoodlePrep extends AutonomousModeBase {
    @Tunable(0.5f)
    private FloatInputPoll containerTurnTime;
    @Tunable(-210.f)
    private FloatInputPoll noodleAngle;
    @Tunable(7.0f)
    private FloatInputPoll nudge;
    @Tunable(0.80f)
    private FloatInputPoll containerHeight;

    public AutonomousModeNoodlePrep() {
        super("Noodle Prep");
    }

    @Override
    protected void runAutonomous() throws InterruptedException, AutonomousModeOverException {
        setClampOpen(true);
        waitUntilNot(Clamp.waitingForAutoCalibration);

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
