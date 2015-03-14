package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeNoodlePrep extends AutonomousModeBase {
    private FloatInputPoll containerTurnTime;
    private FloatInputPoll noodleAngle;
    private FloatInputPoll nudge;
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

    public void loadSettings(TuningContext context) {
        this.nudge = context.getFloat("Auto Mode Noodle Prep Nudge +A", 12.0f);
        this.containerTurnTime = context.getFloat("Auto Mode Noodle Prep Container Turn Time +A", 0.5f);
        this.noodleAngle = context.getFloat("Auto Mode Noodle Prep Auto Zone Angle +A", -210.0f);
        this.containerHeight = context.getFloat("Auto Mode Noodle Prep Container Height +A", 0.33f);
    }
}
