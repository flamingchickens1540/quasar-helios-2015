package org.team1540.quasarhelios;

import ccre.channel.FloatOutput;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;

public abstract class AutonomousModeBase extends InstinctModeModule {
	public static FloatOutput allDriveMotors;
	
	public AutonomousModeBase(String modeName) {
		super(modeName);
	}
	
    @Override
    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
            runAutonomous();
        } finally {
            allDriveMotors.set(0);
        }
    }
    
    protected abstract void runAutonomous() throws InterruptedException, AutonomousModeOverException;

}
