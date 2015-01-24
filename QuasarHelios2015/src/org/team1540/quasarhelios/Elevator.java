package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.FloatOutput;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;

public class Elevator {
	private static final FloatOutput winch = Igneous.makeJaguarMotor(10,
			Igneous.MOTOR_FORWARD, 0.4f);

	public static final BooleanStatus raising = new BooleanStatus();
	public static final BooleanStatus lowering = new BooleanStatus();

	public static final BooleanInputPoll topLimitSwitch = Igneous
			.makeDigitalInput(0);
	public static final BooleanInputPoll bottomLimitSwitch = Igneous
			.makeDigitalInput(1);

	public static EventInput raisingInput;
	public static EventInput loweringInput;

	public static void setup() {
		raising.setFalseWhen(EventMixing.filterEvent(topLimitSwitch, true,
				Igneous.globalPeriodic));
		lowering.setFalseWhen(EventMixing.filterEvent(bottomLimitSwitch, true,
				Igneous.globalPeriodic));

		raising.toggleWhen(raisingInput);
		raising.setFalseWhen(loweringInput);
		lowering.toggleWhen(loweringInput);
		lowering.setFalseWhen(raisingInput);

		FloatMixing.pumpWhen(Igneous.globalPeriodic,
				Mixing.quadSelect(raising, lowering, 0.0f, -1.0f, 1.0f, 0.0f),
				winch);
	}
}
