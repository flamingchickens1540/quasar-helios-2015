package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.EventLogger;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.ctrl.PauseTimer;
import ccre.igneous.Igneous;
import ccre.log.LogLevel;

public class CurrentMonitoring {
    public static final FloatInput[] channels = new FloatInput[16];
    private static final FloatInput threshold = ControlInterface.mainTuning.getFloat("Current Monitoring Threshold +M", 1.5f);
    static {
        for (int i = 0; i < 16; i++) {
            channels[i] = Igneous.getPDPChannelCurrent(i);
        }
    }

    private static final FloatInput driveThreshold = ControlInterface.mainTuning.getFloat("Drive Disabled Current Threshold +M", 60f);

    public static void setup() {
        for (int i = 0; i < 16; i++) {
            Cluck.publish("Current " + i, channels[i]);
            BooleanInput overThreshold = channels[i].atLeast(threshold);
            Cluck.publish("Current " + i + "Active", overThreshold);
            EventLogger.log(overThreshold.onPress(), LogLevel.FINEST, "[LOCAL] Power " + i + " ON");
            EventLogger.log(overThreshold.onRelease(), LogLevel.FINEST, "[LOCAL] Power " + i + " OFF");
        }
        BooleanInput anyFault = setupDriveMotorCurrent(0).or(setupDriveMotorCurrent(1)).or(setupDriveMotorCurrent(8)).or(setupDriveMotorCurrent(9));
        QuasarHelios.publishFault("Drive Motor Current Master", anyFault);
        anyFault.and(ControlInterface.mainTuning.getBoolean("Drive Motor Current Monitor Enable +M", false)).send(DriveCode.disableMotorsForCurrentFault);
    }

    private static BooleanInput setupDriveMotorCurrent(int port) {
        PauseTimer duringOvercurrent = new PauseTimer(1000);
        duringOvercurrent.on(Igneous.globalPeriodic.and(channels[port].atLeast(driveThreshold)));
        QuasarHelios.publishFault("Drive Motor Current (" + port + ")", duringOvercurrent);
        return duringOvercurrent;
    }
}
