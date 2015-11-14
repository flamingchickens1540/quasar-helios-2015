package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.frc.FRC;
import ccre.log.Logger;
import ccre.timers.PauseTimer;

public class CurrentMonitoring {
    public static final FloatInput[] channels = new FloatInput[16];
    private static final FloatInput threshold = ControlInterface.mainTuning.getFloat("Current Monitoring Threshold +M", 1.5f);

    static {
        for (int i = 0; i < 16; i++) {
            channels[i] = FRC.channelCurrentPDP(i);
        }
    }

    private static final FloatInput driveThreshold = ControlInterface.mainTuning.getFloat("Drive Disabled Current Threshold +M", 60f);

    public static void setup() {
        for (int i = 0; i < 16; i++) {
            Cluck.publish("Current " + i, channels[i]);
            BooleanInput overThreshold = channels[i].atLeast(threshold);
            Cluck.publish("Current " + i + "Active", overThreshold);
            final int li = i;
            overThreshold.onPress().send(() -> Logger.fine("[LOCAL] Power " + li + " ON"));
            overThreshold.onRelease().send(() -> Logger.fine("[LOCAL] Power " + li + " OFF"));
        }
        BooleanInput anyFault = setupDriveMotorCurrent(0).or(setupDriveMotorCurrent(1)).or(setupDriveMotorCurrent(8)).or(setupDriveMotorCurrent(9));
        QuasarHelios.publishFault("Drive Motor Current Master", anyFault);
        anyFault.and(ControlInterface.mainTuning.getBoolean("Drive Motor Current Monitor Enable +M", false)).send(DriveCode.disableMotorsForCurrentFault);
    }

    private static BooleanInput setupDriveMotorCurrent(int port) {
        PauseTimer duringOvercurrent = new PauseTimer(1000);
        FRC.globalPeriodic.and(channels[port].atLeast(driveThreshold)).send(duringOvercurrent);
        QuasarHelios.publishFault("Drive Motor Current (" + port + ")", duringOvercurrent);
        return duringOvercurrent;
    }
}
