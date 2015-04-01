package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
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
    private static BooleanInput rollersModeForClampControlDisable;

    public static void setup() {
        setupDrive();
        setupElevator();
        setupRollers();
        setupClamp(); // needs to be after setupRollers()
    }

    private static void setupClamp() {
        FloatMixing.pumpWhen(EventMixing.filterEvent(rollersModeForClampControlDisable, false, QuasarHelios.globalControl),
                Igneous.joystick2.getAxisChannel(2), Clamp.speed);

        Clamp.open.toggleWhen(Igneous.joystick2.getButtonSource(3));
        Igneous.joystick2.getButtonSource(7).send(Clamp.setBottom);
        Igneous.joystick2.getButtonSource(8).send(QuasarHelios.autoStacker.getSetTrueEvent());
        Cluck.publish("Auto Stack", QuasarHelios.autoStacker);
    }

    private static void setupRollers() {
        BooleanStatus rollersMode = new BooleanStatus();
        rollersModeForClampControlDisable = rollersMode;
        rollersMode.toggleWhen(Igneous.joystick2.getButtonSource(5));
        QuasarHelios.publishFault("rollers-overridden", rollersMode.asInput(), rollersMode.getToggleEvent());

        EventInput povPressed = BooleanMixing.onPress(Igneous.joystick2.isPOVPressedSource(1));
        FloatInputPoll povAngle = Igneous.joystick2.getPOVAngle(1);
        EventInput povUp = EventMixing.filterEvent(FloatMixing.floatIsInRange(povAngle, -0.1f, 0.1f), true, povPressed);
        EventInput povDown = EventMixing.filterEvent(FloatMixing.floatIsInRange(povAngle, 179.9f, 180.1f), true, povPressed);
        EventInput povLeft = EventMixing.filterEvent(FloatMixing.floatIsInRange(povAngle, 269.9f, 270.1f), true, povPressed);
        EventInput povRight = EventMixing.filterEvent(FloatMixing.floatIsInRange(povAngle, 89.9f, 90.1f), true, povPressed);

        povDown.send(() -> {
            if (Rollers.direction.get() && Rollers.running.get()) {
                Rollers.running.set(false);
            } else {
                Rollers.running.set(!Rollers.running.get());
            }

            Rollers.direction.set(Rollers.INPUT);
        });

        povUp.send(() -> {
            if (!Rollers.direction.get() && Rollers.running.get()) {
                Rollers.running.set(false);
            } else {
                Rollers.running.set(!Rollers.running.get());
            }

            Rollers.direction.set(Rollers.OUTPUT);
        });

        Rollers.closed.toggleWhen(EventMixing.combine(povLeft, povRight, Igneous.joystick1.getButtonSource(5)));

        FloatInput cutoffRollers = mainTuning.getFloat("Roller Override Threshold +M", 0.8f);

        rollersMode.send(Rollers.overrideRollers);

        FloatInput leftStickY = Igneous.joystick2.getAxisSource(2);
        FloatInput rightStickY = Igneous.joystick2.getAxisSource(6);
        FloatMixing.deadzone(leftStickY, 0.2f).send(Rollers.leftRollerOverride);
        FloatMixing.deadzone(rightStickY, 0.2f).send(Rollers.rightRollerOverride);

        FloatInput cutoffAuto = mainTuning.getFloat("Trigger Threshold +M", 0.5f);

        FloatInput leftTrigger = Igneous.joystick2.getAxisSource(3);
        FloatInput rightTrigger = Igneous.joystick2.getAxisSource(4);

        BooleanInput leftTriggerPressForRollers = BooleanMixing.andBooleans(rollersMode, FloatMixing.floatIsAtLeast(leftTrigger, cutoffRollers));
        BooleanInput rightTriggerPressForRollers = BooleanMixing.andBooleans(rollersMode, FloatMixing.floatIsAtLeast(rightTrigger, cutoffRollers));
        BooleanInput leftTriggerPressForLoader = BooleanMixing.andBooleans(rollersMode.asInvertedInput(), FloatMixing.floatIsAtLeast(leftTrigger, cutoffAuto));
        BooleanInput rightTriggerPressForLoader = BooleanMixing.andBooleans(rollersMode.asInvertedInput(), FloatMixing.floatIsAtLeast(rightTrigger, cutoffAuto));

        BooleanMixing.pumpWhen(QuasarHelios.manualControl, leftTriggerPressForRollers, Rollers.leftPneumaticOverride);
        BooleanMixing.pumpWhen(QuasarHelios.manualControl, rightTriggerPressForRollers, Rollers.rightPneumaticOverride);

        leftTriggerPressForLoader.send(QuasarHelios.autoEjector);
        rightTriggerPressForLoader.send(QuasarHelios.autoLoader);
    }

    private static void setupElevator() {
        Igneous.joystick2.getButtonSource(4).send(Elevator.setTop);
        Igneous.joystick2.getButtonSource(1).send(Elevator.setBottom);
        QuasarHelios.autoHumanLoader.toggleWhen(Igneous.joystick2.getButtonSource(2));

        Igneous.joystick2.getButtonSource(6).send(Elevator.stop);
        BooleanMixing.pumpWhen(QuasarHelios.globalControl, Igneous.joystick2.getButtonChannel(6), Elevator.overrideEnabled);
        Igneous.joystick2.getAxisSource(6).send(FloatMixing.negate((FloatOutput) Elevator.overrideValue));
    }

    private static void setupDrive() {
        DriveCode.shiftEnabled.setTrueWhen(Igneous.joystick1.getButtonSource(5));
        DriveCode.shiftEnabled.setFalseWhen(Igneous.joystick1.getButtonSource(6));

        FloatMixing.deadzone(Igneous.joystick1.getXAxisSource(), .2f).send(DriveCode.leftJoystickXRaw);
        FloatMixing.deadzone(Igneous.joystick1.getYAxisSource(), .2f).send(DriveCode.leftJoystickYRaw);
        FloatMixing.deadzone(Igneous.joystick1.getAxisSource(5), .25f).send(DriveCode.rightJoystickXRaw);
        FloatMixing.deadzone(Igneous.joystick1.getAxisSource(6), .2f).send(DriveCode.rightJoystickYRaw);
        FloatMixing.deadzone(Igneous.joystick1.getAxisSource(3), .1f).send(DriveCode.backwardTrigger);
        FloatMixing.deadzone(Igneous.joystick1.getAxisSource(4), .1f).send(DriveCode.forwardTrigger);

        Igneous.joystick1.getButtonSource(1).send(ContainerGrabber.containerGrabButton); // only here because this is where we work with the drive joystick
        Igneous.joystick1.getButtonSource(2).send(DriveCode.recalibrateButton);
        Igneous.joystick1.getButtonSource(4).send(DriveCode.strafingButton);
    }
}
