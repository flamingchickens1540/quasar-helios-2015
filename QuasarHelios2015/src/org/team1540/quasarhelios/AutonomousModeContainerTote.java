package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeContainerTote extends AutonomousModeBase {
    protected FloatInputPoll toteDistance;
    protected FloatInputPoll autoZoneDistance;
    protected FloatInputPoll secondDistance;
    
    private FloatInputPoll containerTurnTime;
    private FloatInputPoll containerDepositStrafeTime;
    private FloatInputPoll autoZoneAngle;
    private FloatInputPoll autoZoneSpeed;
    private FloatInputPoll nudge;
    private FloatInputPoll containerHeight;
    private FloatInputPoll turnAngle;

    public AutonomousModeContainerTote() {
        super("Container, then Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        // Pickup container.
        pickupContainer(nudge.get());
        setClampHeight(1.0f);
        singleSideTurn((long) (containerTurnTime.get() * 1000), false);

        // Load tote.
        
        // Move elevator.
        boolean elevatorAtTop = Elevator.atTop.get();
        if (!elevatorAtTop) {
            Elevator.setTop.event();
        }

        // Move clamp.
        float currentClampHeight = Clamp.heightReadout.get();
        if (currentClampHeight < AutoLoader.clampHeightThreshold.get()) {
            Clamp.mode.set(Clamp.MODE_HEIGHT);
            Clamp.height.set(AutoLoader.clampHeightThreshold.get());
            if (elevatorAtTop) {
                waitUntil(Clamp.atDesiredHeight);
            } else {
                waitUntil(BooleanMixing.andBooleans(Clamp.atDesiredHeight, Elevator.atTop));
            }
        } else {
            if (!elevatorAtTop) {
                waitUntil(Elevator.atTop);
            }
        }
        
        // Run rollers.
        Rollers.direction.set(Rollers.REVERSE);
        Rollers.running.set(true);
        Rollers.closed.set(true);

        waitUntil(AutoLoader.crateInPosition);
        
        Rollers.running.set(false);
        Rollers.closed.set(false);
        
        // Motion
        turn(autoZoneAngle.get(), true);
        waitForTime(500);
        drive(autoZoneDistance.get(), autoZoneSpeed.get());
        waitForTime(500);
        // Unload
        depositContainer(containerHeight.get());
    }

    public void loadSettings(TuningContext context) {
        this.turnAngle = context.getFloat("Auto Mode Container Tote Turn Angle +A", -45.0f);
        this.toteDistance = context.getFloat("Auto Mode Container Tote Tote Distance +A", 28.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Container Tote Auto Zone Distance +A", 60.0f);
        this.secondDistance = context.getFloat("Auto Mode Container Tote Second Distance +A", 24.0f);
        this.nudge = context.getFloat("Auto Mode Container Tote Nudge +A", 12.0f);
        this.containerTurnTime = context.getFloat("Auto Mode Container Tote Container Turn Time +A", 0.5f);
        this.autoZoneAngle = context.getFloat("Auto Mode Container Tote Auto Zone Angle +A", 90.0f);
        this.containerHeight = context.getFloat("Auto Mode Container Tote Container Height +A", 0.0f);
        this.containerDepositStrafeTime = context.getFloat("Auto Mode Container Tote Container Deposit Strafe Time +A", 2.0f);
        this.autoZoneSpeed = context.getFloat("Auto Mode Container Tote Auto Zone Speed", 1.0f);
    }

}
