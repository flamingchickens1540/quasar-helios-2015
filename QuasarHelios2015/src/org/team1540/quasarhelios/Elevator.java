package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
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
    private static final FloatOutput winch = FloatMixing.ignoredFloatOutput;//winchCAN.asMode(ExtendedMotor.OutputControlMode.VOLTAGE_FRACTIONAL);

    private static final BooleanStatus raising = new BooleanStatus();
    private static final BooleanStatus lowering = new BooleanStatus();
    private static final BooleanStatus goingToMiddle = new BooleanStatus();

    public static final EventOutput setTop = EventMixing.combine(raising.getSetTrueEvent(), lowering.getSetFalseEvent(), goingToMiddle.getSetFalseEvent());
    public static final EventOutput setMiddle = goingToMiddle.getSetTrueEvent();
    public static final EventOutput setBottom = EventMixing.combine(raising.getSetFalseEvent(), lowering.getSetTrueEvent(), goingToMiddle.getSetFalseEvent());
    public static final EventOutput stop = BooleanMixing.getSetEvent(BooleanMixing.combine(raising, lowering, goingToMiddle), false);

    public static final BooleanInput topLimitSwitch = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(0)), Igneous.globalPeriodic);
    public static final BooleanInput bottomLimitSwitch = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(1)), Igneous.globalPeriodic);
    public static final BooleanInput middleUpperLimitSwitch = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(2)), Igneous.globalPeriodic);
    public static final BooleanInput middleLowerLimitSwitch = BooleanMixing.alwaysFalse; //BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(3)), Igneous.globalPeriodic);
    private static BooleanStatus lastLimitSide = new BooleanStatus(); // true = top, false = bottom

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
        
        raising.setFalseWhen(EventMixing.filterEvent(topLimitSwitch, true, Igneous.globalPeriodic));
        lowering.setFalseWhen(EventMixing.filterEvent(bottomLimitSwitch, true, Igneous.globalPeriodic));

        Cluck.publish("Elevator Stop", stop);
        Cluck.publish("Elevator Top", setTop);
        Cluck.publish("Elevator Bottom", setBottom);
        Cluck.publish("Elevator Middle", setMiddle);
        
        Cluck.publish("Elevator Raising", raising);
        Cluck.publish("Elevator Lowering", lowering);
        Cluck.publish("Elevator Aligning", goingToMiddle);

        lastLimitSide.setTrueWhen(BooleanMixing.onPress(topLimitSwitch));
        lastLimitSide.setFalseWhen(BooleanMixing.onPress(bottomLimitSwitch));

        InstinctModule module = new InstinctModule() {
            @Override
            protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
                if (lastLimitSide.get()) {
                    raising.set(false);
                    lowering.set(true);
                } else {
                    raising.set(true);
                    lowering.set(false);
                }

                waitUntil(BooleanMixing.andBooleans(middleLowerLimitSwitch, middleUpperLimitSwitch));

                raising.set(false);
                lowering.set(false);
                goingToMiddle.set(false);
            }
        };

        module.setShouldBeRunning(goingToMiddle);
        module.updateWhen(QuasarHelios.globalControl);

        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(raising,
                Mixing.select(lowering, FloatMixing.always(0.0f), FloatMixing.negate(winchSpeed)),
                Mixing.select(lowering, winchSpeed, FloatMixing.always(0.0f))), winch);

        Cluck.publish(QuasarHelios.testPrefix + "Elevator Motor Speed", winch);
        Cluck.publish(QuasarHelios.testPrefix + "Elevator Limit Top", topLimitSwitch);
        Cluck.publish(QuasarHelios.testPrefix + "Elevator Limit Bottom", bottomLimitSwitch);
    }
}
