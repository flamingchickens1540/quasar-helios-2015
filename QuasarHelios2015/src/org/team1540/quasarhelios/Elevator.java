package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;

public class Elevator {
    private static final FloatOutput winch = Igneous.makeJaguarMotor(10, Igneous.MOTOR_FORWARD, 0.4f);

    public static final BooleanStatus raising = new BooleanStatus();
    public static final BooleanStatus lowering = new BooleanStatus();

    public static final BooleanInput topLimitSwitch = BooleanMixing.createDispatch(Igneous.makeDigitalInput(0), Igneous.globalPeriodic);
    public static final BooleanInput bottomLimitSwitch = BooleanMixing.createDispatch(Igneous.makeDigitalInput(1), Igneous.globalPeriodic);

    public static EventInput raisingInput;
    public static EventInput loweringInput;

    private static FloatInput winchSpeed = ControlInterface.mainTuning.getFloat("main-elevator-speed", 1.0f);

    public static void setup() {
        raising.setFalseWhen(EventMixing.filterEvent(topLimitSwitch, true, Igneous.globalPeriodic));
        lowering.setFalseWhen(EventMixing.filterEvent(bottomLimitSwitch, true, Igneous.globalPeriodic));

        raising.toggleWhen(raisingInput);
        raising.setFalseWhen(loweringInput);
        lowering.toggleWhen(loweringInput);
        lowering.setFalseWhen(raisingInput);

        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(raising, Mixing.select(lowering, FloatMixing.always(0.0f), winchSpeed), Mixing.select(lowering, FloatMixing.always(0.0f), FloatMixing.negate(winchSpeed))), winch);

        Cluck.publish(QuasarHelios.testPrefix + "Elevator Motor Speed", winch);
        Cluck.publish(QuasarHelios.testPrefix + "Elevator Limit Top", topLimitSwitch);
        Cluck.publish(QuasarHelios.testPrefix + "Elevator Limit Bottom", bottomLimitSwitch);
    }
}
