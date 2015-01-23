package org.team1540.quasarhelios;

import ccre.channel.FloatInputPoll;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;

public class AutonomousModeFull extends AutonomousModeBase {
	public FloatInputPoll nudge;
	public FloatInputPoll toteDistance;
	public FloatInputPoll autoZoneDistance;
	public FloatInputPoll strafeDistance;
	
	public AutonomousModeFull() {
		super("Full Auto");
	}

	@Override
	protected void runAutonomous() throws InterruptedException,
			AutonomousModeOverException {
		collectTote();
		drive(nudge.get());
		// TODO: Forklift container
		// TODO: Hold container with top claw
		drive(toteDistance.get());
		collectTote();
		drive(nudge.get());
		// TODO: Forklift container
		drive(toteDistance.get());
		collectTote();
		turn(90);
		drive(autoZoneDistance.get());
		// TODO: Put down container
		DriveCode.octocanumShifting.set(true);
		// TODO: Strafe right
		// TODO: Put down container
		// TODO: Strafe right
		// TODO: Spit out totes
		DriveCode.octocanumShifting.set(false);
	}
	
	public void loadSettings(TuningContext context) {
		this.nudge = context.getFloat("auto.full.nudge", 1.0f);
		this.toteDistance = context.getFloat("auto.full.toteDistance", 7.0f);
		this.autoZoneDistance = context.getFloat("auto.full.autoZoneDistance", 5.0f);
		this.strafeDistance = context.getFloat("auto.full.strafeDistance", 2.0f);
	}

}
