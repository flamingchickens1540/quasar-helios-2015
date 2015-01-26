package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;

public class Rollers {
	public static final BooleanStatus direction = new BooleanStatus(true);
	public static final BooleanStatus running = new BooleanStatus(false);
	public static final BooleanStatus open = new BooleanStatus(BooleanMixing.combine(Igneous.makeSolenoid(0), Igneous.makeSolenoid(1)));
	
	private static final FloatOutput armRollers = FloatMixing.combine(Igneous.makeTalonMotor(4, Igneous.MOTOR_REVERSE, 0.1f), Igneous.makeTalonMotor(5, Igneous.MOTOR_FORWARD, 0.1f)); 
	private static final FloatOutput frontRollers = Igneous.makeTalonMotor(6, Igneous.MOTOR_FORWARD, 0.1f);
	private static final FloatOutput internalRollers = Igneous.makeTalonMotor(7, Igneous.MOTOR_REVERSE, 0.1f); 
	
	private static final FloatOutput externalRollers = FloatMixing.combine(armRollers, frontRollers);
	private static final FloatOutput allRollers = FloatMixing.combine(externalRollers, internalRollers);
	private static final FloatInputPoll actualSpeed = ControlInterface.mainTuning.getFloat("main-rollers-speed", 1.0f);
	private static final FloatInput motorSpeed = FloatMixing.createDispatch(Mixing.select(running, Mixing.select(direction, FloatMixing.createDispatch(actualSpeed, Igneous.globalPeriodic), FloatMixing.createDispatch(FloatMixing.negate(actualSpeed), Igneous.globalPeriodic)), Mixing.select(direction, FloatMixing.always(0.0f), FloatMixing.always(0.0f))), Igneous.globalPeriodic);

	public static EventInput toggleRollersButton;
	public static EventInput runRollersButton;
	public static EventInput toggleOpenButton;
		
	public static void setup() {		
		running.toggleWhen(runRollersButton);
		direction.toggleWhen(toggleRollersButton);
		open.toggleWhen(toggleOpenButton);
		motorSpeed.send(allRollers);
		
		Cluck.publish(QuasarHelios.testPrefix + "Arm Rollers Speed", armRollers);
		Cluck.publish(QuasarHelios.testPrefix + "Front Rollers Speed", frontRollers);
		Cluck.publish(QuasarHelios.testPrefix + "Internal Rollers Speed", internalRollers);
		Cluck.publish(QuasarHelios.testPrefix + "Rollers Open", open);
	}
}
