package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatStatus;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.ctrl.PIDControl;
import ccre.holders.TuningContext;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;

public abstract class AutonomousModeBase extends InstinctModeModule {
    private static final TuningContext context = ControlInterface.autoTuning;
    private static final FloatInputPoll driveSpeed = context.getFloat("auto-drive-speed", 1.0f);
    private static final FloatInputPoll rotateSpeed = context.getFloat("auto-rotate-speed", 1.0f);
    private static final FloatInputPoll clampHeightPadding = context.getFloat("auto-clamp-height-padding", 0.01f);

    public AutonomousModeBase(String modeName) {
        super(modeName);
    }

    protected void drive(float distance) throws AutonomousModeOverException, InterruptedException {
        float desiredAngle = HeadingSensor.absoluteYaw.get();
        FloatStatus ultgain = ControlInterface.mainTuning.getFloat("drive-PID-ultimate-gain", .0162f);
        FloatStatus period = ControlInterface.mainTuning.getFloat("drive-PID-oscillation-period", 2f);
        FloatStatus pconstant = ControlInterface.mainTuning.getFloat("drive-PID-P-constant", .6f);
        FloatStatus iconstant = ControlInterface.mainTuning.getFloat("drive-PID-I-constant", 2f);
        FloatStatus dconstant = ControlInterface.mainTuning.getFloat("drive-PID-D-constant", .125f);
        BooleanStatus calibrating = ControlInterface.mainTuning.getBoolean("calibrating-drive-PID", false);

        FloatInput p = Mixing.select(calibrating, FloatMixing.multiplication.of((FloatInput) ultgain, (FloatInput) pconstant), ultgain);
        FloatInput i = Mixing.select(calibrating, FloatMixing.division.of(
                FloatMixing.multiplication.of(p, (FloatInput) iconstant), (FloatInput) period), FloatMixing.always(0));
        FloatInput d = Mixing.select(calibrating, FloatMixing.multiplication.of(
                FloatMixing.multiplication.of(p, (FloatInput) dconstant), (FloatInput) period), FloatMixing.always(0));

        PIDControl pid = new PIDControl(HeadingSensor.absoluteYaw, new FloatStatus(desiredAngle), p, i, d);
        pid.setOutputBounds(-1f, 1f);
        pid.setIntegralBounds(-.5f, .5f);

        Igneous.globalPeriodic.send(pid);

        float startingEncoder = DriveCode.leftEncoder.get();

        FloatInput rightMotorSpeed, leftMotorSpeed;

        if (distance > 0) {
            rightMotorSpeed = FloatMixing.addition.of(driveSpeed, pid);
            leftMotorSpeed = FloatMixing.subtraction.of(driveSpeed, pid);
        } else {
            rightMotorSpeed = FloatMixing.negate(FloatMixing.addition.of(driveSpeed, pid));
            leftMotorSpeed = FloatMixing.negate(FloatMixing.subtraction.of(driveSpeed, pid));
        }

        try {
            rightMotorSpeed.send(DriveCode.rightMotors);
            leftMotorSpeed.send(DriveCode.leftMotors);

            if (distance > 0) {
                waitUntilAtLeast(DriveCode.leftEncoder, startingEncoder + distance);
            } else {
                waitUntilAtMost(DriveCode.leftEncoder, startingEncoder + distance);
            }
        } finally {
            rightMotorSpeed.unsend(DriveCode.rightMotors);
            leftMotorSpeed.unsend(DriveCode.leftMotors);
            DriveCode.allMotors.set(0.0f);
        }
    }

    protected void strafe(float direction, float time) throws InterruptedException, AutonomousModeOverException {
        DriveCode.strafe.set(direction);
        waitForTime((long) time);
        DriveCode.strafe.set(0.0f);
    }

    protected void turn(float degree) throws AutonomousModeOverException, InterruptedException {
        float startingYaw = HeadingSensor.absoluteYaw.get();

        if (degree > 0) {
            DriveCode.rotate.set(rotateSpeed.get());
            waitUntilAtLeast(HeadingSensor.yaw, startingYaw + degree);
        } else {
            DriveCode.rotate.set(-rotateSpeed.get());
            waitUntilAtMost(HeadingSensor.yaw, startingYaw + degree);
        }

        DriveCode.rotate.set(0.0f);
    }

    protected void collectTote() throws AutonomousModeOverException, InterruptedException {
        QuasarHelios.autoLoader.set(true);
        waitUntil(BooleanMixing.invert((BooleanInput) QuasarHelios.autoLoader));
    }

    protected void setClampOpen(boolean value) throws InterruptedException, AutonomousModeOverException {
        Clamp.openControl.set(value);
        waitForTime(30);
    }

    protected void setClampHeight(float value) throws AutonomousModeOverException, InterruptedException {
        Clamp.mode.set(Clamp.MODE_HEIGHT);
        Clamp.height.set(value);
        waitUntil(FloatMixing.floatIsInRange(Clamp.heightReadout, value - clampHeightPadding.get(), value + clampHeightPadding.get()));
    }

    protected void ejectTotes() throws AutonomousModeOverException, InterruptedException {
        QuasarHelios.autoEjector.set(true);
        waitUntil(BooleanMixing.invert((BooleanInput) QuasarHelios.autoLoader));
    }

    @Override
    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
            runAutonomous();
        } finally {
            DriveCode.allMotors.set(0);
        }
    }

    protected abstract void runAutonomous() throws InterruptedException, AutonomousModeOverException;
}
