package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.EventStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.EventMixing;
import ccre.igneous.Igneous;

public class ContainerGrabber {
    public static final BooleanStatus containerGrabberSolenoid = new BooleanStatus(Igneous.makeSolenoid(0));
    public static final EventStatus containerGrabButton = new EventStatus();

    public static void setup() {
        containerGrabberSolenoid.toggleWhen(EventMixing.filterEvent(ControlInterface.mainTuning.getBoolean("Container Grab Enabled +A", true), true, (EventInput) containerGrabButton));

        Cluck.publish("Container Grab Actuated", containerGrabberSolenoid);
    }
}
