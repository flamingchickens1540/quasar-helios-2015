package org.team1540.quasarhelios;

public class DisplacementSensor {
	private static float dpi;
	
	public static void setup(float dpi) {
		DisplacementSensor.dpi = dpi;
	}
	
	public static Vector2 getDisplacement() {
		int x = 0;
		int y = 0;
		
		return new Vector2(x / dpi, y / dpi);
	}
}
