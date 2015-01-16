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
	public final static BooleanStatus direction = new BooleanStatus(true);
	public final static FloatStatus speed = new FloatStatus(0.0f);
	
	public static FloatOutput armRollers = FloatMixing.combine(Igneous.makeTalonMotor(5, Igneous.MOTOR_REVERSE, 0.1f), Igneous.makeTalonMotor(6, Igneous.MOTOR_FORWARD, 0.1f)); 
	public static FloatOutput frontRollers = Igneous.makeTalonMotor(7, Igneous.MOTOR_FORWARD, 0.1f);
	public static FloatOutput internalRollers = FloatMixing.combine(Igneous.makeTalonMotor(8, Igneous.MOTOR_REVERSE, 0.1f), Igneous.makeTalonMotor(9, Igneous.MOTOR_FORWARD, 0.1f)); 
	
	public static FloatOutput externalRollers = FloatMixing.combine(armRollers, frontRollers);
	public static FloatOutput allRollers = FloatMixing.combine(externalRollers, internalRollers);
	
	public static BooleanInput toggleRollersButton;
	public static BooleanInput runRollersButton;
	
	public static BooleanOutput runRollers = new BooleanOutput() {
		public void set(boolean value) {
			if (value) {
				if (speed.get() == 0.0f) {
					speed.set(1.0f);
				} else {
					speed.set(0.0f);
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
				if (speed.get() != 0.0f) {
					float newSpeed = direction.get() ? 1.0f : -1.0f;
					allRollers.set(newSpeed);
				} else {
					allRollers.set(0.0f);
				}
			}
		});
	}
}
