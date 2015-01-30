package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class Elevator {
    private static final FloatOutput winch = Igneous.makeJaguarMotor(3, Igneous.MOTOR_FORWARD, 0.4f);

    private static final BooleanStatus raising = new BooleanStatus();
    private static final BooleanStatus lowering = new BooleanStatus();
    private static final BooleanStatus goingToMiddle = new BooleanStatus();

    public static EventOutput setTop = EventMixing.combine(BooleanMixing.getSetEvent(raising, true), BooleanMixing.getSetEvent(lowering, false), BooleanMixing.getSetEvent(goingToMiddle, false));
    public static EventOutput setMiddle = BooleanMixing.getSetEvent(goingToMiddle, true);
    public static EventOutput setBottom = EventMixing.combine(BooleanMixing.getSetEvent(raising, false), BooleanMixing.getSetEvent(lowering, true), BooleanMixing.getSetEvent(goingToMiddle, false));
    public static EventOutput stop = BooleanMixing.getSetEvent(BooleanMixing.combine(raising, lowering, goingToMiddle), false);

    public static final BooleanInput topLimitSwitch = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(0)), Igneous.globalPeriodic);
    public static final BooleanInput bottomLimitSwitch = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(1)), Igneous.globalPeriodic);
    public static final BooleanInput middleUpperLimitSwitch = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(2)), Igneous.globalPeriodic);
    public static final BooleanInput middleLowerLimitSwitch = BooleanMixing.alwaysFalse; //BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(3)), Igneous.globalPeriodic);
    private static BooleanStatus lastLimitSide = new BooleanStatus(); // true = top, false = bottom

    private static FloatInput winchSpeed = ControlInterface.mainTuning.getFloat("main-elevator-speed", 1.0f);

    public static void setup() {
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
