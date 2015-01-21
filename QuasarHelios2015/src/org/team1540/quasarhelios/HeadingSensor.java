package org.team1540.quasarhelios;

import ccre.drivers.chrobotics.UM7LT;
import ccre.igneous.Igneous;

public class HeadingSensor {

	public static void setup(){
		UM7LT sensor = new UM7LT(Igneous.makeRS232_MXP(115200, "UM7-LT"));
		sensor.autoreportFaults.set(true);
		sensor.start();
	}
}
