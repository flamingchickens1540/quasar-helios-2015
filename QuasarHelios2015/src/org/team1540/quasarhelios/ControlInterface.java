package org.team1540.quasarhelios;

import ccre.igneous.Igneous;
import ccre.channel.BooleanInput;
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
        FloatMixing.pumpWhen(EventMixing.filterEvent(Igneous.joystick2.getButtonChannel(5), false, QuasarHelios.globalControl),
                Igneous.joystick2.getAxisChannel(2), Clamp.speed);

        Clamp.open.toggleWhen(Igneous.joystick2.getButtonSource(3));
        Igneous.joystick2.getButtonSource(7).send(Clamp.setBottom);
        Igneous.joystick2.getButtonSource(8).send(QuasarHelios.autoStacker.getSetTrueEvent());
        Cluck.publish("Auto Stack", QuasarHelios.autoStacker);
    }

    private static void setupRollers() {
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

        Rollers.closed.toggleWhen(EventMixing.combine(povLeft, povRight));

        FloatInput cutoffRollers = mainTuning.getFloat("Roller Override Threshold +M", 0.3f);
        BooleanInput overrideRollers = BooleanMixing.createDispatch(Igneous.joystick2.getButtonChannel(5), Igneous.globalPeriodic);

        overrideRollers.send(Rollers.overrideRollers);

        FloatInput leftStickX = Igneous.joystick2.getAxisSource(1);
        FloatInput rightStickX = Igneous.joystick2.getAxisSource(5);

        BooleanMixing.onPress(FloatMixing.floatIsAtLeast(leftStickX, cutoffRollers)).send(Rollers.leftPneumaticOverride.getSetTrueEvent());
        BooleanMixing.onPress(FloatMixing.floatIsAtMost(leftStickX, FloatMixing.negate(cutoffRollers))).send(Rollers.leftPneumaticOverride.getSetFalseEvent());

        BooleanMixing.onPress(FloatMixing.floatIsAtMost(rightStickX, FloatMixing.negate(cutoffRollers))).send(Rollers.rightPneumaticOverride.getSetTrueEvent());
        BooleanMixing.onPress(FloatMixing.floatIsAtLeast(rightStickX, cutoffRollers)).send(Rollers.rightPneumaticOverride.getSetFalseEvent());

        FloatInput leftStickY = Igneous.joystick2.getAxisSource(2);
        FloatInput rightStickY = Igneous.joystick2.getAxisSource(6);
        FloatMixing.deadzone(leftStickY, 0.2f).send(Rollers.leftRollerOverride);
        FloatMixing.deadzone(rightStickY, 0.2f).send(Rollers.rightRollerOverride);

        FloatInput cutoffAuto = mainTuning.getFloat("Trigger Threshold +M", 0.5f);

        BooleanMixing.pumpWhen(QuasarHelios.manualControl, FloatMixing.floatIsAtLeast(Igneous.joystick2.getAxisSource(3), cutoffAuto), QuasarHelios.autoEjector);
        BooleanMixing.pumpWhen(QuasarHelios.manualControl, FloatMixing.floatIsAtLeast(Igneous.joystick2.getAxisSource(4), cutoffAuto), QuasarHelios.autoLoader);
    }

    private static void setupElevator() {
        Igneous.joystick2.getButtonSource(4).send(Elevator.setTop);
        Igneous.joystick2.getButtonSource(1).send(Elevator.setBottom);
        Igneous.joystick2.getButtonSource(2).send(Elevator.stop);

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

        Igneous.joystick1.getButtonSource(1).send(DriveCode.octocanumShiftingButton);
        Igneous.joystick1.getButtonSource(2).send(DriveCode.recalibrateButton);
        Igneous.joystick1.getButtonSource(4).send(DriveCode.strafingButton);
    }
}
