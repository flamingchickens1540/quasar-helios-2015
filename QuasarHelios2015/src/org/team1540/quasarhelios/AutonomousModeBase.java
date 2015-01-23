package org.team1540.quasarhelios;

import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;

public abstract class AutonomousModeBase extends InstinctModeModule {
	
	public void drive(float distance) throws AutonomousModeOverException, InterruptedException {
		DriveCode.allMotors.set(1.0f);
		waitUntilAtLeast(DriveCode.leftEncoder, distance);
		DriveCode.allMotors.set(0.0f);
	}
	
	public void turn(float degree) throws AutonomousModeOverException, InterruptedException {
		DriveCode.rotate.set(degree/degree);
		waitUntilAtLeast(HeadingSensor.yaw, degree);
		DriveCode.rotate.set(0.0f);
	}
	
	public AutonomousModeBase(String modeName) {
		super(modeName);
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
