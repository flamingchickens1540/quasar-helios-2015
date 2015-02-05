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
import ccre.ctrl.ExtendedMotor;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.ctrl.Ticker;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class Elevator {
    private static final ExtendedMotor winchCAN = Igneous.makeCANTalon(0);
    private static final FloatOutput winch = winchCAN.asMode(ExtendedMotor.OutputControlMode.VOLTAGE_FRACTIONAL);

    private static final BooleanStatus raising = new BooleanStatus(false);
    private static final BooleanStatus lowering = new BooleanStatus(false);
    private static final BooleanStatus goingToMiddle = new BooleanStatus(false);
    
    private static final BooleanStatus overrideEnabled = new BooleanStatus(false);
    private static final FloatStatus overrideValue = new FloatStatus(0.0f);

    public static final EventOutput setTop = EventMixing.combine(raising.getSetTrueEvent(), lowering.getSetFalseEvent(), goingToMiddle.getSetFalseEvent());
    public static final EventOutput setMiddle = goingToMiddle.getSetTrueEvent();
    public static final EventOutput setBottom = EventMixing.combine(raising.getSetFalseEvent(), lowering.getSetTrueEvent(), goingToMiddle.getSetFalseEvent());
    public static final EventOutput stop = BooleanMixing.getSetEvent(BooleanMixing.combine(raising, lowering, goingToMiddle), false);

    public static final BooleanStatus atTop = new BooleanStatus(false);
    public static final BooleanStatus atBottom = new BooleanStatus(true);
    
    private static BooleanStatus lastLimitSide = new BooleanStatus(false); // true = top, false = bottom

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
        
        BooleanInput limitTop = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(0)), Igneous.globalPeriodic);
        BooleanInput limitMiddleUpper = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(2)), Igneous.globalPeriodic);
        BooleanInput limitMiddleLower = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(3)), Igneous.globalPeriodic);
        BooleanInput limitBottom = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(1)), Igneous.globalPeriodic);
        
        atTop.setTrueWhen(EventMixing.filterEvent(raising, true, BooleanMixing.onPress(limitTop)));
        atBottom.setTrueWhen(EventMixing.filterEvent(lowering, true, BooleanMixing.onPress(limitBottom)));
        
        atTop.setFalseWhen(EventMixing.filterEvent(lowering, true, BooleanMixing.onRelease(limitTop)));
        atBottom.setFalseWhen(EventMixing.filterEvent(raising, true, BooleanMixing.onRelease(limitBottom)));
        
        atTop.setFalseWhen(BooleanMixing.onPress(BooleanMixing.orBooleans(limitMiddleUpper, limitMiddleLower, limitBottom)));
        atBottom.setFalseWhen(BooleanMixing.onPress(BooleanMixing.orBooleans(limitMiddleUpper, limitMiddleLower, limitTop)));
        
        raising.setFalseWhen(EventMixing.filterEvent(atTop, true, Igneous.globalPeriodic));
        lowering.setFalseWhen(EventMixing.filterEvent(atBottom, true, Igneous.globalPeriodic));

        Cluck.publish("Elevator Stop", stop);
        Cluck.publish("Elevator Top", setTop);
        Cluck.publish("Elevator Bottom", setBottom);
        Cluck.publish("Elevator Middle", setMiddle);
        
        Cluck.publish("Elevator Raising", raising);
        Cluck.publish("Elevator Lowering", lowering);
        Cluck.publish("Elevator Aligning", goingToMiddle);

        lastLimitSide.setTrueWhen(BooleanMixing.onPress(BooleanMixing.orBooleans(limitTop, limitMiddleUpper)));
        lastLimitSide.setFalseWhen(BooleanMixing.onPress(BooleanMixing.orBooleans(limitBottom, limitMiddleLower)));

        InstinctModule module = new InstinctModule() {
            @Override
            protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
                while (!(limitMiddleLower.get() && limitMiddleUpper.get())) {
                    if (lastLimitSide.get()) {
                        raising.set(false);
                        lowering.set(true);
                        waitUntil(BooleanMixing.invert((BooleanInputPoll) lastLimitSide));
                    } else {
                        raising.set(true);
                        lowering.set(false);
                        waitUntil(lastLimitSide);
                    }
                }

                raising.set(false);
                lowering.set(false);
                goingToMiddle.set(false);
            }
        };

        module.setShouldBeRunning(goingToMiddle);
        module.updateWhen(QuasarHelios.globalControl);

        /*
        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(raising,
                Mixing.select(lowering, FloatMixing.always(0.0f), FloatMixing.negate(winchSpeed)),
                Mixing.select(lowering, winchSpeed, FloatMixing.always(0.0f))), winch);*/
        
        FloatInputPoll main = Mixing.quadSelect(raising, lowering, FloatMixing.always(0.0f), FloatMixing.negate(winchSpeed), winchSpeed, FloatMixing.always(0.0f));
        FloatInputPoll override = new FloatInputPoll() {
            @Override
            public float get() {
                float f = overrideValue.get();
                if (limitTop.get()) {
                    f = Math.min(0, f);
                }
                
                if (limitBottom.get()) {
                    f = Math.max(0, f);
                }
                
                return f;
            }
        };
        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select((BooleanInputPoll) overrideEnabled, main, override), winch);

        Cluck.publish(QuasarHelios.testPrefix + "Elevator Motor Speed", winch);
        Cluck.publish(QuasarHelios.testPrefix + "Elevator Limit Top", limitTop);
        Cluck.publish(QuasarHelios.testPrefix + "Elevator Limit Bottom", limitBottom);
        Cluck.publish(QuasarHelios.testPrefix + "Elevator Limit Middle Upper", limitMiddleUpper);
        Cluck.publish(QuasarHelios.testPrefix + "Elevator Limit Middle Lower", limitMiddleLower);
    }
}
