package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;

public class Elevator {
	private static final FloatOutput winch = Igneous.makeJaguarMotor(5, Igneous.MOTOR_FORWARD, 0.4f);
	
	private static final FloatStatus elevatorSpeed = new FloatStatus(winch);
	
	private static final BooleanInput topLimitSwitch = BooleanMixing.createDispatch(Igneous.makeDigitalInput(0), Igneous.globalPeriodic);
	private static final BooleanInput bottomLimitSwitch = BooleanMixing.createDispatch(Igneous.makeDigitalInput(1), Igneous.globalPeriodic);
	
	public static final BooleanStatus elevatorControl = new BooleanStatus(Mixing.select(elevatorSpeed, -1.0f, 1.0f));
	
	public static EventInput raisingInput;
	public static EventInput loweringInput;
	
	public static void setup() {		
		elevatorSpeed.setWhen(0.0f, BooleanMixing.onPress(topLimitSwitch));
		elevatorSpeed.setWhen(0.0f, BooleanMixing.onPress(bottomLimitSwitch));
		
		elevatorControl.setTrueWhen(raisingInput);
		elevatorControl.setFalseWhen(loweringInput);
		
		// Needed because elevatorControl initialization causes elevatorSpeed to be set to -1.0.
		elevatorSpeed.set(0.0f);
	}
}
