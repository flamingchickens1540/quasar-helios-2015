package org.team1540.quasarhelios;

import ccre.channel.BooleanCell;
import ccre.channel.EventCell;
import ccre.cluck.Cluck;
import ccre.frc.FRC;

public class ContainerGrabber {
    public static final BooleanCell containerGrabberSolenoid = new BooleanCell(FRC.solenoid(0));
    public static final EventCell containerGrabButton = new EventCell();

    public static void setup() {
        containerGrabberSolenoid.toggleWhen(containerGrabButton.and(ControlInterface.mainTuning.getBoolean("Container Grab Enabled +A", true)));

        Cluck.publish("Container Grab Actuated", containerGrabberSolenoid);
    }
}
