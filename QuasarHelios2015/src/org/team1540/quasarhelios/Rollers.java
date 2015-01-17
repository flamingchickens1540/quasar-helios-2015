package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventOutput;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
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
	
	public static BooleanInput toggleRollersButton;
	public static BooleanInput runRollersButton;
	
	public static BooleanOutput runRollers = new BooleanOutput() {
		public void set(boolean value) {
			if (value) {
				if (!speed.get()) {
					speed.set(true);
				} else {
					speed.set(false);
				}
			}
		}
	};
	
	public static void setup() {		
		runRollersButton.send(runRollers);
		
		toggleRollersButton.send(new BooleanOutput() {
			public void set(boolean value) {
				if (value) {
					direction.set(!direction.get());
				}
			}
		});
		
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
