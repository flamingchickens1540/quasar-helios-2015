package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.ctrl.EventMixing;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;

public class Elevator {
	private static final FloatOutput winch = Igneous.makeJaguarMotor(10, Igneous.MOTOR_FORWARD, 0.4f);
	
	private static final FloatStatus elevatorSpeed = new FloatStatus(winch);
	
	public static final BooleanInputPoll topLimitSwitch = Igneous.makeDigitalInput(0);
	public static final BooleanInputPoll bottomLimitSwitch = Igneous.makeDigitalInput(1);
	
	public static final BooleanStatus elevatorControl = new BooleanStatus(Mixing.select(elevatorSpeed, -1.0f, 1.0f));
	
	public static EventInput raisingInput;
	public static EventInput loweringInput;
	
	public static void setup() {		
		elevatorSpeed.setWhen(0.0f, EventMixing.filterEvent(topLimitSwitch, true, Igneous.globalPeriodic));
		elevatorSpeed.setWhen(0.0f, EventMixing.filterEvent(bottomLimitSwitch, true, Igneous.globalPeriodic));
		
		elevatorControl.setTrueWhen(raisingInput);
		elevatorControl.setFalseWhen(loweringInput);
		
		// Needed because elevatorControl initialization causes elevatorSpeed to be set to -1.0.
		elevatorSpeed.set(0.0f);
	}
}
