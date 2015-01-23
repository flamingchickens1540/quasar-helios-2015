package org.team1540.quasarhelios;

import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;

public abstract class AutonomousModeBase extends InstinctModeModule {
	public AutonomousModeBase(String modeName) {
		super(modeName);
	}
	
	protected void drive(float distance) throws AutonomousModeOverException, InterruptedException {
		float currentEncoder = DriveCode.leftEncoder.get();
		DriveCode.allMotors.set(distance/distance);
		waitUntilAtLeast(DriveCode.leftEncoder, currentEncoder + distance);
		DriveCode.allMotors.set(0.0f);
	}
	
	protected void turn(float degree) throws AutonomousModeOverException, InterruptedException {
		DriveCode.rotate.set(degree/degree);
		waitUntilAtLeast(HeadingSensor.yaw, degree);
		DriveCode.rotate.set(0.0f);
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
    	context.getFloat("autonomous-encoder", 0.1f);
    }

}
