package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.ExpirationTimer;
import ccre.frc.FRC;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;
import ccre.log.Logger;

public class Rollers {
    public static final boolean INPUT = false;
    public static final boolean OUTPUT = true;

    public static final BooleanStatus direction = new BooleanStatus(INPUT);
    public static final BooleanStatus running = new BooleanStatus(false);
    public static final BooleanStatus closed = new BooleanStatus(false);

    // These will need individual tuning for speed.
    public static final FloatOutput rightArmRoller = FRC.makeTalonMotor(4, FRC.MOTOR_REVERSE, 0.1f);
    public static final FloatOutput leftArmRoller = FRC.makeTalonMotor(5, FRC.MOTOR_FORWARD, 0.1f);
    public static final FloatOutput frontRollers = FRC.makeTalonMotor(6, FRC.MOTOR_REVERSE, 0.1f);
    private static final FloatOutput internalRollers = FRC.makeTalonMotor(7, FRC.MOTOR_REVERSE, 0.1f);

    private static final BooleanOutput leftPneumatic = FRC.makeSolenoid(1);
    private static final BooleanOutput rightPneumatic = FRC.makeSolenoid(2);

    public static final BooleanStatus rightPneumaticOverride = new BooleanStatus();
    public static final BooleanStatus leftPneumaticOverride = new BooleanStatus();
    public static final FloatStatus rightRollerOverride = new FloatStatus();
    public static final FloatStatus leftRollerOverride = new FloatStatus();
    public static final BooleanStatus overrideRollerSpeedOnly = new BooleanStatus();
    public static final BooleanStatus overrideRollers = new BooleanStatus();

    private static final BooleanStatus flipFrontRoller = new BooleanStatus();

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
    public static final EventOutput stopSpin = () -> {
        overrideRollerSpeedOnly.set(false);
        closed.set(false);
        leftRollerOverride.set(0.0f);
        rightRollerOverride.set(0.0f);
    };

    // The thresholds are VERY HIGH by default so that these won't come into effect unless we want to turn them on.
    private static final BooleanInput leftArmRollerHasToteRaw = amperageLeftArmRoller.atLeast(ControlInterface.mainTuning.getFloat("Roller Amperage Threshold Left +M", 1000f));
    private static final BooleanInput rightArmRollerHasToteRaw = amperageRightArmRoller.atLeast(ControlInterface.mainTuning.getFloat("Roller Amperage Threshold Right +M", 1000f));
    private static BooleanStatus leftArmRollerHasTote = new BooleanStatus(),
            rightArmRollerHasTote = new BooleanStatus();

    public static void setup() {
        Cluck.publish("Rollers Flip Front Direction", flipFrontRoller);
        QuasarHelios.publishFault("front-roller-flipped", flipFrontRoller, flipFrontRoller.getSetFalseEvent());

        running.setFalseWhen(FRC.startDisabled);
        flipFrontRoller.toFloat(motorSpeed, motorSpeed.negated()).send(frontRollers);
        motorSpeed.send(internalRollers.filterNot(QuasarHelios.autoHumanLoader));
        AutoHumanLoader.requestingRollers.toFloat(actualIntakeSpeedSlow, actualIntakeSpeed).negated().send(internalRollers.filter(QuasarHelios.autoHumanLoader));

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

        BooleanInput normalPneumatics = overrideRollers.not().and(closed);
        BooleanInput overrideLeft = overrideRollers.and(leftPneumaticOverride);
        BooleanInput overrideRight = overrideRollers.and(rightPneumaticOverride);

        normalPneumatics.or(overrideLeft).send(leftPneumatic);
        normalPneumatics.or(overrideRight).send(rightPneumatic);

        BooleanInput clampLow = Clamp.heightReadout.atMost(ControlInterface.mainTuning.getFloat("Clamp Rollers Close Height +M", 0.2f));
        closed.combine(leftPneumaticOverride).combine(rightPneumaticOverride).setFalseWhen(QuasarHelios.globalControl.and(clampLow));

        FloatInput minimumTime = ControlInterface.mainTuning.getFloat("Rollers Has Tote Minimum Time +M", 0.3f);

        leftArmRollerHasTote.setFalseWhen(leftArmRollerHasToteRaw.onRelease());
        ExpirationTimer leftRollers = new ExpirationTimer();
        leftRollers.schedule(minimumTime, leftArmRollerHasTote.getSetTrueEvent());
        leftArmRollerHasToteRaw.send(leftRollers.getRunningControl());

        rightArmRollerHasTote.setFalseWhen(rightArmRollerHasToteRaw.onRelease());
        ExpirationTimer rightRollers = new ExpirationTimer();
        rightRollers.schedule(minimumTime, rightArmRollerHasTote.getSetTrueEvent());
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

        BooleanStatus runningTest = new BooleanStatus();
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
