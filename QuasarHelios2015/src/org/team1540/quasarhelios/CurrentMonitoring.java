package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.EventInput;
import ccre.channel.EventLogger;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.PauseTimer;
import ccre.igneous.Igneous;
import ccre.log.LogLevel;

public class CurrentMonitoring {
    public static final FloatInput[] channels = new FloatInput[16];
    private static final FloatInputPoll threshold = ControlInterface.mainTuning.getFloat("Current Monitoring Threshold +M", 1.5f);
    static {
        for (int i = 0; i < 16; i++) {
            channels[i] = FloatMixing.createDispatch(Igneous.getPDPChannelCurrent(i), Igneous.globalPeriodic);
        }
    }

    private static final FloatInputPoll driveThreshold = ControlInterface.mainTuning.getFloat("Drive Disabled Current Threshold +M", 60f);

    public static void setup() {
        for (int i = 0; i < 16; i++) {
            Cluck.publish("Current " + i, channels[i]);
            BooleanInput overThreshold = FloatMixing.floatIsAtLeast(channels[i], threshold);
            Cluck.publish("Current " + i + "Active", overThreshold);
            overThreshold.send(BooleanMixing.triggerWhenBooleanChanges(
                    new EventLogger(LogLevel.FINEST, "[LOCAL] Power " + i + " OFF"),
                    new EventLogger(LogLevel.FINEST, "[LOCAL] Power " + i + " ON")));
        }
        BooleanInput anyFault = BooleanMixing.orBooleans(setupDriveMotorCurrent(0), setupDriveMotorCurrent(1), setupDriveMotorCurrent(8), setupDriveMotorCurrent(9));
        QuasarHelios.publishFault("Drive Motor Current Master", anyFault);
        BooleanMixing.andBooleans(anyFault, ControlInterface.mainTuning.getBoolean("Drive Motor Current Monitor Enable +M", false)).send(DriveCode.disableMotorsForCurrentFault);
    }

    private static BooleanInput setupDriveMotorCurrent(int port) {
        EventInput onOvercurrent = EventMixing.filterEvent(FloatMixing.floatIsAtLeast(channels[port], driveThreshold), true, Igneous.globalPeriodic);
        PauseTimer duringOvercurrent = new PauseTimer(1000);
        onOvercurrent.send(duringOvercurrent);
        QuasarHelios.publishFault("Drive Motor Current (" + port + ")", duringOvercurrent);
        return duringOvercurrent;
    }
}
