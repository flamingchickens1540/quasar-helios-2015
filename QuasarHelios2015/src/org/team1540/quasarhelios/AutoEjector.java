package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoEjector extends InstinctModule {
	public static final BooleanStatus done = new BooleanStatus(false);
    private FloatInputPoll timeout = ControlInterface.mainTuning.getFloat("main-ejectorTimeout", 2.0f);

	public static BooleanStatus create() {
		BooleanStatus b = new BooleanStatus(false);
		AutoEjector a = new AutoEjector();
		
		a.setShouldBeRunning(b);
		a.updateWhen(Igneous.constantPeriodic);
		
		Rollers.running.setFalseWhen(BooleanMixing.onRelease(b));
		Rollers.direction.setTrueWhen(BooleanMixing.onRelease(b));
		
		return b;
	}

	@Override
	protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
		try {
			done.set(false);
			Elevator.lowering.set(true);
			Elevator.raising.set(false);
			
			waitUntil(Elevator.bottomLimitSwitch);
			
			Rollers.open.set(true);
			Rollers.direction.set(false);
			Rollers.running.set(true);
			
			waitUntil(BooleanMixing.invert(AutoLoader.crateInPosition));
			waitForTime(timeout);
			
			Rollers.running.set(false);
		} finally {
			done.set(true);
		}
	}
}
