package org.team1540.quasarhelios;

import ccre.channel.BooleanOutput;
import ccre.channel.BooleanStatus;
import ccre.channel.FloatOutput;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;

public class Elevator {
	private static final FloatOutput winch = Igneous.makeJaguarMotor(5, Igneous.MOTOR_FORWARD, 0.4f);
	
	private static final BooleanStatus raisingStatus = new BooleanStatus(Mixing.select(winch, 0.0f, 1.0f));
	private static final BooleanStatus loweringStatus = new BooleanStatus(Mixing.select(winch, 0.0f, -1.0f));
	
	public static final BooleanOutput elevatorControl = new BooleanOutput() {
		public void set(boolean value) {
			raisingStatus.set(value);
			loweringStatus.set(!value);
		}
	};
}
