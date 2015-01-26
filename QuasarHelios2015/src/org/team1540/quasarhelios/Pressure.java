package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.igneous.Igneous;

public class Pressure {
	public static final BooleanInput pressureSwitch = BooleanMixing.createDispatch(Igneous.getPCMPressureSwitch(), Igneous.constantPeriodic);
	public static FloatInput pressureGauge;
	public static final BooleanOutput compressor = Igneous.usePCMCompressor();
	
	public static void setup() {
		
		pressureGauge = FloatMixing.createDispatch(FloatMixing.normalizeFloat(Igneous.makeAnalogInput(0), 
				ControlInterface.mainTuning.getFloat("main-pressure-min", 0.0f), ControlInterface.mainTuning.getFloat("pressure-max", 1.0f)), Igneous.globalPeriodic);
		
		Cluck.publish("Pressure Switch", pressureSwitch);
		Cluck.publish("Pressure Gauge", pressureGauge);
		Cluck.publish("Compressor Enable", compressor);
	}
}
