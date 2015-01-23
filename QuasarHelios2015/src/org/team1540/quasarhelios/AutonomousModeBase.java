package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;

public abstract class AutonomousModeBase extends InstinctModeModule {
	public AutonomousModeBase(String modeName) {
		super(modeName);
	}
	
	protected void drive(float distance) throws AutonomousModeOverException, InterruptedException {
		float startingEncoder = DriveCode.leftEncoder.get();
		DriveCode.allMotors.set(distance > 0 ? 1.0f : -1.0f);
		
		if (distance > 0) {
			waitUntilAtLeast(DriveCode.leftEncoder, startingEncoder + distance);
		} else {
			waitUntilAtMost(DriveCode.leftEncoder, startingEncoder + distance);
		}

		DriveCode.allMotors.set(0.0f);
	}
	
	protected void turn(float degree) throws AutonomousModeOverException, InterruptedException {
		float startingYaw = HeadingSensor.yaw.get();
		DriveCode.rotate.set(degree > 0 ? 1.0f : -1.0f);

		if (degree > 0) {
			waitUntilAtLeast(HeadingSensor.yaw, startingYaw + degree);
		} else {
			waitUntilAtMost(HeadingSensor.yaw, startingYaw + degree);
		}
		
		DriveCode.rotate.set(0.0f);
	}
	
	protected void collectTote() throws AutonomousModeOverException, InterruptedException {
		QuasarHelios.autoLoader.set(true);
		waitUntil(BooleanMixing.invert((BooleanInputPoll) QuasarHelios.autoLoader));
	}
	
    @Override
    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
            runAutonomous();
        } finally {
            DriveCode.allMotors.set(0);
        }
    }
    
    protected abstract void runAutonomous() throws InterruptedException, AutonomousModeOverException;
    
}
