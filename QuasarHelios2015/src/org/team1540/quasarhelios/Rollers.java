package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
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
    public static final boolean FORWARD = true;
    public static final boolean REVERSE = false;

    public static final BooleanStatus direction = new BooleanStatus(FORWARD);
    public static final BooleanStatus running = new BooleanStatus(false);
    public static final BooleanStatus closed = new BooleanStatus(true);

    // These will need individual tuning for speed.
    private static final FloatOutput rightArmRoller = Igneous.makeTalonMotor(4, Igneous.MOTOR_REVERSE, 0.1f);
    private static final FloatOutput leftArmRoller = Igneous.makeTalonMotor(5, Igneous.MOTOR_FORWARD, 0.1f);
    private static final FloatOutput frontRollers = Igneous.makeTalonMotor(6, Igneous.MOTOR_FORWARD, 0.1f);
    private static final FloatOutput internalRollers = Igneous.makeTalonMotor(7, Igneous.MOTOR_REVERSE, 0.1f);
    
    private static final BooleanOutput leftPneumatic = Igneous.makeSolenoid(1);
    private static final BooleanOutput rightPneumatic = Igneous.makeSolenoid(2);

    public static final BooleanStatus rightPneumaticOverride = new BooleanStatus();
    public static final BooleanStatus leftPneumaticOverride = new BooleanStatus();
    public static final FloatStatus rightRollerOverride = new FloatStatus();
    public static final FloatStatus leftRollerOverride = new FloatStatus();
    public static final BooleanStatus overrideRollers = new BooleanStatus();

    private static final FloatInput actualSpeed = ControlInterface.mainTuning.getFloat("main-rollers-speed", 1.0f);
    private static final FloatInputPoll motorSpeed = Mixing.quadSelect(running, direction, FloatMixing.always(0.0f), FloatMixing.always(0.0f), FloatMixing.negate(actualSpeed), actualSpeed);

    public static final EventOutput toggleRollersButton = direction.getToggleEvent();
    public static final EventOutput runRollersButton = running.getToggleEvent();
    public static final EventOutput toggleOpenButton = closed.getToggleEvent();

    public static void setup() {
        FloatMixing.pumpWhen(QuasarHelios.globalControl, motorSpeed, FloatMixing.combine(frontRollers, internalRollers));
        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(overrideRollers, motorSpeed, FloatMixing.negate((FloatInput) leftRollerOverride)), leftArmRoller);
        FloatMixing.pumpWhen(QuasarHelios.globalControl, Mixing.select(overrideRollers, motorSpeed, FloatMixing.negate((FloatInput) rightRollerOverride)), rightArmRoller);
        
        BooleanInput normalPneumatics = BooleanMixing.andBooleans(BooleanMixing.invert((BooleanInput) overrideRollers), closed);
        
        BooleanMixing.pumpWhen(QuasarHelios.globalControl, BooleanMixing.orBooleans(normalPneumatics, BooleanMixing.invert(leftPneumaticOverride.asInput())), leftPneumatic);
        BooleanMixing.pumpWhen(QuasarHelios.globalControl, BooleanMixing.orBooleans(normalPneumatics, BooleanMixing.invert(rightPneumaticOverride.asInput())), rightPneumatic);
        
        Cluck.publish(QuasarHelios.testPrefix + "Roller Speed Left Arm", leftArmRoller);
        Cluck.publish(QuasarHelios.testPrefix + "Roller Speed Right Arm", rightArmRoller);
        Cluck.publish(QuasarHelios.testPrefix + "Roller Speed Front", frontRollers);
        Cluck.publish(QuasarHelios.testPrefix + "Roller Speed Internal", internalRollers);
        Cluck.publish(QuasarHelios.testPrefix + "Roller Closed", closed);
        Cluck.publish(QuasarHelios.testPrefix + "Roller Open Left", leftPneumatic);
        Cluck.publish(QuasarHelios.testPrefix + "Roller Open Right", rightPneumatic);
    }
}
