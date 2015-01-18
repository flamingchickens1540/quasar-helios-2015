package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.instinct.InstinctMultiModule;
public class Autonomous {
	public static InstinctMultiModule mainModule;
	
	public static void setup() {
		Igneous.registerAutonomous(mainModule);
	}
}
