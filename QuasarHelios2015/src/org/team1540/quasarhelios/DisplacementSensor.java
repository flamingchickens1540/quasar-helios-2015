package org.team1540.quasarhelios;

public class DisplacementSensor {
	private final float dpi;
	
	public DisplacementSensor(float dpi) {
		this.dpi = dpi;
	}
	
	public Vector2 getDisplacement() {
		int x = 0;
		int y = 0;
		
		return new Vector2(x / dpi, y / dpi);
	}
	
	public void destroy() {
		
	}
}
