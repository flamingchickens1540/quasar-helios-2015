package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.EventStatus;
import ccre.cluck.Cluck;
import ccre.frc.FRC;

public class ContainerGrabber {
    public static final BooleanStatus containerGrabberSolenoid = new BooleanStatus(FRC.makeSolenoid(0));
    public static final EventStatus containerGrabButton = new EventStatus();

    public static void setup() {
        containerGrabberSolenoid.toggleWhen(containerGrabButton.and(ControlInterface.mainTuning.getBoolean("Container Grab Enabled +A", true)));

        Cluck.publish("Container Grab Actuated", containerGrabberSolenoid);
    }
}
