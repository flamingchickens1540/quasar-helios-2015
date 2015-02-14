package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatStatus;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.ctrl.PIDControl;
import ccre.igneous.Igneous;
import ccre.instinct.InstinctMultiModule;

public class Autonomous {
    public static InstinctMultiModule mainModule = new InstinctMultiModule(ControlInterface.autoTuning);
    public static FloatInput PIDValue;
    public static final FloatStatus desiredAngle = new FloatStatus();

    public static void setup() {
        FloatStatus ultgain = ControlInterface.autoTuning.getFloat("auto-PID-ultimate-gain", .0162f);
        FloatStatus period = ControlInterface.autoTuning.getFloat("auto-PID-oscillation-period", 2f);
        FloatStatus pconstant = ControlInterface.autoTuning.getFloat("auto-PID-P-constant", .6f);
        FloatStatus iconstant = ControlInterface.autoTuning.getFloat("auto-PID-I-constant", 2f);
        FloatStatus dconstant = ControlInterface.autoTuning.getFloat("auto-PID-D-constant", .125f);
        BooleanStatus calibrating = ControlInterface.autoTuning.getBoolean("calibrating-auto-PID", false);
        BooleanStatus usePID = ControlInterface.autoTuning.getBoolean("use-PID-in-auto", false);

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
        PIDValue = FloatMixing.createDispatch(Mixing.select(usePID, FloatMixing.always(0), pid), FloatMixing.onUpdate((FloatInput) pid));

        mainModule.publishDefaultControls(true, true);
        mainModule.addMode(new AutonomousModeDrive());
        mainModule.addMode(new AutonomousModeOneTote());
        mainModule.addMode(new AutonomousModeThreeTotes());
        mainModule.loadSettings(mainModule.addNullMode("none", "I'm a sitting chicken!"));
        Igneous.registerAutonomous(mainModule);
    }
}
