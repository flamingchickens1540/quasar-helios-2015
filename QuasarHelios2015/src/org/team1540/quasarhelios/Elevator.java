package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.ExtendedMotor;
import ccre.ctrl.ExtendedMotorFailureException;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.ctrl.Ticker;
import ccre.igneous.Igneous;
import ccre.log.Logger;
import ccre.util.Utils;

public class Elevator {
    private static final ExtendedMotor winchCAN = Igneous.makeCANTalon(0);
    private static final FloatOutput winch;

    static {
        FloatOutput raw = FloatMixing.ignoredFloatOutput;
        try {
            raw = winchCAN.asMode(ExtendedMotor.OutputControlMode.VOLTAGE_FRACTIONAL);
        } catch (ExtendedMotorFailureException e) {
            Logger.severe("Could not initialize elevator CAN", e);
        }
        FloatStatus winchS = new FloatStatus();
        winch = winchS;
        final FloatOutput target = raw;
        BooleanStatus doRamping = new BooleanStatus(true);
        Cluck.publish("Elevator Ramping Enabled", doRamping);
        Igneous.constantPeriodic.send(new EventOutput() {
            private float last = winchS.get();

            public void event() {
                float wanted = winchS.get();
                if (Math.abs(wanted) < Math.abs(last) || !doRamping.get()) {
                    last = wanted;
                } else {
                    last = Utils.updateRamping(last, wanted, 0.05f);
                }
                target.set(last);
            }
        });
    }

    private static final BooleanStatus raising = new BooleanStatus();
    private static final BooleanStatus lowering = new BooleanStatus();

    public static final BooleanStatus overrideEnabled = new BooleanStatus();
    public static final FloatStatus overrideValue = new FloatStatus(0.0f);

    public static final EventOutput setTop = EventMixing.combine(lowering.getSetFalseEvent(), raising.getSetTrueEvent());
    public static final EventOutput setBottom = EventMixing.combine(raising.getSetFalseEvent(), lowering.getSetTrueEvent());
    public static final EventOutput stop = BooleanMixing.getSetEvent(BooleanMixing.combine(raising, lowering), false);

    private static final BooleanStatus atTopStatus = new BooleanStatus();
    private static final BooleanStatus atBottomStatus = new BooleanStatus();

    public static final BooleanInput atTop = atTopStatus;
    public static final BooleanInput atBottom = atBottomStatus;

    private static FloatInput winchSpeed = ControlInterface.mainTuning.getFloat("Elevator Winch Speed +M", 1.0f);

    public static void setup() {

        Ticker updateCAN = new Ticker(100);
        Cluck.publish("Elevator CAN Enable", winchCAN.asEnable());
        Cluck.publish("Elevator CAN Bus Voltage", FloatMixing.createDispatch(winchCAN.asStatus(ExtendedMotor.StatusType.BUS_VOLTAGE), updateCAN));
        Cluck.publish("Elevator CAN Output Current", FloatMixing.createDispatch(winchCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT), updateCAN));
        Cluck.publish("Elevator CAN Output Voltage", FloatMixing.createDispatch(winchCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_VOLTAGE), updateCAN));
        Cluck.publish("Elevator CAN Temperature", FloatMixing.createDispatch(winchCAN.asStatus(ExtendedMotor.StatusType.TEMPERATURE), updateCAN));
        Cluck.publish("Elevator CAN Any Fault", QuasarHelios.publishFault("elevator-can", winchCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.ANY_FAULT)));
        Cluck.publish("Elevator CAN Bus Voltage Fault", BooleanMixing.createDispatch(winchCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.BUS_VOLTAGE_FAULT), updateCAN));
        Cluck.publish("Elevator CAN Temperature Fault", BooleanMixing.createDispatch(winchCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.TEMPERATURE_FAULT), updateCAN));

        BooleanInput limitTop = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(0)), Igneous.constantPeriodic);
        BooleanInput limitBottom = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(1)), Igneous.constantPeriodic);

        BooleanInputPoll reallyRaising = BooleanMixing.orBooleans(
                BooleanMixing.andBooleans(BooleanMixing.invert((BooleanInput) overrideEnabled), raising),
                BooleanMixing.andBooleans(overrideEnabled, FloatMixing.floatIsAtLeast(overrideValue, 0)));

        BooleanInputPoll reallyLowering = BooleanMixing.orBooleans(
                BooleanMixing.andBooleans(BooleanMixing.invert((BooleanInput) overrideEnabled), lowering),
                BooleanMixing.andBooleans(overrideEnabled, FloatMixing.floatIsAtMost(overrideValue, 0)));

        atTopStatus.setTrueWhen(EventMixing.filterEvent(reallyRaising, true, BooleanMixing.onPress(limitTop)));
        atBottomStatus.setTrueWhen(EventMixing.filterEvent(reallyLowering, true, BooleanMixing.onPress(limitBottom)));

        atTopStatus.setFalseWhen(EventMixing.filterEvent(reallyLowering, true, BooleanMixing.onRelease(limitTop)));
        atBottomStatus.setFalseWhen(EventMixing.filterEvent(reallyRaising, true, BooleanMixing.onRelease(limitBottom)));

        atTopStatus.setFalseWhen(BooleanMixing.onPress(limitBottom));
        atBottomStatus.setFalseWhen(BooleanMixing.onPress(limitTop));

        raising.setFalseWhen(EventMixing.filterEvent(atTop, true, Igneous.constantPeriodic));
        lowering.setFalseWhen(EventMixing.filterEvent(atBottom, true, Igneous.constantPeriodic));

        Cluck.publish("Elevator Moving Stop", stop);
        Cluck.publish("Elevator Position Top", setTop);
        Cluck.publish("Elevator Position Bottom", setBottom);

        Cluck.publish("Elevator Moving Raising", raising);
        Cluck.publish("Elevator Moving Lowering", lowering);

        FloatInputPoll main = Mixing.quadSelect(raising, lowering, FloatMixing.always(0.0f), FloatMixing.negate(winchSpeed), winchSpeed, FloatMixing.always(0.0f));
        QuasarHelios.publishFault("elevator-both-directions", BooleanMixing.andBooleans(raising, lowering));
        FloatInputPoll override = () -> {
            float f = overrideValue.get();
            if (atTop.get()) {
                f = Math.min(0, f);
            }

            if (atBottom.get()) {
                f = Math.max(0, f);
            }

            return f;
        };

        BooleanInputPoll maxCurrentNow = FloatMixing.floatIsAtLeast(winchCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT),
                ControlInterface.mainTuning.getFloat("Elevator Max Current Amps +M", 45));
        EventInput maxCurrentEvent = EventMixing.filterEvent(maxCurrentNow, true, Igneous.constantPeriodic);

        QuasarHelios.publishStickyFault("elevator-current-fault", maxCurrentEvent);

        BooleanMixing.setWhen(maxCurrentEvent, BooleanMixing.combine(raising, lowering), false);

        FloatMixing.pumpWhen(QuasarHelios.constantControl, Mixing.select((BooleanInputPoll) overrideEnabled, main, override), winch);

        FloatInput elevatorTimeout = ControlInterface.mainTuning.getFloat("Elevator Timeout +M", 3.0f);

        ExpirationTimer timer = new ExpirationTimer();
        EventInput elevatorTimedOut = timer.schedule(elevatorTimeout);
        QuasarHelios.publishStickyFault("elevator-timeout-fault", elevatorTimedOut);
        BooleanMixing.setWhen(elevatorTimedOut, BooleanMixing.combine(raising, lowering), false);

        BooleanMixing.xorBooleans(raising, lowering).send(timer.getRunningControl());

        Cluck.publish("Elevator Winch Speed Output", winch);
        Cluck.publish("Elevator Limit Top", limitTop);
        Cluck.publish("Elevator Limit Bottom", limitBottom);
        Cluck.publish("Elevator Max Current Amps Reached", maxCurrentEvent);
    }
}
