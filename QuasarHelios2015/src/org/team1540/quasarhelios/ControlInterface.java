package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.channel.BooleanInputPoll;
import ccre.channel.EventInput;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput; 
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.holders.TuningContext;

public class ControlInterface {
    public static TuningContext mainTuning = new TuningContext("Main").publishSavingEvent();
    public static TuningContext autoTuning = new TuningContext("Autonomous").publishSavingEvent();
    public static TuningContext teleTuning = new TuningContext("Teleoperated").publishSavingEvent();

    public static void setup() {
        setupDrive();
        setupClamp();
        setupElevator();
        setupRollers();
    }

    private static void setupClamp() {
        FloatMixing.pumpWhen(EventMixing.filterEvent(Igneous.joystick2.getButtonChannel(5), false, QuasarHelios.globalControl), Igneous.joystick2.getAxisChannel(2), Clamp.speed);
        Clamp.openControl.toggleWhen(Igneous.joystick2.getButtonSource(3));
    }

    private static void setupRollers() {
        EventInput povPressed = BooleanMixing.onPress(Igneous.joystick2.isPOVPressedSource(1));
        FloatInputPoll povAngle = Igneous.joystick2.getPOVAngle(1);
        EventInput povUp = EventMixing.filterEvent(FloatMixing.floatIsInRange(povAngle, -0.1f, 0.1f), true, povPressed);
        EventInput povDown = EventMixing.filterEvent(FloatMixing.floatIsInRange(povAngle, 179.9f, 180.1f), true, povPressed);
        EventInput povLeft = EventMixing.filterEvent(FloatMixing.floatIsInRange(povAngle, 269.9f, 270.1f), true, povPressed);
        EventInput povRight = EventMixing.filterEvent(FloatMixing.floatIsInRange(povAngle, 89.9f, 90.1f), true, povPressed);

        povUp.send(() -> {
            if (Rollers.direction.get()) {
                Rollers.running.set(true);
            } else {
                Rollers.running.set(!Rollers.running.get());
            }

            Rollers.direction.set(false);
        });

        povDown.send(() -> {
            if (!Rollers.direction.get()) {
                Rollers.running.set(true);
            } else {
                Rollers.running.set(!Rollers.running.get());
            }

            Rollers.direction.set(true);
        });

        Rollers.open.toggleWhen(EventMixing.combine(povLeft, povRight));

        FloatInput cutoffRollers = mainTuning.getFloat("roller-override-threshold", 0.3f);
        BooleanInputPoll overrideRollers = Igneous.joystick2.getButtonChannel(5);

        BooleanMixing.pumpWhen(Igneous.globalPeriodic, overrideRollers, Rollers.overrideRollers);

        BooleanMixing.pumpWhen(QuasarHelios.globalControl, BooleanMixing.andBooleans(overrideRollers, FloatMixing.floatIsAtMost(
                Igneous.joystick2.getAxisChannel(1), cutoffRollers)), Rollers.leftPneumaticOverride);

        BooleanMixing.pumpWhen(QuasarHelios.globalControl, BooleanMixing.andBooleans(overrideRollers, FloatMixing.floatIsAtLeast(
                Igneous.joystick2.getAxisChannel(5), FloatMixing.negate(cutoffRollers))), Rollers.rightPneumaticOverride);

        Igneous.joystick2.getAxisSource(2).send(Rollers.leftRollerOverride);
        Igneous.joystick2.getAxisSource(6).send(Rollers.rightRollerOverride);

        FloatInput cutoffAuto = mainTuning.getFloat("autoloader-button-threshold", 0.5f);

        BooleanMixing.pumpWhen(QuasarHelios.globalControl, FloatMixing.floatIsAtLeast(Igneous.joystick2.getAxisSource(3), cutoffAuto), QuasarHelios.autoEjector);
        BooleanMixing.pumpWhen(QuasarHelios.globalControl, FloatMixing.floatIsAtLeast(Igneous.joystick2.getAxisSource(4), cutoffAuto), QuasarHelios.autoLoader);
    }

    private static void setupElevator() {
        Igneous.joystick2.getButtonSource(4).send(Elevator.setTop);
        Igneous.joystick2.getButtonSource(1).send(Elevator.setBottom);
        Igneous.joystick2.getButtonSource(2).send(Elevator.stop);

        BooleanMixing.pumpWhen(QuasarHelios.globalControl, Igneous.joystick2.getButtonChannel(6), Elevator.overrideEnabled);
        Igneous.joystick2.getAxisSource(6).send(FloatMixing.negate((FloatOutput) Elevator.overrideValue));
    }

    private static void setupDrive() {
        FloatInput leftXAxis = Igneous.joystick1.getXAxisSource();
        FloatInput leftYAxis = Igneous.joystick1.getYAxisSource();
        FloatInput rightXAxis = Igneous.joystick1.getAxisSource(5);
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
    }
}
