package org.team1540.quasarhelios;

import ccre.channel.EventOutput;
import ccre.ctrl.BooleanMixing;
import ccre.igneous.Igneous;
import ccre.igneous.IgneousApplication;

/**
 * The main class for QuasarHelios. This dispatches to all of the other modules.
 */
public class QuasarHelios implements IgneousApplication {
	private Clamp clamp;

    public void setupRobot() {
        PositionTracking.setup();
        DriveCode.setup();
        ControlInterface.setup();
        
        clamp = new Clamp();
    }
}
