package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
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
import ccre.ctrl.PIDControl;
import ccre.ctrl.Ticker;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;
import ccre.log.Logger;

public class Clamp {
    public static final boolean MODE_SPEED = true;
    public static final boolean MODE_HEIGHT = false;
    
    public static FloatStatus height = new FloatStatus();
    public static FloatStatus speed = new FloatStatus();
    public static BooleanStatus mode = new BooleanStatus();

    public static final BooleanStatus openControl = new BooleanStatus(Igneous.makeSolenoid(3));
    
    public static final EventOutput setBottom = EventMixing.combine(mode.getSetFalseEvent(), height.getSetEvent(0));

    private static final BooleanStatus useEncoder = ControlInterface.mainTuning.getBoolean("clamp-use-encoder", false);

    public static FloatInputPoll heightReadout;

    public static void setup() {

        FloatInputPoll encoder = Igneous.makeEncoder(10, 11, true);
        ExtendedMotor clampCAN = Igneous.makeCANTalon(1);
        FloatOutput motorControlTemp = FloatMixing.ignoredFloatOutput;

        try {
            motorControlTemp = clampCAN.asMode(ExtendedMotor.OutputControlMode.VOLTAGE_FRACTIONAL);
            
            if (motorControlTemp == null) {
                motorControlTemp = FloatMixing.ignoredFloatOutput;
            }
        } catch (ExtendedMotorFailureException e) {
            Logger.severe("Exception thrown when creating clamp motor", e);
        }

        final FloatOutput speedControl = FloatMixing.addRamping(0.2f, Igneous.constantPeriodic, FloatMixing.negate(motorControlTemp));

        Cluck.publish("CAN Clamp Enable", clampCAN.asEnable());
        Ticker updateCAN = new Ticker(100);
        Cluck.publish("CAN Clamp Bus Voltage", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.BUS_VOLTAGE), updateCAN));
        Cluck.publish("CAN Clamp Output Current", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT), updateCAN));
        Cluck.publish("CAN Clamp Output Voltage", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_VOLTAGE), updateCAN));
        Cluck.publish("CAN Clamp Temperature", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.TEMPERATURE), updateCAN));
        Cluck.publish("CAN Clamp Any Fault", BooleanMixing.createDispatch(clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.ANY_FAULT), updateCAN));
        Cluck.publish("CAN Clamp Bus Voltage Fault", BooleanMixing.createDispatch(clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.BUS_VOLTAGE_FAULT), updateCAN));
        Cluck.publish("CAN Clamp Temperature Fault", BooleanMixing.createDispatch(clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.TEMPERATURE_FAULT), updateCAN));

        BooleanInput limitTop = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(2)), Igneous.globalPeriodic);
        BooleanInput limitBottom = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(3)), Igneous.globalPeriodic);

        FloatStatus min = ControlInterface.mainTuning.getFloat("clamp-min", 0.0f);
        FloatStatus max = ControlInterface.mainTuning.getFloat("clamp-max", 1.0f);

        FloatMixing.pumpWhen(EventMixing.filterEvent(useEncoder, true, BooleanMixing.onPress(limitBottom)), encoder, min);
        FloatMixing.pumpWhen(EventMixing.filterEvent(useEncoder, true, BooleanMixing.onPress(limitTop)), encoder, max);

        FloatStatus p = ControlInterface.mainTuning.getFloat("clamp-p", 1.0f);
        FloatStatus i = ControlInterface.mainTuning.getFloat("clamp-i", 0.0f);
        FloatStatus d = ControlInterface.mainTuning.getFloat("clamp-d", 0.0f);

        heightReadout = FloatMixing.normalizeFloat(encoder, min, max);

        PIDControl pid = new PIDControl(heightReadout, height, p, i, d);
        
        pid.integralTotal.setWhen(0.0f, BooleanMixing.onRelease(mode));
        FloatMixing.pumpWhen(BooleanMixing.onRelease(mode), heightReadout, height);

        QuasarHelios.globalControl.send(pid);

        FloatOutput out = (value) -> {
            if (limitTop.get()) {
                value = Math.max(value, 0);
            }

            if (limitBottom.get()) {
                value = Math.min(value, 0);
            }
            speedControl.set(value);
        };
        
        mode.setTrueWhen(EventMixing.filterEvent(FloatMixing.floatIsOutsideRange(speed, -0.3f, 0.3f), true, QuasarHelios.globalControl));
        mode.setTrueWhen(Igneous.startTele);

        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(BooleanMixing.orBooleans(mode, useEncoder.asInvertedInput()), pid, speed), FloatMixing.deadzone(out, 0.1f));

        Cluck.publish(QuasarHelios.testPrefix + "Clamp Open Control", openControl);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Height Encoder", FloatMixing.createDispatch(encoder, Igneous.globalPeriodic));
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Height Scaled", FloatMixing.createDispatch(heightReadout, Igneous.globalPeriodic));
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Height Setting", height);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Integral Total", pid.integralTotal);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Top", limitTop);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Bottom", limitBottom);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Motor Speed", speedControl);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp PID Output", (FloatInput) pid);

        Cluck.publish("Clamp Max Set", FloatMixing.pumpEvent(encoder, max));
        Cluck.publish("Clamp Min Set", FloatMixing.pumpEvent(encoder, min));
    }
}
