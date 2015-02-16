package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanOutput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.EventStatus;
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
import ccre.log.Logger;

public class Clamp {
    public static final boolean MODE_SPEED = true;
    public static final boolean MODE_HEIGHT = false;

    public static FloatStatus height = new FloatStatus();
    public static FloatStatus speed = new FloatStatus();
    public static BooleanStatus mode = new BooleanStatus(MODE_SPEED);

    public static final BooleanStatus openControl = new BooleanStatus(Igneous.makeSolenoid(3));

    public static final EventOutput setBottom = EventMixing.combine(mode.getSetFalseEvent(), height.getSetEvent(0));

    private static final BooleanStatus useEncoder = ControlInterface.mainTuning.getBoolean("clamp-use-encoder", true);

    public static FloatInputPoll heightReadout;

    public static final FloatInputPoll heightPadding = ControlInterface.autoTuning.getFloat("clamp-height-padding", 0.01f);

    public static void setup() {
        QuasarHelios.publishFault("clamp-encoder-disabled", BooleanMixing.invert((BooleanInputPoll) useEncoder));

        EventStatus zeroEncoder = new EventStatus();

        FloatInputPoll encoder = Igneous.makeEncoder(10, 11, true, zeroEncoder);
        Igneous.startAuto.send(zeroEncoder);

        QuasarHelios.publishFault("clamp-encoder-zero", FloatMixing.floatIsInRange(encoder, -0.1f, 0.1f));

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

        final FloatStatus speedControl = new FloatStatus(FloatMixing.addRamping(0.2f, Igneous.constantPeriodic, FloatMixing.negate(motorControlTemp)));

        Cluck.publish("CAN Clamp Enable", clampCAN.asEnable());
        Ticker updateCAN = new Ticker(100);
        Cluck.publish("CAN Clamp Bus Voltage", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.BUS_VOLTAGE), updateCAN));
        Cluck.publish("CAN Clamp Output Current", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT), updateCAN));
        Cluck.publish("CAN Clamp Output Voltage", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_VOLTAGE), updateCAN));
        Cluck.publish("CAN Clamp Temperature", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.TEMPERATURE), updateCAN));
        Cluck.publish("CAN Clamp Any Fault", QuasarHelios.publishFault("clamp-can", clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.ANY_FAULT)));
        Cluck.publish("CAN Clamp Bus Voltage Fault", BooleanMixing.createDispatch(clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.BUS_VOLTAGE_FAULT), updateCAN));
        Cluck.publish("CAN Clamp Temperature Fault", BooleanMixing.createDispatch(clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.TEMPERATURE_FAULT), updateCAN));

        BooleanInput limitTop = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(2)), Igneous.constantPeriodic);
        BooleanInput limitBottom = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(3)), Igneous.constantPeriodic);

        BooleanStatus atTop = new BooleanStatus(), atBottom = new BooleanStatus();
        atTop.setTrueWhen(EventMixing.filterEvent(FloatMixing.floatIsAtMost(speedControl, -0.01f), true, BooleanMixing.onPress(limitTop)));
        atTop.setFalseWhen(EventMixing.filterEvent(FloatMixing.floatIsAtLeast(speedControl, 0.01f), true, BooleanMixing.onRelease(limitTop)));
        atBottom.setTrueWhen(EventMixing.filterEvent(FloatMixing.floatIsAtLeast(speedControl, 0.01f), true, BooleanMixing.onPress(limitBottom)));
        atBottom.setFalseWhen(EventMixing.filterEvent(FloatMixing.floatIsAtMost(speedControl, -0.01f), true, BooleanMixing.onRelease(limitBottom)));

        FloatStatus distance = ControlInterface.mainTuning.getFloat("clamp-distance", 1.0f);

        FloatMixing.pumpWhen(EventMixing.filterEvent(useEncoder, true, BooleanMixing.onPress(atBottom)), encoder, FloatMixing.negate((FloatOutput) distance));
        EventMixing.filterEvent(useEncoder, true, BooleanMixing.onPress(atTop)).send(zeroEncoder);

        FloatStatus p = ControlInterface.mainTuning.getFloat("clamp-p", 1.0f);
        FloatStatus i = ControlInterface.mainTuning.getFloat("clamp-i", 0.0f);
        FloatStatus d = ControlInterface.mainTuning.getFloat("clamp-d", 0.0f);

        heightReadout = FloatMixing.normalizeFloat(encoder, FloatMixing.negate((FloatInput) distance), FloatMixing.always(0.0f));

        PIDControl pid = new PIDControl(heightReadout, height, p, i, d);

        pid.integralTotal.setWhen(0.0f, BooleanMixing.onRelease(mode));
        FloatMixing.pumpWhen(BooleanMixing.onRelease(mode), heightReadout, height);

        QuasarHelios.constantControl.send(pid);

        FloatOutput out = (value) -> {
            if (atTop.get()) {
                value = Math.max(value, 0);
            }
            if (atBottom.get()) {
                value = Math.min(value, 0);
            }
            speedControl.set(value);
        };

        mode.setTrueWhen(EventMixing.filterEvent(FloatMixing.floatIsOutsideRange(speed, -0.3f, 0.3f), true, QuasarHelios.globalControl));
        mode.setTrueWhen(Igneous.startTele);

        FloatMixing.pumpWhen(QuasarHelios.constantControl, Mixing.select(BooleanMixing.orBooleans(mode,
                useEncoder.asInvertedInput()), pid, speed), FloatMixing.deadzone(out, 0.1f));

        Cluck.publish(QuasarHelios.testPrefix + "Clamp Open Control", openControl);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Height Encoder", FloatMixing.createDispatch(encoder, Igneous.globalPeriodic));
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Height Scaled", FloatMixing.createDispatch(heightReadout, Igneous.globalPeriodic));
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Height Setting", height);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Integral Total", pid.integralTotal);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Top (Direct)", limitTop);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Bottom (Direct)", limitBottom);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Top", atTop);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Bottom", atBottom);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Motor Speed", speedControl);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp PID Output", (FloatInput) pid);

        Cluck.publish("Clamp Distance", distance);
        Cluck.publish("Clamp Min Set", FloatMixing.pumpEvent(encoder, FloatMixing.negate((FloatOutput) distance)));
        Cluck.publish("Clamp Max Set", zeroEncoder);
        Cluck.publish("Clamp Mode", mode);
        Cluck.publish("Clamp Speed", speed);
    }
}
