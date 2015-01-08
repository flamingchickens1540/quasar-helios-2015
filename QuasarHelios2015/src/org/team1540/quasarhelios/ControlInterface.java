package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.holders.TuningContext;

public class ControlInterface {
	public static TuningContext autoTuning = new TuningContext("Autonomous").publishSavingEvent();
	public static TuningContext teleTuning = new TuningContext("Teleoperated").publishSavingEvent();
	
	public static void setup() {
		setupJoysticks();
		setupCluck();
	}
	
	public static void setupJoysticks() {
		DriveCode.leftJoystickChannel = Igneous.joystick1.getYAxisSource();
		DriveCode.rightJoystickChannel = Igneous.joystick2.getYAxisSource();		
	}
	public static void setupPhidget() {
	
	}
	public static void setupCluck() {
		
	}
}