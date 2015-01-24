package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.igneous.IgneousApplication;

/**
 * The main class for QuasarHelios. This dispatches to all of the other modules.
 */
public class QuasarHelios implements IgneousApplication {
	public static Clamp clamp;
	public static BooleanStatus autoLoader;
	public static BooleanStatus autoEjector;

	public void setupRobot() {
		ControlInterface.setup();
		HeadingSensor.setup();
		PositionTracking.setup();
		DriveCode.setup();
		Elevator.setup();
		Rollers.setup();
		Autonomous.setup();
		Suspension.setup();
		Pressure.setup();

		clamp = new Clamp();
		autoLoader = AutoLoader.create();
		autoEjector = AutoEjector.create();
	}
}
