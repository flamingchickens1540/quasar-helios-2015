package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.ExtendedMotor;
import ccre.ctrl.ExtendedMotorFailureException;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.ctrl.Ticker;
import ccre.igneous.Igneous;

public class Elevator {
    private static final ExtendedMotor winchCAN = Igneous.makeCANTalon(0);
    private static final FloatOutput winch;

    static {
        try {
            winch = winchCAN.asMode(ExtendedMotor.OutputControlMode.VOLTAGE_FRACTIONAL);
        } catch (ExtendedMotorFailureException e) {
            throw new RuntimeException(e);
        }
    }

    private static final BooleanStatus raising = new BooleanStatus();
    private static final BooleanStatus lowering = new BooleanStatus();

    public static final BooleanStatus overrideEnabled = new BooleanStatus();
    public static final FloatStatus overrideValue = new FloatStatus(0.0f);

    public static final EventOutput setTop = EventMixing.combine(raising.getSetTrueEvent(), lowering.getSetFalseEvent());
    public static final EventOutput setBottom = EventMixing.combine(raising.getSetFalseEvent(), lowering.getSetTrueEvent());
    public static final EventOutput stop = BooleanMixing.getSetEvent(BooleanMixing.combine(raising, lowering), false);

    private static final BooleanStatus atTopStatus = new BooleanStatus();
    private static final BooleanStatus atBottomStatus = new BooleanStatus();
    
    public static final BooleanInput atTop = atTopStatus;
    public static final BooleanInput atBottom = atBottomStatus;

    private static FloatInput winchSpeed = ControlInterface.mainTuning.getFloat("main-elevator-speed", 1.0f);

    public static void setup() {

        Ticker updateCAN = new Ticker(100);
        Cluck.publish("CAN Elevator Enable", winchCAN.asEnable());
        Cluck.publish("CAN Elevator Bus Voltage", FloatMixing.createDispatch(winchCAN.asStatus(ExtendedMotor.StatusType.BUS_VOLTAGE), updateCAN));
        Cluck.publish("CAN Elevator Output Current", FloatMixing.createDispatch(winchCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT), updateCAN));
        Cluck.publish("CAN Elevator Output Voltage", FloatMixing.createDispatch(winchCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_VOLTAGE), updateCAN));
        Cluck.publish("CAN Elevator Temperature", FloatMixing.createDispatch(winchCAN.asStatus(ExtendedMotor.StatusType.TEMPERATURE), updateCAN));
        Cluck.publish("CAN Elevator Any Fault", BooleanMixing.createDispatch(winchCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.ANY_FAULT), updateCAN));
        Cluck.publish("CAN Elevator Bus Voltage Fault", BooleanMixing.createDispatch(winchCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.BUS_VOLTAGE_FAULT), updateCAN));
        Cluck.publish("CAN Elevator Temperature Fault", BooleanMixing.createDispatch(winchCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.TEMPERATURE_FAULT), updateCAN));

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

        Cluck.publish("Elevator Stop", stop);
        Cluck.publish("Elevator Top", setTop);
        Cluck.publish("Elevator Bottom", setBottom);

        Cluck.publish("Elevator Raising", raising);
        Cluck.publish("Elevator Lowering", lowering);

        FloatInputPoll main = Mixing.quadSelect(raising, lowering, FloatMixing.always(0.0f), FloatMixing.negate(winchSpeed), winchSpeed, FloatMixing.always(0.0f));
        FloatInputPoll override = new FloatInputPoll() {
            @Override
            public float get() {
                float f = overrideValue.get();
                if (atTop.get()) {
                    f = Math.min(0, f);
                }

                if (atBottom.get()) {
                    f = Math.max(0, f);
                }

                return f;
            }
        };
        FloatMixing.pumpWhen(QuasarHelios.constantControl, Mixing.select((BooleanInputPoll) overrideEnabled, main, override), winch);

        Cluck.publish(QuasarHelios.testPrefix + "Elevator Motor Speed", winch);
        Cluck.publish(QuasarHelios.testPrefix + "Elevator Limit Top", limitTop);
        Cluck.publish(QuasarHelios.testPrefix + "Elevator Limit Bottom", limitBottom);
    }
}
