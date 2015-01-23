package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;

public abstract class AutonomousModeBase extends InstinctModeModule {
	public FloatInputPoll driveSpeed;
	public FloatInputPoll rotateSpeed;

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
			DriveCode.allMotors.set(-1.0f * driveSpeed.get());
			waitUntilAtMost(DriveCode.leftEncoder, startingEncoder + distance);
		}

		DriveCode.allMotors.set(0.0f);
	}

	protected void turn(float degree) throws AutonomousModeOverException,
			InterruptedException {
		float startingYaw = HeadingSensor.yaw.get();

		if (degree > 0) {
			DriveCode.rotate.set(rotateSpeed.get());
			waitUntilAtLeast(HeadingSensor.yaw, startingYaw + degree);
		} else {
			DriveCode.rotate.set(-1.0f * rotateSpeed.get());
			waitUntilAtMost(HeadingSensor.yaw, startingYaw + degree);
		}

		DriveCode.rotate.set(0.0f);
	}

	protected void collectTote() throws AutonomousModeOverException,
			InterruptedException {
		QuasarHelios.autoLoader.set(true);
		waitUntil(BooleanMixing
				.invert((BooleanInputPoll) QuasarHelios.autoLoader));
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

	public void loadSettings(TuningContext context) {
		this.driveSpeed = context.getFloat("auto-main-driveSpeed", 1.0f);
		this.rotateSpeed = context.getFloat("auto-main-rotateSpeed", 1.0f);
	}

}
