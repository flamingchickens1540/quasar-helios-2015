package org.team1540.quasarhelios;

import ccre.channel.*;
import ccre.ctrl.*;
import ccre.igneous.*;

public class DriveCode {
	public static FloatInput leftJoystickChannelX;
	public static FloatInput leftJoystickChannelY;
	public static FloatInput rightJoystickChannelX;
	public static FloatInput rightJoystickChannelY;
	public static BooleanInput octocanumShifting;
	private static FloatOutput rightFrontMotor = Igneous.makeTalonMotor(1, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput leftFrontMotor = Igneous.makeTalonMotor(2, Igneous.MOTOR_REVERSE, .1f);
	private static FloatOutput rightBackMotor = Igneous.makeTalonMotor(3, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput leftBackMotor = Igneous.makeTalonMotor(4, Igneous.MOTOR_REVERSE, .1f);
	private static FloatOutput rightMotors = FloatMixing.combine(rightFrontMotor, rightBackMotor);
	private static FloatOutput leftMotors = FloatMixing.combine(leftFrontMotor, leftBackMotor);

	private static double π = Math.PI;

	public static void setup() {
		Igneous.duringTele.send(new EventOutput() {
			public void event() {
				float distanceY = rightJoystickChannelY.get();
				float distanceX = rightJoystickChannelX.get();
				float speed = leftJoystickChannelY.get();
				float rotationspeed = leftJoystickChannelX.get();
				double angle;
				if (distanceX == 0) {
					if (distanceY >= 0) {
						angle = π / 2;
					} else {
						angle = 3 * π / 2;
					}
				} else {
					angle = Math.atan(distanceY / distanceX);
					if (distanceX < 0) {
						angle += π;
					}
					if (angle < 0) {
						angle += 2 * π;
					}
				}
				float leftFront = (float) (speed * Math.sin(angle + π / 4) + rotationspeed);
				float rightFront = (float) (speed * Math.cos(angle + π / 4) - rotationspeed);
				float leftBack = (float) (speed * Math.cos(angle + π / 4) + rotationspeed);
				float rightBack = (float) (speed * Math.sin(angle + π / 4) - rotationspeed);
				float normalize = Math.max(
						Math.max(Math.abs(leftFront), Math.abs(rightFront)),
						Math.max(Math.abs(leftBack), Math.abs(rightBack)));
				if (normalize > 1) {
					leftFront /= normalize;
					rightFront /= normalize;
					leftBack /= normalize;
					rightBack /= normalize;
				}
				if (normalize < Math.abs(speed)){
					float multiplier = Math.abs(speed)/normalize;
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
		});
	}

}
