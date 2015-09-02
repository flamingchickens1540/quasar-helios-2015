package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.FloatInput;
import ccre.cluck.Cluck;
import ccre.ctrl.IJoystick;
import ccre.ctrl.PauseTimer;
import ccre.holders.TuningContext;
import ccre.frc.FRC;

public class ControlInterface {
    public static TuningContext mainTuning = new TuningContext("Main").publishSavingEvent();
    public static TuningContext autoTuning = new TuningContext("Autonomous").publishSavingEvent();
    public static TuningContext teleTuning = new TuningContext("Teleoperated").publishSavingEvent();
    private static BooleanInput rollersModeForClampControlDisable;

    public static void setup() {
        setupDrive();
        setupElevator();
        setupRollers();
        setupClamp();// needs to be after setupRollers()
    }

    private static void setupClamp() {
        PauseTimer disableMotion = new PauseTimer(2000);

        disableMotion.toFloat(FRC.joystick2.axis(2), 0).send(Clamp.speed.filterNot(rollersModeForClampControlDisable));

        EventInput toggle = FRC.joystick2.onPress(3);

        Clamp.open.toggleWhen(toggle);

        // When clamp is changed to closed, prevent motion for two seconds.
        // TODO: CHECK IF THIS STILL WORKS WITH THE NEW SYSTEM
        toggle.andNot(Clamp.open).send(disableMotion);

        Cluck.publish("Auto Stack", QuasarHelios.autoStacker);
    }

    private static void setupRollers() {
        BooleanStatus rollersMode = new BooleanStatus();
        rollersModeForClampControlDisable = rollersMode;
        rollersMode.toggleWhen(FRC.joystick2.onPress(5));
        QuasarHelios.publishFault("rollers-overridden", rollersMode.asInput(), rollersMode.getSetFalseEvent());

        EventInput povUp = FRC.joystick2.onPressPOV(IJoystick.POV_NORTH);
        EventInput povDown = FRC.joystick2.onPressPOV(IJoystick.POV_SOUTH);
        EventInput povLeft = FRC.joystick2.onPressPOV(IJoystick.POV_WEST);
        EventInput povRight = FRC.joystick2.onPressPOV(IJoystick.POV_EAST);

        povDown.send(() -> {
            if (Rollers.direction.get() && Rollers.running.get()) {
                Rollers.running.set(false);
            } else {
                Rollers.running.set(!Rollers.running.get());
            }

            Rollers.direction.set(Rollers.INPUT);

            rollersMode.set(false);
        });

        povUp.send(() -> {
            if (!Rollers.direction.get() && Rollers.running.get()) {
                Rollers.running.set(false);
            } else {
                Rollers.running.set(!Rollers.running.get());
            }

            Rollers.direction.set(Rollers.OUTPUT);

            rollersMode.set(false);
        });

        EventInput toggleClosed = povLeft.or(povRight).or(FRC.joystick1.onPress(5));
        toggleClosed.send(rollersMode.getSetFalseEvent());
        Rollers.closed.toggleWhen(toggleClosed);

        FloatInput cutoffRollers = mainTuning.getFloat("Roller Override Threshold +M", 0.8f);

        rollersMode.send(Rollers.overrideRollers);

        FRC.joystick2.axis(2).deadzone(0.2f).send(Rollers.leftRollerOverride);
        FRC.joystick2.axis(6).deadzone(0.2f).send(Rollers.rightRollerOverride);

        FloatInput cutoffAuto = mainTuning.getFloat("Trigger Threshold +M", 0.5f);

        FloatInput leftTrigger = FRC.joystick2.axis(3);
        FloatInput rightTrigger = FRC.joystick2.axis(4);

        BooleanInput leftTriggerPressForRollers = rollersMode.and(leftTrigger.atLeast(cutoffRollers));
        BooleanInput rightTriggerPressForRollers = rollersMode.and(rightTrigger.atLeast(cutoffRollers));
        BooleanInput leftTriggerPressForLoader = rollersMode.not().and(leftTrigger.atLeast(cutoffAuto));
        BooleanInput rightTriggerPressForLoader = rollersMode.not().and(rightTrigger.atLeast(cutoffAuto));

        leftTriggerPressForRollers.send(Rollers.leftPneumaticOverride.filter(QuasarHelios.isInManualControl));
        rightTriggerPressForRollers.send(Rollers.rightPneumaticOverride.filter(QuasarHelios.isInManualControl));

        leftTriggerPressForLoader.send(QuasarHelios.autoEjector);
        rightTriggerPressForLoader.send(QuasarHelios.autoLoader);

        BooleanInput holding = FRC.joystick2.button(7).and(QuasarHelios.isInManualControl);
        Rollers.startHoldIn.on(holding.onPress());
        rollersMode.setFalseWhen(holding.onPress());
        Rollers.stopHoldIn.on(holding.onRelease());

        BooleanInput spinning = FRC.joystick2.button(8).and(QuasarHelios.isInManualControl);
        Rollers.startSpin.on(spinning.onPress());
        rollersMode.setFalseWhen(spinning.onPress());
        Rollers.stopSpin.on(spinning.onRelease());
    }

    private static void setupElevator() {
        FRC.joystick2.onPress(4).send(Elevator.setTop);
        FRC.joystick2.onPress(1).send(Elevator.setBottom);
        QuasarHelios.autoHumanLoader.toggleWhen(FRC.joystick2.onPress(2));

        FRC.joystick2.onPress(6).send(Elevator.stop);
        FRC.joystick2.button(6).send(Elevator.overrideEnabled);
        FRC.joystick2.axis(6).send(Elevator.overrideValue.negate());
    }

    private static void setupDrive() {
        DriveCode.shiftEnabled.setTrueWhen(FRC.joystick1.onPress(5));
        DriveCode.shiftEnabled.setFalseWhen(FRC.joystick1.onPress(6));

        FRC.joystick1.axisX().deadzone(.2f).send(DriveCode.leftJoystickXRaw);
        FRC.joystick1.axisY().deadzone(.2f).send(DriveCode.leftJoystickYRaw);
        FRC.joystick1.axis(5).deadzone(.25f).send(DriveCode.rightJoystickXRaw);
        FRC.joystick1.axis(6).deadzone(.2f).send(DriveCode.rightJoystickYRaw);
        FRC.joystick1.axis(3).deadzone(.1f).send(DriveCode.backwardTrigger);
        FRC.joystick1.axis(4).deadzone(.1f).send(DriveCode.forwardTrigger);

        FRC.joystick1.onPress(1).send(ContainerGrabber.containerGrabButton);// only here because this is where we work with the drive joystick
        FRC.joystick1.onPress(2).send(DriveCode.recalibrateButton);
        FRC.joystick1.onPress(4).send(DriveCode.strafingButton);
    }
}
