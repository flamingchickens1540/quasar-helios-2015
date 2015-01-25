package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoLoader extends InstinctModule {
	public static BooleanStatus done = new BooleanStatus(false);
    private static final BooleanInput crateInPosition = BooleanMixing.createDispatch(Igneous.makeDigitalInput(10), Igneous.globalPeriodic);

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus(false);
        AutoLoader a = new AutoLoader();

        a.setShouldBeRunning(b);
        a.updateWhen(Igneous.globalPeriodic);

        Elevator.raising.setFalseWhen(BooleanMixing.onRelease(b));
        Elevator.lowering.setTrueWhen(BooleanMixing.onRelease(b));
        
    	Cluck.publish(QuasarHelios.testPrefix + "Crate Loaded", crateInPosition);

        return b;
    }

    @Override
    public void autonomousMain() throws AutonomousModeOverException, InterruptedException {
    	try {
	    	done.set(false);
	        Elevator.lowering.set(false);
	        Elevator.raising.set(true);
	
	        waitUntil(Elevator.topLimitSwitch);
	
	        boolean r = Rollers.running.get();
	        boolean d = Rollers.direction.get();
	        boolean o = Rollers.open.get();
	
	        Rollers.direction.set(true);
	        Rollers.running.set(true);
	        Rollers.open.set(false);
	
	        waitUntil(crateInPosition);
	
	        Rollers.running.set(r);
	        Rollers.direction.set(d);
	        Rollers.open.set(o);
	
	        Elevator.raising.set(false);
	        Elevator.lowering.set(true);
    	} finally {
    		done.set(true);
    	}
    }
}
