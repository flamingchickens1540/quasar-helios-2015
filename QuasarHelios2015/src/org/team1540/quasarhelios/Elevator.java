package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.ctrl.BooleanMixing;
import ccre.igneous.Igneous;

public class Elevator {
	private static final FloatOutput winch = Igneous.makeJaguarMotor(5, Igneous.MOTOR_FORWARD, 0.4f);
	
	private static final FloatStatus elevatorSpeed = new FloatStatus(winch);
	
	private static final BooleanInput topLimitSwitch = BooleanMixing.createDispatch(Igneous.makeDigitalInput(0), Igneous.globalPeriodic);
	private static final BooleanInput bottomLimitSwitch = BooleanMixing.createDispatch(Igneous.makeDigitalInput(1), Igneous.globalPeriodic);
	
	public static final BooleanOutput elevatorControl = new BooleanOutput() {
		public void set(boolean value) {
			if (value) {
				elevatorSpeed.set(1.0f);
			} else {
				elevatorSpeed.set(-1.0f);
			}
		}
	};
	
	public static BooleanInput raisingInput;
	public static BooleanInput loweringInput;
	
	public static void setup() {
		topLimitSwitch.send(new BooleanOutput() {
			public void set(boolean value) {
				if (value && elevatorSpeed.get() > 0) {
					elevatorSpeed.set(0.0f);
				}
			}
		});
		
		bottomLimitSwitch.send(new BooleanOutput() {
			public void set(boolean value) {
				if (value && elevatorSpeed.get() < 0) {
					elevatorSpeed.set(0.0f);
				}
			}
		});
		
		raisingInput.send(new BooleanOutput() {
			public void set(boolean value) {
				if (value) {
					elevatorControl.set(value);
				}
			}
		});
		
		loweringInput.send(new BooleanOutput() {
			public void set(boolean value) {
				if (value) {
					elevatorControl.set(!value);
				}
			}
		});
	}
}
