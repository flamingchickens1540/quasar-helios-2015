package org.team1540.quasarhelios;

import ccre.channel.*;
import ccre.cluck.Cluck;
import ccre.ctrl.*;
import ccre.holders.TuningContext;
import ccre.igneous.*;
import ccre.log.Logger;

public class DriveCode {
	public static FloatInput leftJoystickChannelX;
	public static FloatInput leftJoystickChannelY;
	public static FloatInput rightJoystickChannelX;
	public static FloatInput rightJoystickChannelY;
	public static BooleanInput octocanumShifting;
	private static FloatOutput leftFrontMotor = Igneous.makeTalonMotor(0, Igneous.MOTOR_REVERSE, .1f);
	private static FloatOutput leftBackMotor = Igneous.makeTalonMotor(1, Igneous.MOTOR_REVERSE, .1f);
	private static FloatOutput rightFrontMotor = Igneous.makeTalonMotor(2, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput rightBackMotor = Igneous.makeTalonMotor(3, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput rightMotors = FloatMixing.combine(rightFrontMotor, rightBackMotor);
	private static FloatOutput leftMotors = FloatMixing.combine(leftFrontMotor, leftBackMotor);
	
	private static double π = Math.PI;
	
	private static EventOutput mecanum = new EventOutput() {
		public void event() {
			float distanceY = rightJoystickChannelY.get();
			float distanceX = rightJoystickChannelX.get();
			float speed = leftJoystickChannelY.get();
			float rotationspeed = leftJoystickChannelX.get();
			double angle;
			if (distanceX == 0) {
				if (distanceY > 0) {
					angle = 3 * π / 2;
				} else {
					angle = π / 2;
				}
			} else {
				angle = Math.atan(distanceY / distanceX);
				if (distanceX >= 0) {
					angle += π;
				}
			}
			float leftFront = (float) (speed * Math.sin(angle - π / 4) + rotationspeed);
			float rightFront = (float) (speed * Math.cos(angle - π / 4) - rotationspeed);
			float leftBack = (float) (speed * Math.cos(angle - π / 4) + rotationspeed);
			float rightBack = (float) (speed * Math.sin(angle - π / 4) - rotationspeed);
			float normalize = Math.max(Math.max(Math.abs(leftFront), Math.abs(rightFront)), Math.max(Math.abs(leftBack), Math.abs(rightBack)));
			if (normalize > 1) {
				leftFront /= normalize;
				rightFront /= normalize;
				leftBack /= normalize;
				rightBack /= normalize;
			}
			if (normalize < Math.abs(speed)) {
				float multiplier = Math.abs(speed) / normalize;
				leftFront *= multiplier;
				rightFront *= multiplier;
				leftBack *= multiplier;
				rightBack *= multiplier;
			}
			rightFrontMotor.set(rightFront);
			leftFrontMotor.set(leftFront);
			rightBackMotor.set(rightBack);
			leftBackMotor.set(leftBack);
		}
	};
	
	private static EventOutput tankDrive = new EventOutput() {
		public void event(){
			leftMotors.set(leftJoystickChannelY.get());
			rightMotors.set(rightJoystickChannelY.get());
		}
	};

	public static void setup() {
		Igneous.duringTele.send(EventMixing.filterEvent(octocanumShifting, false, mecanum));
		Igneous.duringTele.send(EventMixing.filterEvent(octocanumShifting, true, tankDrive));
	}
}
