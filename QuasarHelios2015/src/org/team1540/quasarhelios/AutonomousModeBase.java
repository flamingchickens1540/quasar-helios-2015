package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;

public abstract class AutonomousModeBase extends InstinctModeModule {
	private FloatInputPoll encoderScaling;
	public AutonomousModeBase(String modeName) {
		super(modeName);
	}
	
	protected void drive(float distance) throws AutonomousModeOverException, InterruptedException {
		float currentEncoder = DriveCode.leftEncoder.get() * encoderScaling.get();
		if (distance > 0) {
			DriveCode.allMotors.set(1.0f);
		} else {
			DriveCode.allMotors.set(-1.0f);
		}

		waitUntilAtLeast(FloatMixing.multiplication.of(DriveCode.leftEncoder, encoderScaling), currentEncoder * encoderScaling.get() + distance);
		DriveCode.allMotors.set(0.0f);
	}
	
	protected void turn(float degree) throws AutonomousModeOverException, InterruptedException {
		float currentYaw = HeadingSensor.yaw.get();
		if (degree > 0) {
			DriveCode.rotate.set(1.0f);
		} else {
			DriveCode.rotate.set(-1.0f);
		}

		waitUntilAtLeast(HeadingSensor.yaw, currentYaw + degree);
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
    
    public void loadSettings(TuningContext context) {
    	// This is in ????
    	this.encoderScaling = context.getFloat("autonomous-encoder", 0.1f);
    }

}
