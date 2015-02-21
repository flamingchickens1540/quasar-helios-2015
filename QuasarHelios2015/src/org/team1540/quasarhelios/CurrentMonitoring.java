package org.team1540.quasarhelios;

import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.FloatMixing;
import ccre.igneous.Igneous;

public class CurrentMonitoring {
    public static FloatInput[] channels = new FloatInput[16];
    static {
        for (int i = 0; i < 16; i++) {
            channels[i] = FloatMixing.createDispatch(Igneous.getPDPChannelCurrent(i), Igneous.globalPeriodic);
        }
    }

    public static void setup() {
        for (int i = 0; i < 16; i++) {
            Cluck.publish("Current " + i, channels[i]);
        }
    }
}
