package org.team1540.quasarhelios;

import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.ctrl.PIDControl;
import ccre.igneous.Igneous;
import ccre.instinct.InstinctMultiModule;

public class Autonomous {
    public static InstinctMultiModule mainModule = new InstinctMultiModule(ControlInterface.autoTuning);
    public static FloatInput autoPID;
    public static FloatInput reversePID;
    public static final FloatStatus desiredAngle = new FloatStatus();

    public static void setup() {
        FloatStatus ultgain = ControlInterface.autoTuning.getFloat("Auto PID Calibrating Ultimate Gain +A", .03f);
        FloatStatus period = ControlInterface.autoTuning.getFloat("Auto PID Calibrating Oscillation Period +A", 2f);
        FloatStatus pconstant = ControlInterface.autoTuning.getFloat("Auto PID Calibrating P Constant +A", .6f);
        FloatStatus iconstant = ControlInterface.autoTuning.getFloat("Auto PID Calibrating I Constant +A", 2f);
        FloatStatus dconstant = ControlInterface.autoTuning.getFloat("Auto PID Calibrating D Constant +A", .125f);
        BooleanStatus calibrating = ControlInterface.autoTuning.getBoolean("Auto PID Calibrating +A", false);
        BooleanStatus usePID = ControlInterface.autoTuning.getBoolean("Auto PID Enabled +A", true);

        FloatInput p = FloatMixing.createDispatch(
                Mixing.select(calibrating, FloatMixing.multiplication.of((FloatInput) ultgain, (FloatInput) pconstant), ultgain),
                EventMixing.filterEvent(calibrating, true, FloatMixing.onUpdate(ultgain)));
        FloatInput i = Mixing.select(calibrating, FloatMixing.division.of(
                FloatMixing.multiplication.of(p, (FloatInput) iconstant), (FloatInput) period), FloatMixing.always(0));
        FloatInput d = Mixing.select(calibrating, FloatMixing.multiplication.of(
                FloatMixing.multiplication.of(p, (FloatInput) dconstant), (FloatInput) period), FloatMixing.always(0));

        PIDControl pid = new PIDControl(HeadingSensor.absoluteYaw, desiredAngle, p, i, d);
        pid.setOutputBounds(-1f, 1f);
        pid.setIntegralBounds(-.5f, .5f);
        Igneous.duringAuto.send(pid);
        autoPID = FloatMixing.createDispatch(Mixing.select((BooleanInputPoll) usePID, FloatMixing.always(0), pid), FloatMixing.onUpdate((FloatInput) pid));
        reversePID = FloatMixing.negate(autoPID);

        Cluck.publish("Auto PID Output", autoPID);

        mainModule.publishDefaultControls(true, true);
        mainModule.addMode(new AutonomousModeDrive());
        mainModule.addMode(new AutonomousModeToteContainer());
        mainModule.addMode(new AutonomousModeContainerTote());
//        mainModule.addMode(new AutonomousModeThreeTotes());
        mainModule.addMode(new AutonomousModeOneContainer());
        mainModule.addMode(new AutonomousModeCalibration());
        mainModule.loadSettings(mainModule.addNullMode("none", "I'm a sitting chicken!"));
        Igneous.registerAutonomous(mainModule);
    }
}
