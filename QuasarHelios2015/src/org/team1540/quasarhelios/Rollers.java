package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanOutput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;
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
    public static final FloatOutput rightArmRoller = Igneous.makeTalonMotor(4, Igneous.MOTOR_REVERSE, 0.1f);
    public static final FloatOutput leftArmRoller = Igneous.makeTalonMotor(5, Igneous.MOTOR_FORWARD, 0.1f);
    public static final FloatOutput frontRollers = Igneous.makeTalonMotor(6, Igneous.MOTOR_REVERSE, 0.1f);
    private static final FloatOutput internalRollers = Igneous.makeTalonMotor(7, Igneous.MOTOR_REVERSE, 0.1f);

    private static final BooleanOutput leftPneumatic = Igneous.makeSolenoid(1);
    private static final BooleanOutput rightPneumatic = Igneous.makeSolenoid(2);

    public static final BooleanStatus rightPneumaticOverride = new BooleanStatus();
    public static final BooleanStatus leftPneumaticOverride = new BooleanStatus();
    public static final FloatStatus rightRollerOverride = new FloatStatus();
    public static final FloatStatus leftRollerOverride = new FloatStatus();
    public static final BooleanStatus overrideRollerSpeedOnly = new BooleanStatus();
    public static final BooleanStatus overrideRollers = new BooleanStatus();

    private static final FloatInput actualIntakeSpeed = ControlInterface.mainTuning.getFloat("Roller Speed Intake +M", 1.0f);
    private static final FloatInput actualIntakeSpeedSlow = ControlInterface.mainTuning.getFloat("Roller Speed Intake Slow +M", .3f);
    private static final FloatInput actualEjectSpeed = ControlInterface.mainTuning.getFloat("Roller Speed Eject +M", 1.0f);

    private static final FloatInputPoll motorSpeed = Mixing.quadSelect(running, direction, FloatMixing.always(0.0f), FloatMixing.always(0.0f), FloatMixing.negate(actualIntakeSpeed), actualEjectSpeed);

    private static final FloatInput amperageLeftArmRoller = CurrentMonitoring.channels[15];
    private static final FloatInput amperageRightArmRoller = CurrentMonitoring.channels[0];

    public static final EventOutput startHoldIn = EventMixing.combine(overrideRollerSpeedOnly.getSetTrueEvent(), closed.getSetTrueEvent(), FloatMixing.getSetEvent(FloatMixing.combine(leftRollerOverride, rightRollerOverride), 1.0f));
    public static final EventOutput stopHoldIn = EventMixing.combine(overrideRollerSpeedOnly.getSetFalseEvent(), closed.getSetFalseEvent(), FloatMixing.getSetEvent(FloatMixing.combine(leftRollerOverride, rightRollerOverride), 0.0f));

    public static final EventOutput startSpin = EventMixing.combine(overrideRollerSpeedOnly.getSetTrueEvent(), closed.getSetTrueEvent(), FloatMixing.getSetEvent(leftRollerOverride, 1.0f), FloatMixing.getSetEvent(rightRollerOverride, -1.0f));
    public static final EventOutput stopSpin = EventMixing.combine(overrideRollerSpeedOnly.getSetFalseEvent(), closed.getSetFalseEvent(), FloatMixing.getSetEvent(FloatMixing.combine(leftRollerOverride, rightRollerOverride), 0.0f));

    // The thresholds are VERY HIGH by default so that these won't come into effect unless we want to turn them on.
    private static final BooleanInput leftArmRollerHasToteRaw = FloatMixing.floatIsAtLeast(amperageLeftArmRoller,
            ControlInterface.mainTuning.getFloat("Roller Amperage Threshold Left +M", 1000f));
    private static final BooleanInput rightArmRollerHasToteRaw = FloatMixing.floatIsAtLeast(amperageRightArmRoller,
            ControlInterface.mainTuning.getFloat("Roller Amperage Threshold Right +M", 1000f));
    private static BooleanStatus leftArmRollerHasTote = new BooleanStatus(),
            rightArmRollerHasTote = new BooleanStatus();

    public static void setup() {
        running.setFalseWhen(Igneous.startDisabled);
        FloatMixing.pumpWhen(QuasarHelios.globalControl, motorSpeed, frontRollers);
        FloatMixing.pumpWhen(EventMixing.filterEvent(QuasarHelios.autoHumanLoader, false, QuasarHelios.globalControl), motorSpeed, internalRollers);
        FloatMixing.pumpWhen(EventMixing.filterEvent(QuasarHelios.autoHumanLoader, true, QuasarHelios.globalControl),
                Mixing.select(AutoHumanLoader.requestingRollers, FloatMixing.negate(actualIntakeSpeedSlow), FloatMixing.negate(actualIntakeSpeed)),
                internalRollers);
        Igneous.globalPeriodic.send(new EventOutput() {
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
        overrideRollers.send(new BooleanOutput() {
            public void set(boolean value) {
                Logger.finer(value ? "Enabled roller override." : "Disabled roller override.");
            }
        });
        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(BooleanMixing.orBooleans(overrideRollers, overrideRollerSpeedOnly), motorSpeed, FloatMixing.negate((FloatInput) leftRollerOverride)), leftArmRoller);
        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(BooleanMixing.orBooleans(overrideRollers, overrideRollerSpeedOnly), motorSpeed, FloatMixing.negate((FloatInput) rightRollerOverride)), rightArmRoller);

        BooleanInput normalPneumatics = BooleanMixing.andBooleans(overrideRollers.asInvertedInput(), closed);
        BooleanInput overrideLeft = BooleanMixing.andBooleans(overrideRollers.asInput(), leftPneumaticOverride);
        BooleanInput overrideRight = BooleanMixing.andBooleans(overrideRollers.asInput(), rightPneumaticOverride);

        BooleanMixing.pumpWhen(QuasarHelios.globalControl, BooleanMixing.orBooleans(normalPneumatics, overrideLeft), leftPneumatic);
        BooleanMixing.pumpWhen(QuasarHelios.globalControl, BooleanMixing.orBooleans(normalPneumatics, overrideRight), rightPneumatic);

        BooleanInputPoll clampLow = FloatMixing.floatIsAtMost(Clamp.heightReadout, ControlInterface.mainTuning.getFloat("Clamp Rollers Close Height +M", 0.2f));
        BooleanMixing.setWhen(EventMixing.filterEvent(clampLow, true, QuasarHelios.globalControl), BooleanMixing.combine(closed, leftPneumaticOverride, rightPneumaticOverride), false);

        FloatInputPoll minimumTime = ControlInterface.mainTuning.getFloat("Rollers Has Tote Minimum Time +M", 0.3f);

        leftArmRollerHasTote.setFalseWhen(BooleanMixing.onRelease(leftArmRollerHasToteRaw));
        ExpirationTimer leftRollers = new ExpirationTimer();
        leftRollers.schedule(minimumTime, leftArmRollerHasTote.getSetTrueEvent());
        leftArmRollerHasToteRaw.send(leftRollers.getRunningControl());

        rightArmRollerHasTote.setFalseWhen(BooleanMixing.onRelease(rightArmRollerHasToteRaw));
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
        Cluck.publish("Roller Force Open", BooleanMixing.createDispatch(clampLow, QuasarHelios.readoutUpdate));

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
