package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeOneTote extends AutonomousModeBase {
	protected FloatInputPoll toteDistance;
	protected FloatInputPoll autoZoneDistance;
	protected FloatInputPoll returnDistance;
	
	public AutonomousModeOneTote() {
		super("One Tote");
	}

	@Override
	protected void runAutonomous() throws InterruptedException,
			AutonomousModeOverException {
		collectTote();
		turn(90);
		drive(autoZoneDistance.get());
		// TODO: dump out tote
		drive(-returnDistance.get());
	}
	
	public void loadSettings(TuningContext context) {
		this.toteDistance = context.getFloat("auto-onetote-toteDistance", 2.0f);
		this.autoZoneDistance = context.getFloat("auto-onetote-autoZoneDistance", 2.0f);
		this.returnDistance = context.getFloat("auto-onetote-returnDistance", 2.0f);
	}

}
