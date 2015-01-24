package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.ctrl.FloatMixing;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;

public abstract class AutonomousModeBase extends InstinctModeModule {
	public static TuningContext context = ControlInterface.autoTuning;
	public static FloatInputPoll driveSpeed = context.getFloat("auto-main-driveSpeed", 1.0f);
	public static FloatInputPoll rotateSpeed = context.getFloat("auto-main-rotateSpeed", 1.0f);
	public static FloatInputPoll clampHeightPadding = context.getFloat("auto-main-clampHeightPadding", 0.01f);

	
	public AutonomousModeBase(String modeName) {
		super(modeName);
	}

	protected void drive(float distance) throws AutonomousModeOverException,
			InterruptedException {
		float startingEncoder = DriveCode.leftEncoder.get();

		if (distance > 0) {
			DriveCode.allMotors.set(driveSpeed.get());
			waitUntilAtLeast(DriveCode.leftEncoder, startingEncoder + distance);
		} else {
			DriveCode.allMotors.set(-driveSpeed.get());
			waitUntilAtMost(DriveCode.leftEncoder, startingEncoder + distance);
		}

		DriveCode.allMotors.set(0.0f);
	}
	
	protected void strafe(float direction, float time) throws InterruptedException, AutonomousModeOverException {
		DriveCode.leftJoystickX.set(direction);
		DriveCode.rightJoystickX.set(direction);
		waitForTime((long) time);
		DriveCode.leftJoystickX.set(0.0f);
		DriveCode.rightJoystickX.set(0.0f);
	}

	protected void turn(float degree) throws AutonomousModeOverException,
			InterruptedException {
		float startingYaw = HeadingSensor.yaw.get();

		if (degree > 0) {
			DriveCode.rotate.set(rotateSpeed.get());
			waitUntilAtLeast(HeadingSensor.yaw, startingYaw + degree);
		} else {
			DriveCode.rotate.set(-rotateSpeed.get());
			waitUntilAtMost(HeadingSensor.yaw, startingYaw + degree);
		}

		DriveCode.rotate.set(0.0f);
	}

	protected void collectTote() throws AutonomousModeOverException,
			InterruptedException {
		QuasarHelios.autoLoader.set(true);
		waitUntil(AutoLoader.done);
	}
	
	protected void setClampOpen(boolean value) throws InterruptedException, AutonomousModeOverException {
		QuasarHelios.clamp.openControl.set(value);
		waitForTime(30);
	}
	
	protected void setClampHeight(float value) throws AutonomousModeOverException, InterruptedException {
		QuasarHelios.clamp.heightControl.set(value);
		waitUntil(FloatMixing.floatIsInRange(QuasarHelios.clamp.heightReadout, value - clampHeightPadding.get(), value + clampHeightPadding.get()));
	}
	
	protected void ejectTotes() throws AutonomousModeOverException, InterruptedException {
		QuasarHelios.autoEjector.set(true);
		waitUntil(AutoEjector.done);
	}

	@Override
	protected void autonomousMain() throws AutonomousModeOverException,
			InterruptedException {
		try {
			runAutonomous();
		} finally {
			DriveCode.allMotors.set(0);
		}
	}

	protected abstract void runAutonomous() throws InterruptedException,
			AutonomousModeOverException;
}
