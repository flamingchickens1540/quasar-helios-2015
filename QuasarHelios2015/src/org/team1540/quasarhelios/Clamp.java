package org.team1540.quasarhelios;

import ccre.channel.BooleanOutput;
import ccre.channel.EventInput;
import ccre.channel.FloatFilter;
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
	public final BooleanOutput openControl = BooleanMixing.combine(Igneous.makeSolenoid(0), Igneous.makeSolenoid(1));
	public final FloatOutput heightControl;

	public final FloatInputPoll heightReadout;

	public Clamp() {
		heightControl = new FloatStatus();

		FloatFilter limit = FloatMixing.limit(0.0f, 1.0f);
		FloatInput desiredHeight = limit.wrap((FloatInput) heightControl);

		FloatInputPoll encoder = Igneous.makeEncoder(4, 5, false);
		FloatOutput speedControl = Igneous.makeTalonMotor(11, Igneous.MOTOR_REVERSE, 0.1f);

		EventInput limitTop = EventMixing.filterEvent(Igneous.makeDigitalInput(2), true, Igneous.globalPeriodic);
		EventInput limitBottom = EventMixing.filterEvent(Igneous.makeDigitalInput(3), true, Igneous.globalPeriodic);

		TuningContext context = new TuningContext("Clamp").publishSavingEvent();

		FloatStatus min = context.getFloat("clamp-min", 0.0f);
		FloatStatus max = context.getFloat("clamp-max", 1.0f);

		FloatMixing.pumpWhen(limitTop, encoder, max);
		FloatMixing.pumpWhen(limitBottom, encoder, min);

		FloatStatus p = context.getFloat("clamp-p", 1.0f);
		FloatStatus i = context.getFloat("clamp-i", 0.0f);
		FloatStatus d = context.getFloat("clamp-d", 0.0f);

		heightReadout = FloatMixing.normalizeFloat(encoder, min, max);

		PIDControl pid = new PIDControl(heightReadout, desiredHeight, p, i, d);

		QuasarHelios.globalControl.send(pid);

		pid.send(speedControl);

		Cluck.publish(QuasarHelios.testPrefix + "Clamp Open Control", openControl);
		Cluck.publish(QuasarHelios.testPrefix + "Clamp Height Encoder", FloatMixing.createDispatch(encoder, Igneous.globalPeriodic));
		Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Top", limitTop);
		Cluck.publish(QuasarHelios.testPrefix + "Clamp Limit Bottom", limitBottom);
		Cluck.publish(QuasarHelios.testPrefix + "Clamp Motor Speed", speedControl);
	}
}
