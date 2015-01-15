package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
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
		DriveCode.leftJoystickChannelX = FloatMixing.deadzone(Igneous.joystick1.getXAxisSource(), .1f);
		DriveCode.leftJoystickChannelY = FloatMixing.deadzone(Igneous.joystick1.getYAxisSource(), .1f);
		DriveCode.rightJoystickChannelX = FloatMixing.deadzone(Igneous.joystick1.getAxisSource(5), .1f);
		DriveCode.rightJoystickChannelY = FloatMixing.deadzone(Igneous.joystick1.getAxisSource(6), .1f);	
		DriveCode.octocanumShifting = BooleanMixing.createDispatch(Igneous.joystick1.getButtonChannel(1), Igneous.globalPeriodic);
	}
	
	public static void setupPhidget() {
	
	}
	
	public static void setupCluck() {
		
	}
}
