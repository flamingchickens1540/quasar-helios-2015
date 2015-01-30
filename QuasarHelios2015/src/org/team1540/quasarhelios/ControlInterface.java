package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.ctrl.FloatMixing;
import ccre.holders.TuningContext;

public class ControlInterface {
    public static TuningContext mainTuning = new TuningContext("Main").publishSavingEvent();
    public static TuningContext autoTuning = new TuningContext("Autonomous").publishSavingEvent();
    public static TuningContext teleTuning = new TuningContext("Teleoperated").publishSavingEvent();

    public static void setup() {
        setupJoysticks();
        setupCluck();
    }

    public static void setupJoysticks() {
        FloatInput leftXAxis = Igneous.joystick1.getXAxisSource();
        FloatInput rightXAxis = Igneous.joystick1.getAxisSource(5);
        FloatInput leftYAxis = Igneous.joystick1.getYAxisSource();
        FloatInput rightYAxis = Igneous.joystick1.getAxisSource(6);

        Cluck.publish("Right Joystick X", rightXAxis);
        Cluck.publish("Right Joystick Y", rightYAxis);
        Cluck.publish("Left Joystick X", leftXAxis);
        Cluck.publish("Left Joystick Y", leftYAxis);

        FloatMixing.deadzone(leftXAxis, .2f).send(DriveCode.leftJoystickX);
        FloatMixing.deadzone(leftYAxis, .2f).send(DriveCode.leftJoystickY);
        FloatMixing.deadzone(rightXAxis, .25f).send(DriveCode.rightJoystickX);
        FloatMixing.deadzone(rightYAxis, .2f).send(DriveCode.rightJoystickY);
        DriveCode.octocanumShiftingButton = Igneous.joystick1.getButtonSource(1);
        DriveCode.recalibrateButton = Igneous.joystick1.getButtonSource(2);

        Elevator.goTopInput = Igneous.joystick2.getButtonSource(1);
        Elevator.goBottomInput = Igneous.joystick2.getButtonSource(2);
        Elevator.goMiddleInput = Igneous.joystick2.getButtonSource(3);
        Elevator.stopInput = Igneous.joystick2.getButtonSource(4);
        Rollers.runRollersButton = Igneous.joystick2.getButtonSource(5);
        Rollers.toggleRollersButton = Igneous.joystick2.getButtonSource(6);
        Rollers.toggleOpenButton = Igneous.joystick2.getButtonSource(7);
        Clamp.heightInput = Igneous.joystick2.getAxisSource(1);
        Clamp.openInput = Igneous.joystick2.getButtonSource(8);
        
        QuasarHelios.autoLoader.toggleWhen(Igneous.joystick2.getButtonSource(1)); // TODO: Pick buttons
        QuasarHelios.autoEjector.toggleWhen(Igneous.joystick2.getButtonSource(2)); // TODO: Pick buttons
    }

    public static void setupCluck() {

    }
}
