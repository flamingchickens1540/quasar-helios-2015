package org.team1540.quasarhelios;

public class PositionTracking {
	private static Vector2 position;
	private static boolean running;
	
	public static void setup() {
		DisplacementSensor.setup();
		HeadingSensor.setup();
		
		position = new Vector2(0.0f, 0.0f);
		running = true;
		
		new Thread() {
			@Override
			public void run() {
				while (running) {
					float heading = HeadingSensor.getHeading();
					Vector2 displacement = DisplacementSensor.getDisplacement();
					
					position = position.plus(displacement.rotate((float)Math.toRadians(heading)));
				}
			}
		}.start();
	}
	
	public static void destroy() {
		running = false;
	}
	
	public static Vector2 getPosition() {
		return position;
	}
}
