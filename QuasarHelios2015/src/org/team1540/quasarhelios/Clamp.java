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
import ccre.igneous.Igneous;

public class Clamp {
	public final BooleanOutput openControl = BooleanMixing.combine(Igneous.makeSolenoid(0), Igneous.makeSolenoid(1));
	public final FloatOutput heightControl;
	
	private FloatStatus min = new FloatStatus(0.0f);
	private FloatStatus max = new FloatStatus(1.0f);
	
	public Clamp() {
		FloatStatus height = new FloatStatus();
		heightControl = height;
		
		FloatInputPoll encoder = Igneous.makeEncoder(0, 1, false);
		FloatOutput speedControl = Igneous.makeTalonMotor(2, Igneous.MOTOR_REVERSE, 0.1f);
		
		EventInput limitTop = EventMixing.filterEvent(Igneous.makeDigitalInput(2), true, Igneous.constantPeriodic);
		EventInput limitBottom = EventMixing.filterEvent(Igneous.makeDigitalInput(3), true, Igneous.constantPeriodic);
		
		FloatMixing.pumpWhen(limitTop, encoder, max);
		FloatMixing.pumpWhen(limitBottom, encoder, min);
		
		PIDControl pid = new PIDControl(FloatMixing.normalizeFloat(encoder, min, max), height, FloatMixing.always(1.0f), FloatMixing.always(0.0f), FloatMixing.always(0.0f));
		
		Igneous.constantPeriodic.send(pid);
		
		pid.send(speedControl);
		
		
	}
}
