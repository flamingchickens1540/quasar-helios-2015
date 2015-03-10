package org.team1540.quasarhelios;

import ccre.channel.BooleanOutput;
import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.log.Logger;

public class AutonomousModeThreeTotes extends AutonomousModeBase {
    public AutonomousModeThreeTotes() {
        super("Three Totes");
    }
    
    private FloatInputPoll nudge;
    private FloatInputPoll hallwayAngle;
    private FloatInputPoll distanceToSecondTote;
    private FloatInputPoll distanceToThirdTote;
    private FloatInputPoll autoZoneAngle;
    private FloatInputPoll autoZoneDistance;
    private FloatInputPoll toteAngle;
    private FloatInputPoll adjustmentAngle;

    
    private BooleanOutput closed = BooleanMixing.combine(Rollers.leftPneumaticOverride, Rollers.rightPneumaticOverride);

    @Override
    protected void runAutonomous() throws InterruptedException,
            AutonomousModeOverException {
        try {
            DriveCode.octocanumShifting.set(true);
            // Setup rollers for later.
            closed.set(false);
            Rollers.leftRollerOverride.set(-1.0f);
            Rollers.rightRollerOverride.set(1.0f);

            float startAngle = HeadingSensor.absoluteYaw.get();
            collectToteWithElevator();
            waitForTime(1000);
            turnAbsolute(startAngle, -hallwayAngle.get(), false);

            driveToTote(distanceToSecondTote);
            // Get second tote
            turnAbsolute(startAngle, toteAngle.get(), false, 0.4f);
            collectToteFromHallway(nudge);
            turnAbsolute(startAngle, -hallwayAngle.get(), false, 0.4f);
            driveToTote(distanceToThirdTote);
            // Get third tote
            turnAbsolute(startAngle, toteAngle.get(), false, 0.4f);
            collectToteFromHallway(nudge);
            turnAbsolute(startAngle, -autoZoneAngle.get(), true, 0.4f);
            // Go to autozone
            straightening.set(false);
            drive(autoZoneDistance.get());
            ejectTotes();
        } finally {
            Rollers.overrideRollers.set(false);
        }
    }
    protected void driveToTote(FloatInputPoll distance) throws AutonomousModeOverException, InterruptedException {
        Rollers.overrideRollers.set(true);
        straightening.set(false);
        drive(distance.get());
        straightening.set(false);

        Rollers.overrideRollers.set(false);
    }
    
    protected void collectToteFromHallway(FloatInputPoll nudge) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        drive(nudge.get());
        straightening.set(false);
        turn(-adjustmentAngle.get(), false);
        collectToteWithElevator();
        turn(adjustmentAngle.get(), false);
        straightening.set(false);
        drive(-nudge.get());
        straightening.set(false);

    }
    
    public void loadSettings(TuningContext context) {
        this.nudge = context.getFloat("Auto Mode Three Totes Nudge +A", 8.5f);
        this.adjustmentAngle = context.getFloat("Auto Mode Three Totes Adjustment Angle", 30.0f);
        this.hallwayAngle = context.getFloat("Auto Mode Three Totes Hallway Angle +A",  20.0f);
        this.distanceToSecondTote = context.getFloat("Auto Mode Three Totes Distance To Second Tote +A", 75.0f);
        this.distanceToThirdTote = context.getFloat("Auto Mode Three Totes Distance To Third Tote +A", 75.0f);
        this.autoZoneAngle = context.getFloat("Auto Mode Three Totes Auto Zone Angle +A", 90.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Three Totes Auto Zone Distance +A", 60.0f);
        this.toteAngle = context.getFloat("Auto Mode Three Totes Tote Angle +A", 10.0f);
    }
}
