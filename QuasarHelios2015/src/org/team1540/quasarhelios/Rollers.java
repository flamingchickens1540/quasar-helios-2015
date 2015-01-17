package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.FloatOutput;
import ccre.ctrl.FloatMixing;
import ccre.igneous.Igneous;

public class Rollers {
	private final static BooleanStatus direction = new BooleanStatus(true);
	private final static BooleanStatus speed = new BooleanStatus(false);
	
	private static final FloatOutput armRollers = FloatMixing.combine(Igneous.makeTalonMotor(5, Igneous.MOTOR_REVERSE, 0.1f), Igneous.makeTalonMotor(6, Igneous.MOTOR_FORWARD, 0.1f)); 
	private static final FloatOutput frontRollers = Igneous.makeTalonMotor(7, Igneous.MOTOR_FORWARD, 0.1f);
	private static final FloatOutput internalRollers = FloatMixing.combine(Igneous.makeTalonMotor(8, Igneous.MOTOR_REVERSE, 0.1f), Igneous.makeTalonMotor(9, Igneous.MOTOR_FORWARD, 0.1f)); 
	
	private static final FloatOutput externalRollers = FloatMixing.combine(armRollers, frontRollers);
	private static final FloatOutput allRollers = FloatMixing.combine(externalRollers, internalRollers);
	
	public static EventInput toggleRollersButton;
	public static EventInput runRollersButton;
		
	public static void setup() {		
		speed.toggleWhen(runRollersButton);
		direction.toggleWhen(toggleRollersButton);
		
		Igneous.globalPeriodic.send(new EventOutput() {
			public void event() {
				if (speed.get()) {
					float newSpeed = direction.get() ? 1.0f : -1.0f;
					allRollers.set(newSpeed);
				} else {
					allRollers.set(0.0f);
				}
			}
		});
	}
}
