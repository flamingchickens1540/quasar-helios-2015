package org.team1540.quasarhelios;

import ccre.channel.BooleanCell;
import ccre.channel.FloatCell;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.ctrl.PIDController;
import ccre.frc.FRC;
import ccre.instinct.InstinctMultiModule;

public class Autonomous {
    public static InstinctMultiModule mainModule = new InstinctMultiModule(ControlInterface.autoTuning);
    public static FloatInput autoPID;
    public static FloatInput reversePID;
    public static final FloatCell desiredAngle = new FloatCell();

    public static void setup() {
        FloatCell ultgain = ControlInterface.autoTuning.getFloat("Auto PID Calibrating Ultimate Gain +A", .03f);
        FloatCell period = ControlInterface.autoTuning.getFloat("Auto PID Calibrating Oscillation Period +A", 2f);
        FloatCell pconstant = ControlInterface.autoTuning.getFloat("Auto PID Calibrating P Constant +A", .6f);
        FloatCell iconstant = ControlInterface.autoTuning.getFloat("Auto PID Calibrating I Constant +A", 2f);
        FloatCell dconstant = ControlInterface.autoTuning.getFloat("Auto PID Calibrating D Constant +A", .125f);
        BooleanCell calibrating = ControlInterface.autoTuning.getBoolean("Auto PID Calibrating +A", false);
        BooleanCell usePID = ControlInterface.autoTuning.getBoolean("Auto PID Enabled +A", false);

        FloatInput p = calibrating.toFloat(ultgain.multipliedBy(pconstant), ultgain).filterUpdates(calibrating);
        FloatInput i = calibrating.toFloat(p.multipliedBy(iconstant).dividedBy(period), 0);
        FloatInput d = calibrating.toFloat(p.multipliedBy(dconstant).multipliedBy(period), 0);

        PIDController pid = new PIDController(HeadingSensor.absoluteYaw, desiredAngle, p, i, d);
        pid.setOutputBounds(1f);
        pid.setIntegralBounds(.5f);
        FRC.duringAuto.send(pid);
        reversePID = usePID.toFloat(0, pid);
        autoPID = reversePID.negated();

        Cluck.publish("Auto PID Output", autoPID);

        mainModule.publishDefaultControls(true, true);
        mainModule.publishRConfControls();
        mainModule.addMode(new AutonomousModeDrive());
        mainModule.addMode(new AutonomousModeBump());
        mainModule.addMode(new AutonomousModeGrabContainer());
        mainModule.addMode(new AutonomousModeToteContainer());
        mainModule.addMode(new AutonomousModeNoodlePrep());
        mainModule.addMode(new AutonomousModeNoodleAndTote());
        mainModule.addMode(new AutonomousModeContainerTote());
        mainModule.addMode(new AutonomousModeThreeTotes());
        mainModule.loadSettings(mainModule.addNullMode("none", "I'm a sitting chicken!"));
        FRC.registerAutonomous(mainModule);
    }
}
