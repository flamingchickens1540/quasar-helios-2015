package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.EventLogger;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.igneous.Igneous;
import ccre.log.LogLevel;

public class CurrentMonitoring {
    public static FloatInput[] channels = new FloatInput[16];
    private static FloatInputPoll threshold = ControlInterface.mainTuning.getFloat("Current Monitoring Threshold", 1.5f);
    static {
        for (int i = 0; i < 16; i++) {
            channels[i] = FloatMixing.createDispatch(Igneous.getPDPChannelCurrent(i), Igneous.globalPeriodic);
        }
    }

    public static void setup() {
        for (int i = 0; i < 16; i++) {
            Cluck.publish("Current " + i, channels[i]);
            BooleanInput overThreshold = FloatMixing.floatIsAtLeast(channels[i], threshold);
            Cluck.publish("Current " + i + "Active", overThreshold);
            overThreshold.send(BooleanMixing.triggerWhenBooleanChanges(
                    new EventLogger(LogLevel.FINEST, "[LOCAL] Power " + i + " OFF"),
                    new EventLogger(LogLevel.FINEST, "[LOCAL] Power " + i + " ON")));
        }
    }
}
