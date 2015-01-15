package org.team1540.quasarhelios;

import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.concurrency.ReporterThread;

public class PositionTracking {
	private Vector2 position;
	private boolean running;
	
	private final DisplacementSensor displacementSensor;
	private final HeadingSensor headingSensor;
	
	private final FloatStatus xAxis;
	private final FloatStatus yAxis;
	
	public PositionTracking(DisplacementSensor displacementSensor, HeadingSensor headingSensor) {
		this.displacementSensor = displacementSensor;
		this.headingSensor = headingSensor;
		
		position = new Vector2(0.0f, 0.0f);
		running = true;
		
		xAxis = new FloatStatus();
		yAxis = new FloatStatus();
		
		new ReporterThread("PositionTracking Polling/Integration") {
			@Override
			public void threadBody() {
				while (running) {
					Vector2 displacement = displacementSensor.getDisplacement();
					float heading = headingSensor.getHeading();
					
					position = position.plus(displacement.rotate((float)Math.toRadians(heading)));
					
					xAxis.set(position.x);
					yAxis.set(position.y);
				}
			}
		}.start();
	}
	
	public void destroy() {
		running = false;
		displacementSensor.destroy();
		headingSensor.destroy();
	}
	
	public Vector2 getPosition() {
		return position;
	}
	
	public FloatInput getXAxis() {
		return xAxis;
	}
	
	public FloatInput getYAxis() {
		return yAxis;
	}
}
