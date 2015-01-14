package org.team1540.quasarhelios;

import ccre.channel.*;
import ccre.ctrl.FloatMixing;
import ccre.igneous.*;

public class DriveCode {
	public static FloatInput leftJoystickChannel;
	public static FloatInput rightJoystickChannel;
	public static BooleanInput octocanumShifting;
	private static FloatOutput rightFrontMotor = Igneous.makeTalonMotor(1, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput leftFrontMotor = Igneous.makeTalonMotor(2, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput rightBackMotor = Igneous.makeTalonMotor(3, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput leftBackMotor = Igneous.makeTalonMotor(4, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput rightMotors = FloatMixing.combine(rightFrontMotor, rightBackMotor);
	private static FloatOutput leftMotors = FloatMixing.combine(leftFrontMotor, leftBackMotor);
	
	public static void setup() {
		
	}
}
