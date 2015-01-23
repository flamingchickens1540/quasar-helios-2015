package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.ctrl.FloatMixing;
import ccre.holders.TuningContext;

public class ControlInterface {
	public static TuningContext mainTuning = new TuningContext("Main").publishSavingEvent();
	public static TuningContext autoTuning = new TuningContext("Autonomous").publishSavingEvent();
	public static TuningContext teleTuning = new TuningContext("Teleoperated").publishSavingEvent();
	
	public static void setup() {
		setupJoysticks();
		setupCluck();
	}
	
	public static void setupJoysticks() {
		FloatMixing.deadzone(Igneous.joystick1.getXAxisSource(), .2f).send(DriveCode.leftJoystickX);
		FloatMixing.deadzone(Igneous.joystick1.getYAxisSource(), .2f).send(DriveCode.leftJoystickY);
		FloatMixing.deadzone(Igneous.joystick1.getAxisSource(5), .2f).send(DriveCode.rightJoystickX);
		FloatMixing.deadzone(Igneous.joystick1.getAxisSource(6), .2f).send(DriveCode.rightJoystickY);	
		DriveCode.octocanumShiftingButton = Igneous.joystick1.getButtonSource(1);
		
		Elevator.raisingInput = Igneous.joystick2.getButtonSource(1);
		Elevator.loweringInput = Igneous.joystick2.getButtonSource(2);
		Rollers.runRollersButton = Igneous.joystick2.getButtonSource(3);
		Rollers.toggleRollersButton = Igneous.joystick2.getButtonSource(4);
		Rollers.toggleOpenButton = Igneous.joystick2.getButtonSource(5);
	}
		
	public static void setupCluck() {
		
	}
}
