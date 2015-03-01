package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeBump extends AutonomousModeBase {

    protected FloatInputPoll autoZoneDistance, autoZoneTime;
    private FloatInputPoll autoZoneSpeed;

    public AutonomousModeBump() {
        super("Bump");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        // Motion
        DriveCode.octocanumShifting.set(true);
        drive(autoZoneDistance.get(), autoZoneSpeed.get());
        driveForTime((long) (autoZoneTime.get() * 1000), autoZoneSpeed.get());
    }

    public void loadSettings(TuningContext context) {
        this.autoZoneSpeed = context.getFloat("Auto Mode Bump Auto Zone Speed +A", 1.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Bump Auto Zone Distance (1) +A", 60.0f);
        this.autoZoneTime = context.getFloat("Auto Mode Bump Auto Zone Time (2) +A", 1.0f);
    }

}
