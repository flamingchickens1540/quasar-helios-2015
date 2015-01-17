package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanOutput;
import ccre.channel.EventInput;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.PIDControl;
import ccre.holders.TuningContext;
import ccre.igneous.Igneous;

public class Clamp {
	public final BooleanOutput openControl = BooleanMixing.combine(
			Igneous.makeSolenoid(0), Igneous.makeSolenoid(1));
	public final FloatOutput heightControl;

	public Clamp() {
		FloatStatus height = new FloatStatus();
		heightControl = height;

		FloatInputPoll encoder = Igneous.makeEncoder(0, 1, false);
		FloatOutput speedControl = Igneous.makeTalonMotor(2, Igneous.MOTOR_REVERSE, 0.1f);

		EventInput limitTop = EventMixing.filterEvent(Igneous.makeDigitalInput(2), true, Igneous.constantPeriodic);
		EventInput limitBottom = EventMixing.filterEvent(Igneous.makeDigitalInput(3), true, Igneous.constantPeriodic);

		TuningContext context = new TuningContext("Clamp").publishSavingEvent();

		FloatStatus min = context.getFloat("clamp-min", 0.0f);
		FloatStatus max = context.getFloat("clamp-max", 1.0f);

		FloatMixing.pumpWhen(limitTop, encoder, max);
		FloatMixing.pumpWhen(limitBottom, encoder, min);

		FloatStatus p = context.getFloat("clamp-p", 1.0f);
		FloatStatus i = context.getFloat("clamp-i", 0.0f);
		FloatStatus d = context.getFloat("clamp-d", 0.0f);

		PIDControl pid = new PIDControl(FloatMixing.normalizeFloat(encoder, min, max), height, p, i, d);

		Igneous.constantPeriodic.send(pid);

		pid.send(speedControl);
	}
}
