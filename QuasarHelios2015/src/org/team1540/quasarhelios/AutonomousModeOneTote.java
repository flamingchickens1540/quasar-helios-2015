package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeOneTote extends AutonomousModeBase {
    protected FloatInputPoll toteDistance;
    protected FloatInputPoll autoZoneDistance;
    protected FloatInputPoll returnDistance;
    
    private FloatInputPoll containerAngle;
    private FloatInputPoll autoZoneAngle;
    private FloatInputPoll nudge;
    private FloatInputPoll containerHeight;

    public AutonomousModeOneTote() {
        super("One Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        // Loading
        collectTote();
        turn(containerAngle.get(), false);
        pickupContainer(nudge.get());
        // Motion
        turn(autoZoneAngle.get(), true);
        waitForTime(500);
        drive(autoZoneDistance.get());
        waitForTime(500);
        // Unload
        ejectTotes();
        depositContainer(containerHeight.get());
        drive(-returnDistance.get());
        setClampHeight(1.0f);
    }

    public void loadSettings(TuningContext context) {
        this.toteDistance = context.getFloat("Auto Mode Single Tote Tote Distance +A", 28.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Single Tote Auto Zone Distance +A", 60.0f);
        this.returnDistance = context.getFloat("Auto Mode Single Tote Return Distance +A", 30.0f);
        this.nudge = context.getFloat("Auto Mode Single Tote Nudge +A", 12.0f);
        this.containerAngle = context.getFloat("Auto Mode Single Tote Container Angle +A", 16.0f);
        this.autoZoneAngle = context.getFloat("Auto Mode Single Tote Auto Zone Angle +A", 90.0f);
        this.containerHeight = context.getFloat("Auto Mode Single Tote Container Height +A", 0.0f);
    }

}
