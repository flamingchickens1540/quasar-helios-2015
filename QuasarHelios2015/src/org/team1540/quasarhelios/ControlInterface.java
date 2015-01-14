package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.phidget.PhidgetReader;
import ccre.ctrl.BooleanMixing;
import ccre.holders.TuningContext;

public class ControlInterface {
	public static TuningContext autoTuning = new TuningContext("Autonomous").publishSavingEvent();
	public static TuningContext teleTuning = new TuningContext("Teleoperated").publishSavingEvent();
	
	public static void setup() {
		setupJoysticks();
		setupPhidget();
		setupCluck();
	}
	
	public static void setupJoysticks() {
		DriveCode.leftJoystickChannel = Igneous.joystick1.getYAxisSource();
		DriveCode.rightJoystickChannel = Igneous.joystick1.getAxisSource(6);	
		DriveCode.octocanumShifting = BooleanMixing.createDispatch(Igneous.joystick1.getButtonChannel(1), Igneous.globalPeriodic);
	}
		
	public static void setupCluck() {
		
	}
}
