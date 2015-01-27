package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.EventStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.igneous.Igneous;

public class Pressure {
	public static final BooleanInput pressureSwitch = BooleanMixing.createDispatch(Igneous.getPCMPressureSwitch(), Igneous.constantPeriodic);
	public static FloatInput pressureGauge;
	public static final BooleanOutput compressor = Igneous.usePCMCompressor();
	
	public static void setup() {
		FloatInput pressureInput = FloatMixing.createDispatch(Igneous.makeAnalogInput(0), Igneous.globalPeriodic);
		
		FloatStatus min = ControlInterface.mainTuning.getFloat("pressure-min", 0.0f);
		FloatStatus max = ControlInterface.mainTuning.getFloat("pressure-max", 1.0f);
		
		pressureGauge = FloatMixing.createDispatch(FloatMixing.normalizeFloat(pressureInput, min, max), Igneous.globalPeriodic);
		
		Cluck.publish("Pressure Switch", pressureSwitch);
		Cluck.publish("Pressure Gauge", pressureGauge);
		Cluck.publish("Pressure Gauge Raw", pressureInput);
		Cluck.publish("Compressor Enable", compressor);
		Cluck.publish("Pressure Min Set", FloatMixing.pumpEvent(pressureInput, min));
		Cluck.publish("Pressure Max Set", FloatMixing.pumpEvent(pressureInput, max));
	}
}
