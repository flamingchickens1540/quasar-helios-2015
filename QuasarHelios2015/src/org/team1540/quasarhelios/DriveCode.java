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
	public static EventInput octocanumShiftingButton;
	private static FloatOutput leftFrontMotor = Igneous.makeTalonMotor(0, Igneous.MOTOR_REVERSE, .1f);
	private static FloatOutput leftBackMotor = Igneous.makeTalonMotor(1, Igneous.MOTOR_REVERSE, .1f);
	private static FloatOutput rightFrontMotor = Igneous.makeTalonMotor(2, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput rightBackMotor = Igneous.makeTalonMotor(3, Igneous.MOTOR_FORWARD, .1f);
	private static FloatOutput rightMotors = FloatMixing.combine(rightFrontMotor, rightBackMotor);
	private static FloatOutput leftMotors = FloatMixing.combine(leftFrontMotor, leftBackMotor);
	private static BooleanStatus octocanumShifting = new BooleanStatus();

	private static FloatStatus centricAngleOffset;
	private static FloatStatus calibratedAngle = new FloatStatus(0);
	
	private static double π = Math.PI;
	
	private static EventOutput mecanum = new EventOutput() {
		public void event() {
			float speed;
			float rotationspeed;
			double angle;
			
			float distanceY = leftJoystickChannelY.get();
			float distanceX = leftJoystickChannelX.get();
			speed = (float) Math.sqrt(distanceX*distanceX+distanceY*distanceY);
			rotationspeed = rightJoystickChannelX.get();
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
			
			double centric = calibratedAngle.get() - centricAngleOffset.get();
			double currentAngle = HeadingSensor.yaw.get();
			currentAngle = currentAngle / 180 * π;
			currentAngle -= centric;
			angle -= currentAngle;
			
			float leftFront = (float) (speed * Math.sin(angle - π / 4) - rotationspeed);
			float rightFront = (float) (speed * Math.cos(angle - π / 4) + rotationspeed);
			float leftBack = (float) (speed * Math.cos(angle - π / 4) - rotationspeed);
			float rightBack = (float) (speed * Math.sin(angle - π / 4) + rotationspeed);
			float normalize = Math.max(Math.max(Math.abs(leftFront), Math.abs(rightFront)),
					Math.max(Math.abs(leftBack), Math.abs(rightBack)));
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
	
	private static EventOutput calibrate = new EventOutput() {
		public void event() {
			calibratedAngle.set((float) (HeadingSensor.yaw.get() / 180 * π));
			Logger.info("Calibrated Angle: " + HeadingSensor.yaw.get());
		}
	};

	public static void setup() {
		TuningContext context = new TuningContext("DriveTuning").publishSavingEvent();
		centricAngleOffset = context.getFloat("centric_angle", 0);
		Cluck.publish("Centric Angle Offset",centricAngleOffset);
		Cluck.publish("Calibrate Field Centric Angle", calibrate);
		Cluck.publish("Get Current Angle", new EventOutput() {
			public void event() {
				double angle = calibratedAngle.get() / π * 180 - HeadingSensor.yaw.get();
				Logger.info("Current Angle: " + angle);
			}
		});
		Cluck.publish("Zero Gyro", HeadingSensor.zeroGyro);
		
		ExpirationTimer timer = new ExpirationTimer();
		timer.schedule(100, calibrate);
		timer.start();
		octocanumShifting.toggleWhen(octocanumShiftingButton);
		Igneous.duringTele.send(EventMixing.filterEvent(octocanumShifting, false, mecanum));
		Igneous.duringTele.send(EventMixing.filterEvent(octocanumShifting, true,
				DriverImpls.createTankDriverEvent(leftJoystickChannelY, rightJoystickChannelY, leftMotors, rightMotors)));
	}
}
