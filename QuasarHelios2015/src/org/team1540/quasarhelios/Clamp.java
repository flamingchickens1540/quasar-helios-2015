package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.PIDControl;
import ccre.igneous.Igneous;

public class Clamp {
    public static FloatInput heightInput;
    public static EventInput openInput;
    public static final BooleanStatus openControl = new BooleanStatus(Igneous.makeSolenoid(3));
    public static final FloatStatus heightControl = new FloatStatus();

    public static FloatInputPoll heightReadout;

    public static void setup() {

        FloatInputPoll encoder = Igneous.makeEncoder(10, 11, false);
        FloatOutput speedControl = Igneous.makeTalonMotor(11, Igneous.MOTOR_REVERSE, 0.1f);

        BooleanInput limitTop = BooleanMixing.createDispatch(Igneous.makeDigitalInput(2), Igneous.globalPeriodic);
        BooleanInput limitBottom = BooleanMixing.createDispatch(Igneous.makeDigitalInput(3), Igneous.globalPeriodic);

        FloatStatus min = ControlInterface.mainTuning.getFloat("clamp-min", 0.0f);
        FloatStatus max = ControlInterface.mainTuning.getFloat("clamp-max", 1.0f);

        FloatMixing.pumpWhen(BooleanMixing.onPress(limitBottom), encoder, min);
        FloatMixing.pumpWhen(BooleanMixing.onPress(limitTop), encoder, max);

        FloatStatus p = ControlInterface.mainTuning.getFloat("clamp-p", 1.0f);
        FloatStatus i = ControlInterface.mainTuning.getFloat("clamp-i", 0.0f);
        FloatStatus d = ControlInterface.mainTuning.getFloat("clamp-d", 0.0f);

        heightReadout = FloatMixing.normalizeFloat(encoder, min, max);

        PIDControl pid = new PIDControl(heightReadout, heightControl, p, i, d);

        QuasarHelios.globalControl.send(pid);

        FloatOutput out = new FloatOutput() {
            @Override
            public void set(float value) {
                if (limitTop.get()) {
                    value = Math.max(value, 0);
                }

                if (limitBottom.get()) {
                    value = Math.min(value, 0);
                }
                speedControl.set(value);
            }
        };

        FloatMixing.pumpWhen(QuasarHelios.globalControl, pid, out);

        heightInput.send(heightControl);
        openControl.toggleWhen(openInput);

        Cluck.publish(QuasarHelios.testPrefix + "Clamp Open Control", openControl);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Height Encoder", FloatMixing.createDispatch(encoder, Igneous.globalPeriodic));
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Top", limitTop);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Bottom", limitBottom);
        Cluck.publish(QuasarHelios.testPrefix + "Clamp Motor Speed", speedControl);

        Cluck.publish("Clamp Max Set", FloatMixing.pumpEvent(encoder, max));
        Cluck.publish("Clamp Min Set", FloatMixing.pumpEvent(encoder, min));
    }
}
