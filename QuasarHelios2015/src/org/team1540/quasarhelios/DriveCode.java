package org.team1540.quasarhelios;

import ccre.channel.*;
import ccre.ctrl.*;
import ccre.igneous.*;

public class DriveCode {
	public static FloatInput leftJoystickChannelX;
	public static FloatInput leftJoystickChannelY;
	public static FloatInput rightJoystickChannelX;
	public static FloatInput rightJoystickChannelY;
	public static EventInput octocanumShiftingButton;
	private static FloatOutput leftFrontMotor = Igneous.makeTalonMotor(0, Igneous.MOTOR_REVERSE, .1f);
	private static FloatOutput leftBackMotor = Igneous.makeTalonMotor(1, Igneous.MOTOR_REVERSE, .1f);
	private static FloatOutput rightFrontMotor = Igneous.makeTalonMotor(2, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput rightBackMotor = Igneous.makeTalonMotor(3, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput rightMotors = FloatMixing.combine(rightFrontMotor, rightBackMotor);
	private static FloatOutput leftMotors = FloatMixing.combine(leftFrontMotor, leftBackMotor);
	public static FloatOutput allMotors = FloatMixing.combine(leftMotors, rightMotors);
	public static FloatOutput rotate = new FloatOutput() {
		public void set(float value) {
			leftMotors.set(value);
			rightMotors.set(-value);
		}
	};
	public static BooleanStatus octocanumShifting = new BooleanStatus();
	public static FloatInput leftEncoderRaw = FloatMixing.createDispatch(Igneous.makeEncoder(6,7, Igneous.MOTOR_REVERSE), Igneous.globalPeriodic);
	public static FloatInput rightEncoderRaw = FloatMixing.createDispatch(Igneous.makeEncoder(8,9, Igneous.MOTOR_FORWARD), Igneous.globalPeriodic);
	public static FloatInput encoderScaling = ControlInterface.mainTuning.getFloat("main-encoder-scaling", 0.1f);
	public static FloatInput leftEncoder = FloatMixing.multiplication.of(leftEncoderRaw, encoderScaling);
	public static FloatInput rightEncoder = FloatMixing.multiplication.of(rightEncoderRaw, encoderScaling);

	private static double π = Math.PI;
	
	private static EventOutput mecanum = new EventOutput() {
		public void event() {
			float distanceY = leftJoystickChannelY.get();
			float distanceX = leftJoystickChannelX.get();
			float speed = (float) Math.sqrt(distanceX*distanceX+distanceY*distanceY);
			float rotationspeed = rightJoystickChannelX.get();
			double angle;
			if (distanceX == 0) {
				if (distanceY > 0) {
					angle = π / 2;
				} else {
					angle = 3 * π / 2;
				}
			} else {
				angle = Math.atan(distanceY / distanceX);
				if (distanceX < 0) {
					angle += π;
				}
			}
			float leftFront = (float) (speed * Math.sin(angle - π / 4) - rotationspeed);
			float rightFront = (float) (speed * Math.cos(angle - π / 4) + rotationspeed);
			float leftBack = (float) (speed * Math.cos(angle - π / 4) - rotationspeed);
			float rightBack = (float) (speed * Math.sin(angle - π / 4) + rotationspeed);
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

	public static void setup() {
		octocanumShifting.toggleWhen(octocanumShiftingButton);
		Igneous.duringTele.send(EventMixing.filterEvent(octocanumShifting, false, mecanum));
		Igneous.duringTele.send(EventMixing.filterEvent(octocanumShifting, true,
		DriverImpls.createTankDriverEvent(leftJoystickChannelY, rightJoystickChannelY, leftMotors, rightMotors)));

	}
}
