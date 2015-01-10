package org.team1540.quasarhelios;

public class Vector2 {
	public float x;
	public float y;
	
	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public float mag() {
		return (float)Math.sqrt(x * x + y * y);
	}
	
	public float dir() {
		return (float)Math.atan2(y, x);
	}
	
	public Vector2 rotate(float angle) {
		float s = (float)Math.sin(angle);
		float c = (float)Math.cos(angle);
		
		float nx = x * c + y * s;
		float ny = y * c - x * s;
		
		return new Vector2(nx, ny);
	}
	
	public Vector2 plus(Vector2 vec) {
		return new Vector2(x + vec.x, y + vec.y);
	}
}
