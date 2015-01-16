package org.team1540.quasarhelios;

import ccre.igneous.IgneousApplication;

/**
 * The main class for QuasarHelios. This dispatches to all of the other modules.
 */
public class QuasarHelios implements IgneousApplication {

    public void setupRobot() {
        ControlInterface.setup();
        PositionTracking.setup();
        DriveCode.setup();
        Rollers.setup();
    }
}
