package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeContainerTote extends AutonomousModeBase {
    protected FloatInputPoll toteDistance;
    protected FloatInputPoll autoZoneDistance;
    protected FloatInputPoll secondDistance;
    
    private FloatInputPoll containerTurnTime;
    private FloatInputPoll autoZoneAngle;
    private FloatInputPoll autoZoneSpeed;
    private FloatInputPoll nudge;
    private FloatInputPoll containerHeight;
    private FloatInputPoll topClampHeight;

    public AutonomousModeContainerTote() {
        super("Container, then Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        waitUntilNot(Clamp.waitingForAutoCalibration);

        // Pickup container.
        pickupContainer(nudge.get());
        setClampHeight(topClampHeight.get());
        singleSideTurn((long) (containerTurnTime.get() * 1000), false);

        collectTote();
        
        // Motion.
        turn(autoZoneAngle.get(), true);
        waitForTime(500);
        drive(autoZoneDistance.get(), autoZoneSpeed.get());
    }

    public void loadSettings(TuningContext context) {
        this.toteDistance = context.getFloat("Auto Mode Container Tote Tote Distance +A", 28.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Container Tote Auto Zone Distance +A", 60.0f);
        this.secondDistance = context.getFloat("Auto Mode Container Tote Second Distance +A", 24.0f);
        this.nudge = context.getFloat("Auto Mode Container Tote Nudge +A", 12.0f);
        this.containerTurnTime = context.getFloat("Auto Mode Container Tote Container Turn Time +A", 0.5f);
        this.autoZoneAngle = context.getFloat("Auto Mode Container Tote Auto Zone Angle +A", 90.0f);
        this.containerHeight = context.getFloat("Auto Mode Container Tote Container Height +A", 0.0f);
        this.autoZoneSpeed = context.getFloat("Auto Mode Container Tote Auto Zone Speed +A", 1.0f);
        this.topClampHeight = context.getFloat("Auto Mode Container Tote Top Clamp Height +A", 0.75f);
    }

}
