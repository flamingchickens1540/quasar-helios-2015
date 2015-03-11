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
import ccre.log.Logger;

public class Rollers {
    public static final boolean INPUT = false;
    public static final boolean OUTPUT = true;

    public static final BooleanStatus direction = new BooleanStatus(INPUT);
    public static final BooleanStatus running = new BooleanStatus(false);
    public static final BooleanStatus closed = new BooleanStatus(true);

    // These will need individual tuning for speed.
    private static final FloatOutput rightArmRoller = Igneous.makeTalonMotor(4, Igneous.MOTOR_REVERSE, 0.1f);
    private static final FloatOutput leftArmRoller = Igneous.makeTalonMotor(5, Igneous.MOTOR_FORWARD, 0.1f);
    private static final FloatOutput frontRollers = Igneous.makeTalonMotor(6, Igneous.MOTOR_REVERSE, 0.1f);
    private static final FloatOutput internalRollers = Igneous.makeTalonMotor(7, Igneous.MOTOR_REVERSE, 0.1f);

    private static final BooleanOutput leftPneumatic = Igneous.makeSolenoid(1);
    private static final BooleanOutput rightPneumatic = Igneous.makeSolenoid(2);

    public static final BooleanStatus rightPneumaticOverride = new BooleanStatus();
    public static final BooleanStatus leftPneumaticOverride = new BooleanStatus();
    public static final FloatStatus rightRollerOverride = new FloatStatus();
    public static final FloatStatus leftRollerOverride = new FloatStatus();
    public static final BooleanStatus overrideRollers = new BooleanStatus();

    private static final FloatInput actualSpeed = ControlInterface.mainTuning.getFloat("Roller Speed +M", 1.0f);
    private static final FloatInputPoll motorSpeed = Mixing.quadSelect(running, direction, FloatMixing.always(0.0f), FloatMixing.always(0.0f), FloatMixing.negate(actualSpeed), actualSpeed);

    private static final FloatInput amperageLeftArmRoller = CurrentMonitoring.channels[15];
    private static final FloatInput amperageRightArmRoller = CurrentMonitoring.channels[0];

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
                Mixing.select(AutoHumanLoader.requestingRollers, FloatMixing.always(0), FloatMixing.negate(actualSpeed)),
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
        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(overrideRollers, motorSpeed, FloatMixing.negate((FloatInput) leftRollerOverride)), leftArmRoller);
        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(overrideRollers, motorSpeed, FloatMixing.negate((FloatInput) rightRollerOverride)), rightArmRoller);

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
    }
}
