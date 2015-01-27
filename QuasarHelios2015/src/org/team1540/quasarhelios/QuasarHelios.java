package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.ctrl.EventMixing;
import ccre.igneous.Igneous;
import ccre.igneous.IgneousApplication;

/**
 * The main class for QuasarHelios. This dispatches to all of the other modules.
 */
public class QuasarHelios implements IgneousApplication {
    public static BooleanStatus autoLoader;
    public static BooleanStatus autoEjector;
    public static final EventInput globalControl = EventMixing.filterEvent(Igneous.getIsTest(), false, Igneous.globalPeriodic);
    public static final String testPrefix = "(Test) ";

    public void setupRobot() {
        ControlInterface.setup();
        HeadingSensor.setup();
        DriveCode.setup();
        Elevator.setup();
        Clamp.setup();
        Rollers.setup();
        Autonomous.setup();
        Suspension.setup();
        Pressure.setup();
        autoLoader = AutoLoader.create();
        autoEjector = AutoEjector.create();
    }
}
