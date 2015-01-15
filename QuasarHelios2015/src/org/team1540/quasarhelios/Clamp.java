package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanOutput;
import ccre.channel.EventInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.PIDControl;
import ccre.igneous.Igneous;

public class Clamp {
	public BooleanOutput openControl;
	public FloatOutput heightControl;
	
	//private Integrator heightInput;
	
	private FloatStatus min;
	private FloatStatus max;
	
	public Clamp() {
		
		BooleanOutput leftClamp = Igneous.makeSolenoid(0);
		BooleanOutput rightClamp = Igneous.makeSolenoid(1);
		
		openControl = BooleanMixing.combine(leftClamp, rightClamp);
		heightControl = new FloatStatus();
		min = new FloatStatus(0.0f);
		max = new FloatStatus(1.0f);
		
		FloatInputPoll encoder = Igneous.makeEncoder(0, 0, Igneous.MOTOR_FORWARD);
		FloatOutput speedControl = Igneous.makeTalonMotor(2, Igneous.MOTOR_FORWARD, 1.0f);
		
		BooleanInputPoll inputLimitTop = Igneous.makeDigitalInput(0);
		BooleanInputPoll inputLimitBottom = Igneous.makeDigitalInput(1);
		
		EventInput limitTop = EventMixing.filterEvent(inputLimitTop, true, Igneous.constantPeriodic);
		EventInput limitBottom = EventMixing.filterEvent(inputLimitBottom, true, Igneous.constantPeriodic);
		
		//heightInput = new Integrator(Igneous.constantPeriodic, encoder);
		
		
		FloatMixing.pumpWhen(limitTop, encoder, max);
		FloatMixing.pumpWhen(limitBottom, encoder, min);
		
		PIDControl pid = new PIDControl(FloatMixing.normalizeFloat(encoder, min, max), (FloatStatus)heightControl, FloatMixing.always(1.0f), FloatMixing.always(0.0f), FloatMixing.always(0.0f));
		Igneous.constantPeriodic.send(pid);
		
		pid.send(speedControl);
	}
	/*
	private class Integrator implements FloatInputPoll, FloatOutput {
		private float currentValue;
		private FloatInputPoll base;
		private long lastUpdate;
		
		public Integrator(EventInput update, FloatInputPoll base) {
			this.currentValue = 0.0f;
			this.base = base;
			this.lastUpdate = System.nanoTime();
			
			update.send(new EventOutput() {
				@Override
				public void event() {
					long currentTime = System.nanoTime();
					float dt = (currentTime - lastUpdate) / 1e9f;
					lastUpdate = currentTime;
					
					currentValue += base.get() * dt;
				}
			});
		}

		@Override
		public float get() {
			return currentValue;
		}
		
		@Override
		public void set(float value) {
			currentValue = value;
		}
	}*/
}
