package org.team1540.quasarhelios;

import ccre.channel.BooleanOutput;
import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;

public class AutonomousModeThreeTotes extends AutonomousModeBase {
    private FloatInputPoll nudge;
    private FloatInputPoll nudgeToTote;
    private FloatInputPoll toteDistance;
    private FloatInputPoll autoZoneDistance;
    private FloatInputPoll autoZoneAngle;
    private FloatInputPoll autoZoneTime;
    private FloatInputPoll autoZoneSpeed;
    private FloatInputPoll toteSpeed;
    public AutonomousModeThreeTotes() {
        super("Three Totes");
    }

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        try { 
            DriveCode.octocanumShifting.set(false);

            float startAngle = HeadingSensor.absoluteYaw.get();
            BooleanOutput closed = BooleanMixing.combine(Rollers.leftPneumaticOverride, Rollers.rightPneumaticOverride);

            // Collect first tote
            collectToteWithElevator();
            Rollers.overrideRollers.set(true);

            // Move first container
            closed.set(true);
            Rollers.rightRollerOverride.set(1.0f);
            Rollers.leftRollerOverride.set(-1.0f);
            drive(nudge.get() + toteDistance.get(), toteSpeed.get());
            closed.set(false);
            waitForTime(1000);
            
            // Second tote
            drive(nudgeToTote.get(), toteSpeed.get());
            Rollers.rightArmRoller.set(0.0f);
            Rollers.leftArmRoller.set(0.0f);
            Rollers.overrideRollers.set(false);
            collectToteWithElevator();
            Rollers.overrideRollers.set(true);
            
            // Move second container
            closed.set(true);
            Rollers.rightRollerOverride.set(1.0f);
            Rollers.leftRollerOverride.set(-1.0f);
            drive(nudge.get() + toteDistance.get(), toteSpeed.get());
            Rollers.rightArmRoller.set(0.0f);
            Rollers.leftArmRoller.set(0.0f);
            closed.set(false);
            waitForTime(1000);
            
            // Collect last tote
            drive(nudgeToTote.get(), toteSpeed.get());
            Rollers.overrideRollers.set(false);
            collectToteWithElevator();
            
            // Go to Auto Zone
            DriveCode.octocanumShifting.set(true);
            turnAbsolute(startAngle, -autoZoneAngle.get(), true);
            drive(autoZoneDistance.get());
            driveForTime((long) (autoZoneTime.get() * 1000), autoZoneSpeed.get());
            
            // Drop totes off
            ejectTotes();
        } finally {
            Rollers.overrideRollers.set(false);
            DriveCode.octocanumShifting.set(true);
        }
    }

    public void loadSettings(TuningContext context) {
        this.nudge = context.getFloat("Auto Mode Three Totes Nudge +A", 11.0f);
        this.nudgeToTote = context.getFloat("Auto Mode Three Totes Nudge to Tote", 5.0f);
        this.toteDistance = context.getFloat("Auto Mode Three Totes Tote Distance +A", 62.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Three Totes Auto Zone Distance +A", 36.0f);
        this.autoZoneAngle = context.getFloat("Auto Mode Three Totes Auto Zone Angle +A", 80.0f);
        this.autoZoneTime = context.getFloat("Auto Mode Three Totes Auto Zone Time +A", 1.0f);
        this.autoZoneSpeed = context.getFloat("Auto Mode Three Totes Auto Zone Speed +A", 1.0f);
        this.toteSpeed = context.getFloat("Auto Mode Three Totes Tote Speed +A", 0.25f);
    }
}
