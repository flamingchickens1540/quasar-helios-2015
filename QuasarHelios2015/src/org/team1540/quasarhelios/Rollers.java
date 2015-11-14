package org.team1540.quasarhelios;

import ccre.channel.BooleanCell;
import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.EventOutput;
import ccre.channel.FloatCell;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.cluck.Cluck;
import ccre.frc.FRC;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;
import ccre.log.Logger;
import ccre.timers.ExpirationTimer;

public class Rollers {
    public static final boolean INPUT = false;
    public static final boolean OUTPUT = true;

    public static final BooleanCell direction = new BooleanCell(INPUT);
    public static final BooleanCell running = new BooleanCell(false);
    public static final BooleanCell closed = new BooleanCell(false);

    // These will need individual tuning for speed.
    public static final FloatOutput rightArmRoller = FRC.talon(4, FRC.MOTOR_REVERSE, 0.1f);
    public static final FloatOutput leftArmRoller = FRC.talon(5, FRC.MOTOR_FORWARD, 0.1f);
    public static final FloatOutput frontRollers = FRC.talon(6, FRC.MOTOR_REVERSE, 0.1f);
    private static final FloatOutput internalRollers = FRC.talon(7, FRC.MOTOR_REVERSE, 0.1f);

    private static final BooleanOutput leftPneumatic = FRC.solenoid(1);
    private static final BooleanOutput rightPneumatic = FRC.solenoid(2);

    public static final BooleanCell rightPneumaticOverride = new BooleanCell();
    public static final BooleanCell leftPneumaticOverride = new BooleanCell();
    public static final FloatCell rightRollerOverride = new FloatCell();
    public static final FloatCell leftRollerOverride = new FloatCell();
    public static final BooleanCell overrideRollerSpeedOnly = new BooleanCell();
    public static final BooleanCell overrideRollers = new BooleanCell();

    private static final BooleanCell flipFrontRoller = new BooleanCell();

    private static final FloatInput actualIntakeSpeed = ControlInterface.mainTuning.getFloat("Roller Speed Intake +M", 1.0f);
    private static final FloatInput actualIntakeSpeedSlow = ControlInterface.mainTuning.getFloat("Roller Speed Intake Slow +M", .3f);
    private static final FloatInput actualEjectSpeed = ControlInterface.mainTuning.getFloat("Roller Speed Eject +M", 1.0f);

    private static final FloatInput motorSpeed = running.toFloat(0, direction.toFloat(actualIntakeSpeed.negated(), actualEjectSpeed));

    private static final FloatInput amperageLeftArmRoller = CurrentMonitoring.channels[15];
    private static final FloatInput amperageRightArmRoller = CurrentMonitoring.channels[0];

    public static final EventOutput startHoldIn = () -> {
        overrideRollerSpeedOnly.set(true);
        closed.set(true);
        leftRollerOverride.set(1.0f);
        rightRollerOverride.set(1.0f);
    };
    public static final EventOutput stopHoldIn = () -> {
        overrideRollerSpeedOnly.set(false);
        closed.set(false);
        leftRollerOverride.set(0.0f);
        rightRollerOverride.set(0.0f);
    };

    public static final EventOutput startSpin = () -> {
        overrideRollerSpeedOnly.set(true);
        closed.set(true);
        leftRollerOverride.set(1.0f);
        rightRollerOverride.set(-1.0f);
    };
    public static final EventOutput stopSpin = stopHoldIn;

    // The thresholds are VERY HIGH by default so that these won't come into
    // effect unless we want to turn them on.
    private static final BooleanInput leftArmRollerHasToteRaw = amperageLeftArmRoller.atLeast(ControlInterface.mainTuning.getFloat("Roller Amperage Threshold Left +M", 1000f));
    private static final BooleanInput rightArmRollerHasToteRaw = amperageRightArmRoller.atLeast(ControlInterface.mainTuning.getFloat("Roller Amperage Threshold Right +M", 1000f));
    private static BooleanCell leftArmRollerHasTote = new BooleanCell(),
            rightArmRollerHasTote = new BooleanCell();

    public static void setup() {
        Cluck.publish("Rollers Flip Front Direction", flipFrontRoller);
        QuasarHelios.publishFault("front-roller-flipped", flipFrontRoller, flipFrontRoller.eventSet(false));

        running.setFalseWhen(FRC.startDisabled);
        flipFrontRoller.toFloat(motorSpeed, motorSpeed.negated()).send(frontRollers);
        QuasarHelios.autoHumanLoader.toFloat(motorSpeed, AutoHumanLoader.requestingRollers.toFloat(actualIntakeSpeedSlow, actualIntakeSpeed).negated()).send(internalRollers);
        FRC.globalPeriodic.send(new EventOutput() {
            private boolean wasRunning = false;
            private boolean lastDirection = INPUT;

            public void event() {
                boolean run = running.get(), dir = direction.get();
                if (run) {
                    if (wasRunning) {
                        if (lastDirection != dir) {
                            Logger.finer(direction.get() == INPUT ? "Now running rollers inward." : "Now running rollers outward.");
                        }
                    } else {
                        Logger.finer(direction.get() == INPUT ? "Started running rollers inward." : "Started running rollers outward.");
                    }
                } else if (wasRunning) {
                    Logger.finer("Stopped running rollers.");
                }
                wasRunning = run;
                lastDirection = dir;
            }
        });
        overrideRollers.send(value -> Logger.finer(value ? "Enabled roller override." : "Disabled roller override."));

        overrideRollers.or(overrideRollerSpeedOnly).toFloat(motorSpeed, leftRollerOverride.negated()).send(leftArmRoller);
        overrideRollers.or(overrideRollerSpeedOnly).toFloat(motorSpeed, rightRollerOverride.negated()).send(rightArmRoller);

        closed.andNot(overrideRollers).or(overrideRollers.and(leftPneumaticOverride)).send(leftPneumatic);
        closed.andNot(overrideRollers).or(overrideRollers.and(rightPneumaticOverride)).send(rightPneumatic);

        BooleanInput clampLow = Clamp.heightReadout.atMost(ControlInterface.mainTuning.getFloat("Clamp Rollers Close Height +M", 0.2f));
        QuasarHelios.globalControl.and(clampLow).send(() -> {
            closed.set(false);
            leftPneumaticOverride.set(false);
            rightPneumaticOverride.set(false);
        });

        FloatInput minimumTime = ControlInterface.mainTuning.getFloat("Rollers Has Tote Minimum Time +M", 0.3f);

        leftArmRollerHasTote.setFalseWhen(leftArmRollerHasToteRaw.onRelease());
        ExpirationTimer leftRollers = new ExpirationTimer();
        leftRollers.schedule(minimumTime, leftArmRollerHasTote.eventSet(true));
        leftArmRollerHasToteRaw.send(leftRollers.getRunningControl());

        rightArmRollerHasTote.setFalseWhen(rightArmRollerHasToteRaw.onRelease());
        ExpirationTimer rightRollers = new ExpirationTimer();
        rightRollers.schedule(minimumTime, rightArmRollerHasTote.eventSet(true));
        rightArmRollerHasToteRaw.send(rightRollers.getRunningControl());

        Cluck.publish("Roller Speed Left Arm", leftArmRoller);
        Cluck.publish("Roller Speed Right Arm", rightArmRoller);
        Cluck.publish("Roller Speed Front", frontRollers);
        Cluck.publish("Roller Speed Internal", internalRollers);
        Cluck.publish("Roller Closed", closed);
        Cluck.publish("Roller Closed Left", leftPneumatic);
        Cluck.publish("Roller Closed Right", rightPneumatic);
        Cluck.publish("Roller Force Open", clampLow);

        Cluck.publish("Roller Amperage Left", amperageLeftArmRoller);
        Cluck.publish("Roller Amperage Right", amperageRightArmRoller);

        Cluck.publish("Roller Has Tote Left (Raw)", leftArmRollerHasToteRaw);
        Cluck.publish("Roller Has Tote Right (Raw)", rightArmRollerHasToteRaw);

        Cluck.publish("Roller Has Tote Left", leftArmRollerHasTote.asInput());
        Cluck.publish("Roller Has Tote Right", rightArmRollerHasTote.asInput());

        Cluck.publish("(DEBUG) Roller Direction", direction);
        Cluck.publish("(DEBUG) Roller Running", running);
        Cluck.publish("(DEBUG) Roller Override", overrideRollers);
        Cluck.publish("(DEBUG) Roller Override Speed", overrideRollerSpeedOnly);

        BooleanCell runningTest = new BooleanCell();
        Cluck.publish("(DEBUG) Roller Overtest", runningTest);
        FloatInput onDelay = ControlInterface.teleTuning.getFloat("(DEBUG) Roller Test Delay On +T", 0.3f);
        FloatInput offDelay = ControlInterface.teleTuning.getFloat("(DEBUG) Roller Test Delay Off +T", 0.3f);
        new InstinctModule(runningTest) {
            @Override
            protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
                overrideRollerSpeedOnly.set(true);
                try {
                    while (true) {
                        leftRollerOverride.set(1.0f);
                        waitForTime(onDelay);
                        leftRollerOverride.set(0.0f);
                        waitForTime(offDelay);
                    }
                } finally {
                    overrideRollerSpeedOnly.set(false);
                }
            }
        };
    }
}
