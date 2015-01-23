package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoEjector extends InstinctModule {
	public BooleanStatus create() {
		BooleanStatus b = new BooleanStatus(false);
		AutoEjector a = new AutoEjector();
		a.setShouldBeRunning(b);
		a.updateWhen(Igneous.constantPeriodic);
		return b;
	}

	@Override
	protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
		Elevator.lowering.set(true);
		Elevator.raising.set(false);
		
		waitUntil(Elevator.bottomLimitSwitch);
		
		Rollers.open.set(true);
		Rollers.direction.set(false);
		Rollers.running.set(true);
	}
	
}
