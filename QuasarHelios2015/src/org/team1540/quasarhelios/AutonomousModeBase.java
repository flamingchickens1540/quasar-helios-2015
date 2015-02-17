package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;

public abstract class AutonomousModeBase extends InstinctModeModule {
    private static final TuningContext context = ControlInterface.autoTuning;
    private static final FloatInputPoll driveSpeed = FloatMixing.negate((FloatInputPoll) context.getFloat("Auto Drive Speed +A", 1.0f));
    private static final FloatInputPoll rotateSpeed = FloatMixing.negate((FloatInputPoll) context.getFloat("Auto Rotate Speed +A", 0.5f));
    private static final FloatInputPoll rotateMultiplier = context.getFloat("Auto Rotate Multiplier +A", 1.0f);
    private static final FloatInputPoll rotateOffset = context.getFloat("Auto Rotate Offset +A", -30f);
    public static final float STRAFE_RIGHT = 1.0f;
    public static final float STRAFE_LEFT = -1.0f;

    protected final BooleanStatus straightening = new BooleanStatus();

    public AutonomousModeBase(String modeName) {
        super(modeName);
    }

    protected void drive(float distance) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        Autonomous.desiredAngle.set(HeadingSensor.absoluteYaw.get());

        float startingEncoder = DriveCode.leftEncoder.get();

        FloatInput rightMotorSpeed, leftMotorSpeed;

        if (distance > 0) {
            rightMotorSpeed = FloatMixing.addition.of(driveSpeed, Autonomous.autoPID);
            leftMotorSpeed = FloatMixing.addition.of(driveSpeed, Autonomous.reversePID);
        } else {
            rightMotorSpeed = FloatMixing.negate(FloatMixing.addition.of(driveSpeed, Autonomous.autoPID));
            leftMotorSpeed = FloatMixing.negate(FloatMixing.addition.of(driveSpeed, Autonomous.reversePID));
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
            straightening.set(true);
        }
    }

    protected void strafe(float direction, float time) throws InterruptedException, AutonomousModeOverException {
        straightening.set(false);
        DriveCode.strafe.set(direction);
        waitForTime((long) time);
        DriveCode.strafe.set(0.0f);
    }

    protected void turn(float degree) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        DriveCode.octocanumShifting.set(true);
        float startingYaw = HeadingSensor.absoluteYaw.get();

        if (degree > 0) {
            float actualDegree = degree * rotateMultiplier.get() + rotateOffset.get();
            if (actualDegree > 0) {
                DriveCode.rotate.set(-rotateSpeed.get());
                waitUntilAtMost(HeadingSensor.absoluteYaw, startingYaw - actualDegree);
            }
        } else {
            float actualDegree = degree * rotateMultiplier.get() - rotateOffset.get();
            if (actualDegree < 0) {
                DriveCode.rotate.set(rotateSpeed.get());
                waitUntilAtLeast(HeadingSensor.absoluteYaw, startingYaw - actualDegree);
            }
        }

        DriveCode.rotate.set(0.0f);
        DriveCode.octocanumShifting.set(false);
    }

    protected void collectTote() throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        QuasarHelios.autoLoader.set(true);
        waitUntilNot(QuasarHelios.autoLoader);
    }

    protected void setClampOpen(boolean value) throws InterruptedException, AutonomousModeOverException {
        Clamp.openControl.set(value);
        waitForTime(30);
    }

    protected void setClampHeight(float value) throws AutonomousModeOverException, InterruptedException {
        Clamp.mode.set(Clamp.MODE_HEIGHT);
        Clamp.height.set(value);
        waitUntil(FloatMixing.floatIsInRange(Clamp.heightReadout, value - Clamp.heightPadding.get(), value + Clamp.heightPadding.get()));
    }

    protected void ejectTotes() throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        QuasarHelios.autoEjector.set(true);
        waitUntilNot(QuasarHelios.autoEjector);
    }

    protected void pickupContainer(float nudge) throws AutonomousModeOverException, InterruptedException {
        setClampHeight(0.0f);
        setClampOpen(true);
        drive(nudge);
        setClampOpen(false);
        setClampHeight(1.0f);
    }

    protected void depositContainer() throws AutonomousModeOverException, InterruptedException {
        setClampHeight(0.0f);
        setClampOpen(true);
        setClampHeight(1.0f);
    }

    @Override
    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
            FloatMixing.pumpWhen(EventMixing.filterEvent(straightening, true, FloatMixing.onUpdate(Autonomous.autoPID)), Autonomous.autoPID, DriveCode.leftMotors);
            FloatMixing.pumpWhen(EventMixing.filterEvent(straightening, true, FloatMixing.onUpdate(Autonomous.reversePID)), Autonomous.reversePID, DriveCode.rightMotors);
            runAutonomous();
        } finally {
            straightening.set(false);
            DriveCode.allMotors.set(0);
        }
    }

    protected abstract void runAutonomous() throws InterruptedException, AutonomousModeOverException;
}
