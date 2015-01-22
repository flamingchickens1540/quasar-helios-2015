package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;

public class Rollers {
	public static final BooleanStatus direction = new BooleanStatus(true);
	public static final BooleanStatus running = new BooleanStatus(false);
	
	private static final FloatOutput armRollers = FloatMixing.combine(Igneous.makeTalonMotor(5, Igneous.MOTOR_REVERSE, 0.1f), Igneous.makeTalonMotor(6, Igneous.MOTOR_FORWARD, 0.1f)); 
	private static final FloatOutput frontRollers = Igneous.makeTalonMotor(7, Igneous.MOTOR_FORWARD, 0.1f);
	private static final FloatOutput internalRollers = FloatMixing.combine(Igneous.makeTalonMotor(8, Igneous.MOTOR_REVERSE, 0.1f), Igneous.makeTalonMotor(9, Igneous.MOTOR_FORWARD, 0.1f)); 
	
	private static final FloatOutput externalRollers = FloatMixing.combine(armRollers, frontRollers);
	private static final FloatOutput allRollers = FloatMixing.combine(externalRollers, internalRollers);
	
	private static final FloatInput motorSpeed = FloatMixing.createDispatch(Mixing.quadSelect(running, direction, 0.0f, 0.0f, -1.0f, 1.0f), Igneous.globalPeriodic);

	public static EventInput toggleRollersButton;
	public static EventInput runRollersButton;
		
	public static void setup() {		
		running.toggleWhen(runRollersButton);
		direction.toggleWhen(toggleRollersButton);
		motorSpeed.send(allRollers);
	}
}
