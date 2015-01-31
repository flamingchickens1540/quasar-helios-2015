package org.team1540.quasarhelios;

import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.FloatMixing;
import ccre.igneous.Igneous;

public class CurrentMonitoring {
    public static void setup() {
        for (int i = 0; i < 16; i++) {
            FloatStatus zero = ControlInterface.mainTuning.getFloat("current-zero-" + i, 0.0f);
            FloatInput current = FloatMixing.createDispatch(Igneous.getPDPChannelCurrent(i), Igneous.globalPeriodic);

            Cluck.publish("Zero Current " + i, FloatMixing.pumpEvent(current, zero));

            Cluck.publish("Current " + i, FloatMixing.subtraction.of(current, (FloatInput) zero));
        }
    }
}
