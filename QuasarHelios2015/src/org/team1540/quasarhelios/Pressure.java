package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.holders.TuningContext;
import ccre.igneous.Igneous;

public class Pressure {
	public static final BooleanInput pressureSwitch = BooleanMixing
			.createDispatch(Igneous.getPCMPressureSwitch(),
					Igneous.constantPeriodic);
	public static FloatInput pressureGauge;
	public static final BooleanOutput compressor = Igneous.usePCMCompressor();

	public static void setup() {
		TuningContext context = new TuningContext("Pressure")
				.publishSavingEvent();

		pressureGauge = FloatMixing.createDispatch(
				FloatMixing.normalizeFloat(Igneous.makeAnalogInput(0),
						context.getFloat("pressure-min", 0.0f),
						context.getFloat("pressure-max", 1.0f)),
				Igneous.globalPeriodic);

		Cluck.publish("Pressure Switch", pressureSwitch);
		Cluck.publish("Pressure Gauge", pressureGauge);
		Cluck.publish("Compressor Enable", compressor);
	}
}
