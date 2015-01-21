package org.team1540.quasarhelios;

import ccre.channel.EventOutput;
import ccre.igneous.Igneous;

public class AutoLoader implements EventOutput {
	private Clamp clamp;
	private BooleanInputPoll crateInPosition = Igneous.makeDigitalInput(0);
	
	public AutoLoader(Clamp clamp) {
		this.clamp = clamp;
	}
	
	@Override
	public void event() {
		clamp.openControl.set(true);
		clamp.heightControl.set(0.0f);
		
		Rollers.allRollers.set(1.0f);
		
		
	}
}

