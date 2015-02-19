package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeOneTote extends AutonomousModeBase {
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

    public AutonomousModeOneTote() {
        super("One Tote");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        // Loading
        collectTote();
        setClampOpen(true);
        setClampHeight(0.0f);
        singleSideTurn((long) (containerTurnTime.get() * 1000), true);
        pickupContainer(nudge.get());
        // Motion
        turn(-autoZoneAngle.get(), true);
        waitForTime(500);
        DriveCode.octocanumShifting.set(true);
        drive(autoZoneDistance.get(), autoZoneSpeed.get());
        DriveCode.octocanumShifting.set(false);
        waitForTime(500);
        // Unload
        ejectTotes();
        //strafe(STRAFE_LEFT, containerDepositStrafeTime.get());
        turn(turnAngle.get(), true);
        waitForTime(300);
        drive(secondDistance.get(), autoZoneSpeed.get());
        waitForTime(300);
        depositContainer(containerHeight.get());
        //drive(-returnDistance.get());
        setClampHeight(1.0f);
    }

    public void loadSettings(TuningContext context) {
        this.turnAngle = context.getFloat("Auto Mode Single Tote Turn Angle +A", -45.0f);
        this.toteDistance = context.getFloat("Auto Mode Single Tote Tote Distance +A", 28.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Single Tote Auto Zone Distance +A", 60.0f);
        this.secondDistance = context.getFloat("Auto Mode Single Tote Second Distance +A", 24.0f);
        this.nudge = context.getFloat("Auto Mode Single Tote Nudge +A", 12.0f);
        this.containerTurnTime = context.getFloat("Auto Mode Single Tote Container Turn Time +A", 0.5f);
        this.autoZoneAngle = context.getFloat("Auto Mode Single Tote Auto Zone Angle +A", 90.0f);
        this.containerHeight = context.getFloat("Auto Mode Single Tote Container Height +A", 0.0f);
        this.containerDepositStrafeTime = context.getFloat("Auto Mode Single Tote Container Deposit Strafe Time +A", 2.0f);
        this.autoZoneSpeed = context.getFloat("Auto Mode Single Tote Auto Zone Speed", 1.0f);
    }

}
