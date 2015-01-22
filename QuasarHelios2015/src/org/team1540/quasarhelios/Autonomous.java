package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.instinct.InstinctMultiModule;
public class Autonomous {
	public static InstinctMultiModule mainModule = new InstinctMultiModule(ControlInterface.autoTuning);
	
	public static void setup() {
		Igneous.registerAutonomous(mainModule);
	}
}
