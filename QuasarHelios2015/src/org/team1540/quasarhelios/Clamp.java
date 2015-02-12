package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
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
import ccre.igneous.Igneous;
import ccre.log.Logger;

public class Clamp {
    public static FloatStatus heightOrSpeed = new FloatStatus();
    public static BooleanStatus mode = new BooleanStatus(); // true = speed,
                                                            // false = height
    public static final BooleanStatus openControl = new BooleanStatus(Igneous.makeSolenoid(3));

    private static final BooleanStatus useEncoder = ControlInterface.mainTuning.getBoolean("clamp-use-encoder", false);

    public static FloatInputPoll heightReadout;

    public static void setup() {

        FloatInputPoll encoder = Igneous.makeEncoder(10, 11, false);
        ExtendedMotor clampCAN = Igneous.makeCANTalon(1);
        FloatOutput temp = FloatMixing.ignoredFloatOutput;
        try {
            temp = clampCAN.asMode(ExtendedMotor.OutputControlMode.VOLTAGE_FRACTIONAL);
        } catch (ExtendedMotorFailureException e) {
            Logger.severe("Exception thrown when creating clamp motor", e);
        }

        final FloatOutput speedControl = temp;

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

        PIDControl pid = new PIDControl(heightReadout, heightOrSpeed, p, i, d);

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

        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(BooleanMixing.orBooleans(mode, useEncoder.asInvertedInput()), pid, heightOrSpeed), FloatMixing.deadzone(out, 0.1f));

        Cluck.publish(QuasarHelios.testPrefix + "Clamp Open Control", openControl);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Height Encoder", FloatMixing.createDispatch(encoder, Igneous.globalPeriodic));
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Top", limitTop);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Bottom", limitBottom);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Motor Speed", speedControl);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp PID Output", (FloatInput) pid);

        Cluck.publish("Clamp Max Set", FloatMixing.pumpEvent(encoder, max));
        Cluck.publish("Clamp Min Set", FloatMixing.pumpEvent(encoder, min));
    }
}
