package org.team1540.quasarhelios;

import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.drivers.chrobotics.UM7LT;
import ccre.igneous.Igneous;

public class HeadingSensor {
	public static FloatInput pitch;
	public static FloatInput yaw;
	public static FloatInput roll;
	
	public static FloatInput pitchRate;
	public static FloatInput yawRate;
	public static FloatInput rollRate;
	
	public static EventOutput zeroGyro;

	public static void setup(){
		UM7LT sensor = new UM7LT(Igneous.makeRS232_MXP(115200, "UM7-LT"));
		sensor.autoreportFaults.set(true);
		sensor.start();
		
		pitch = sensor.pitch;
		yaw = sensor.yaw;
		roll = sensor.roll;
		
		pitchRate = sensor.pitchRate;
		yawRate = sensor.yawRate;
		rollRate = sensor.rollRate;
		
		zeroGyro = sensor.zeroGyro;
	}
}
