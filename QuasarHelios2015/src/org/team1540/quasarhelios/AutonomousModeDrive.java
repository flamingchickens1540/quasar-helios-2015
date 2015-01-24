package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeDrive extends AutonomousModeBase {
	protected FloatInputPoll driveDistance;
	
	public AutonomousModeDrive() {
		super("Drive");
	}

	@Override
	protected void runAutonomous() throws InterruptedException,
			AutonomousModeOverException {
		drive(driveDistance.get());
	}
	
	public void loadSettings(TuningContext context) {
		super.loadSettings(context);
		this.driveDistance = context.getFloat("auto-drive-distance", 2.0f);
	}
}
