package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.ctrl.FloatMixing;
import ccre.holders.TuningContext;

public class ControlInterface {
	public static TuningContext autoTuning = new TuningContext("Autonomous").publishSavingEvent();
	public static TuningContext teleTuning = new TuningContext("Teleoperated").publishSavingEvent();
	
	public static void setup() {
		setupJoysticks();
		setupCluck();
	}
	
	public static void setupJoysticks() {
		DriveCode.leftJoystickChannelX = FloatMixing.deadzone(Igneous.joystick1.getXAxisSource(), .2f);
		DriveCode.leftJoystickChannelY = FloatMixing.deadzone(Igneous.joystick1.getYAxisSource(), .2f);
		DriveCode.rightJoystickChannelX = FloatMixing.deadzone(Igneous.joystick1.getAxisSource(5), .2f);
		DriveCode.rightJoystickChannelY = FloatMixing.deadzone(Igneous.joystick1.getAxisSource(6), .2f);	
		DriveCode.octocanumShiftingButton = Igneous.joystick1.getButtonSource(1);
		
		Elevator.raisingInput = Igneous.joystick1.getButtonSource(2);
		Elevator.loweringInput = Igneous.joystick1.getButtonSource(3);
		Rollers.runRollersButton = Igneous.joystick2.getButtonSource(3);
		Rollers.toggleRollersButton =Igneous.joystick2.getButtonSource(4);
	}
		
	public static void setupCluck() {
		
	}
}
