package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;

public class Rollers {
    public static final BooleanStatus direction = new BooleanStatus(true);
    public static final BooleanStatus running = new BooleanStatus(false);
    public static final BooleanStatus open = new BooleanStatus(true);

    private static final FloatOutput armRollers = FloatMixing.combine(Igneous.makeTalonMotor(4, Igneous.MOTOR_REVERSE, 0.1f), Igneous.makeTalonMotor(5, Igneous.MOTOR_FORWARD, 0.1f));
    private static final FloatOutput frontRollers = Igneous.makeTalonMotor(6, Igneous.MOTOR_FORWARD, 0.1f);
    private static final FloatOutput internalRollers = Igneous.makeTalonMotor(7, Igneous.MOTOR_REVERSE, 0.1f);

    private static final FloatStatus pneumaticOverride = new FloatStatus();
    private static final FloatStatus spinOverride = new FloatStatus();

    private static final FloatOutput externalRollers = FloatMixing.combine(armRollers, frontRollers);
    private static final FloatOutput allRollers = FloatMixing.combine(externalRollers, internalRollers);
    private static final FloatInput actualSpeed = ControlInterface.mainTuning.getFloat("main-rollers-speed", 1.0f);
    private static final FloatInputPoll motorSpeed = Mixing.quadSelect(running, direction, FloatMixing.always(0.0f), FloatMixing.always(0.0f), FloatMixing.negate(actualSpeed), actualSpeed);

    public static final EventOutput toggleRollersButton = direction.getToggleEvent();
    public static final EventOutput runRollersButton = running.getToggleEvent();
    public static final EventOutput toggleOpenButton = open.getToggleEvent();
    public static final FloatOutput controlArmsIndependently = pneumaticOverride;
    public static final FloatOutput controlRollersManually = spinOverride;

    public static void setup() {
        FloatMixing.pumpWhen(QuasarHelios.globalControl, FloatMixing.addition.of((FloatInputPoll) spinOverride, motorSpeed), allRollers);
        Cluck.publish(QuasarHelios.testPrefix + "Roller Speed Arm", armRollers);
        Cluck.publish(QuasarHelios.testPrefix + "Roller Speed Front", frontRollers);
        Cluck.publish(QuasarHelios.testPrefix + "Roller Speed Internal", internalRollers);
        Cluck.publish(QuasarHelios.testPrefix + "Roller Open", open);

        BooleanMixing.pumpWhen(QuasarHelios.globalControl,
                BooleanMixing.xorBooleans(open, FloatMixing.floatIsAtMost(pneumaticOverride, -0.3f)),
                BooleanMixing.invert(Igneous.makeSolenoid(1)));
        BooleanMixing.pumpWhen(QuasarHelios.globalControl,
                BooleanMixing.xorBooleans(open, FloatMixing.floatIsAtLeast(pneumaticOverride, 0.3f)),
                BooleanMixing.invert(Igneous.makeSolenoid(2)));
    }
}
