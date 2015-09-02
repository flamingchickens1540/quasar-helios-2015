package org.team1540.quasarhelios;

import ccre.channel.BooleanOutput;
import ccre.channel.FloatInput;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeThreeTotes extends AutonomousModeBase {
    public AutonomousModeThreeTotes() {
        super("Three Totes");
    }

    private FloatInput nudge1, nudge2;
    private FloatInput hallwayAngle1, hallwayAngle2;
    private FloatInput distanceToSecondTote;
    private FloatInput distanceToThirdTote;
    private FloatInput autoZoneAngle;
    private FloatInput autoZoneDistance;
    private FloatInput toteAngle, toteAngle2;
    private FloatInput adjustmentAngle2;

    private BooleanOutput closed = Rollers.leftPneumaticOverride.combine(Rollers.rightPneumaticOverride);

    @Override
    protected void runAutonomous() throws InterruptedException, AutonomousModeOverException {
        try {
            // Setup rollers for later.
            closed.set(false);
            Rollers.leftRollerOverride.set(-1.0f);
            Rollers.rightRollerOverride.set(1.0f);

            float startAngle = HeadingSensor.absoluteYaw.get();
            collectToteFastStart();

            waitForTime(1500);

            Rollers.overrideRollers.set(true);
            Rollers.leftPneumaticOverride.set(true);
            Rollers.rightPneumaticOverride.set(true);

            try {
                singleSideTurnAbsolute(startAngle, -hallwayAngle1.get(), true, false, 0.4f);
                collectToteFastEnd(false);
            } finally {
                Rollers.overrideRollers.set(false);
            }

            Rollers.direction.set(Rollers.OUTPUT);
            Rollers.running.set(true);

            drive(distanceToSecondTote.get());
            straightening.set(false);
            // Get second tote
            Rollers.closed.set(false);
            Rollers.running.set(false);
            turn(toteAngle.get(), false, 0.4f);
            drive(nudge1.get(), 0.7f);
            waitUntilNot(AutoLoader.crateInPosition);// finish loading last tote
            collectToteFastStart();
            turn(adjustmentAngle2.get(), false);
            straightening.set(false);
            drive(1.5f);
            straightening.set(false);
            turnAbsolute(startAngle, -hallwayAngle2.get(), false, 0.4f);
            straightening.set(false);
            drive(distanceToThirdTote.get());
            straightening.set(false);
            // Get third tote
            turn(toteAngle2.get(), false, 0.4f);
            collectToteFastEnd(true);
            straightening.set(false);
            drive(nudge2.get());
            straightening.set(false);
            Rollers.leftArmRoller.set(1.0f);
            Rollers.rightArmRoller.set(1.0f);
            Rollers.overrideRollers.set(true);
            waitUntilNot(AutoLoader.crateInPosition);
            Rollers.overrideRollers.set(false);
            collectToteFastStart();
            collectToteFastEnd(true);
            straightening.set(false);
            turnAbsolute(startAngle, -autoZoneAngle.get(), true, 0.4f);
            // Go to autozone
            straightening.set(false);
            QuasarHelios.autoHumanLoader.set(false);
            Elevator.setBottom.event();
            drive(autoZoneDistance.get(), 1.0f);
            ejectTotes();
        } finally {
            Rollers.overrideRollers.set(false);
        }
    }

    protected void driveToTote(FloatInput distance) throws AutonomousModeOverException, InterruptedException {
        Rollers.overrideRollers.set(true);
        straightening.set(false);
        drive(distance.get());
        straightening.set(false);

        Rollers.overrideRollers.set(false);
    }

    public void loadSettings(TuningContext context) {
        this.nudge1 = context.getFloat("Auto Mode Three Totes Nudge 1 +A", 1f);
        this.nudge2 = context.getFloat("Auto Mode Three Totes Nudge 2 +A", 20f);
        this.adjustmentAngle2 = context.getFloat("Auto Mode Three Totes Adjustment Angle 2 +A", -2.0f);
        this.hallwayAngle1 = context.getFloat("Auto Mode Three Totes Hallway Angle 1 +A", 20.0f);
        this.hallwayAngle2 = context.getFloat("Auto Mode Three Totes Hallway Angle 2 +A", 10.0f);
        this.distanceToSecondTote = context.getFloat("Auto Mode Three Totes Distance To Second Tote +A", 36.0f);
        this.distanceToThirdTote = context.getFloat("Auto Mode Three Totes Distance To Third Tote +A", 60.0f);
        this.autoZoneAngle = context.getFloat("Auto Mode Three Totes Auto Zone Angle +A", 100.0f);
        this.autoZoneDistance = context.getFloat("Auto Mode Three Totes Auto Zone Distance +A", 72.0f);
        this.toteAngle = context.getFloat("Auto Mode Three Totes Tote Angle +A", 20.0f);
        this.toteAngle2 = context.getFloat("Auto Mode Three Totes Tote Angle 2 +A", 20.0f);
    }
}
