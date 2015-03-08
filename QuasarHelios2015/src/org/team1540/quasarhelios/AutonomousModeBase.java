package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.FloatMixing;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModeModule;
import ccre.util.Utils;

public abstract class AutonomousModeBase extends InstinctModeModule {
    private static final TuningContext context = ControlInterface.autoTuning;
    private static final FloatInputPoll driveSpeed = context.getFloat("Auto Drive Speed +A", 0.5f);
    private static final FloatInputPoll rotateSpeed = FloatMixing.negate((FloatInputPoll) context.getFloat("Auto Rotate Speed +A", 0.5f));
    private static final FloatInputPoll rotateMultiplier = context.getFloat("Auto Rotate Multiplier +A", 1.0f);
    private static final FloatInputPoll rotateOffset = context.getFloat("Auto Rotate Offset +A", -30f);
    public static final float STRAFE_RIGHT = 1.0f;
    public static final float STRAFE_LEFT = -1.0f;

    protected final BooleanStatus straightening = new BooleanStatus();

    public AutonomousModeBase(String modeName) {
        super(modeName);
    }

    protected void driveForTime(long time, float speed) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        Autonomous.desiredAngle.set(HeadingSensor.absoluteYaw.get());

        FloatInput rightMotorSpeed, leftMotorSpeed;

        if (time > 0) {
            rightMotorSpeed = FloatMixing.addition.of(-speed, Autonomous.autoPID);
            leftMotorSpeed = FloatMixing.addition.of(-speed, Autonomous.reversePID);
        } else {
            rightMotorSpeed = FloatMixing.negate(FloatMixing.addition.of(-speed, Autonomous.autoPID));
            leftMotorSpeed = FloatMixing.negate(FloatMixing.addition.of(-speed, Autonomous.reversePID));
        }

        try {
            rightMotorSpeed.send(DriveCode.rightMotors);
            leftMotorSpeed.send(DriveCode.leftMotors);

            waitForTime(Math.abs(time));
        } finally {
            rightMotorSpeed.unsend(DriveCode.rightMotors);
            leftMotorSpeed.unsend(DriveCode.leftMotors);
            DriveCode.allMotors.set(0.0f);
            straightening.set(true);
        }
    }

    protected void drive(float distance) throws AutonomousModeOverException, InterruptedException {
        drive(distance, driveSpeed.get());
    }

    protected void drive(float distance, float speed) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        Autonomous.desiredAngle.set(HeadingSensor.absoluteYaw.get());

        float startingEncoder = DriveCode.leftEncoder.get();

        FloatInput rightMotorSpeed, leftMotorSpeed;

        if (distance > 0) {
            rightMotorSpeed = FloatMixing.addition.of(-speed, Autonomous.reversePID);
            leftMotorSpeed = FloatMixing.addition.of(-speed, Autonomous.autoPID);
        } else {
            rightMotorSpeed = FloatMixing.negate(FloatMixing.addition.of(-speed, Autonomous.reversePID));
            leftMotorSpeed = FloatMixing.negate(FloatMixing.addition.of(-speed, Autonomous.autoPID));
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

    protected void turnAbsolute(float start, float degree, boolean adjustAngle) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        DriveCode.octocanumShifting.set(true);

        if (degree > 0) {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() + rotateOffset.get() : degree;
            if (actualDegree > 0) {
                DriveCode.rotate.set(-rotateSpeed.get());
                waitUntilAtMost(HeadingSensor.absoluteYaw, start - actualDegree);
            }
        } else {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() - rotateOffset.get() : degree;

            if (actualDegree < 0) {
                DriveCode.rotate.set(rotateSpeed.get());
                waitUntilAtLeast(HeadingSensor.absoluteYaw, start - actualDegree);
            }
        }

        DriveCode.rotate.set(0.0f);
    }

    protected void turn(float degree, boolean adjustAngle) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        DriveCode.octocanumShifting.set(true);
        float startingYaw = HeadingSensor.absoluteYaw.get();

        if (degree > 0) {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() + rotateOffset.get() : degree;
            if (actualDegree > 0) {
                DriveCode.rotate.set(-rotateSpeed.get());
                waitUntilAtMost(HeadingSensor.absoluteYaw, startingYaw - actualDegree);
            }
        } else {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() - rotateOffset.get() : degree;

            if (actualDegree < 0) {
                DriveCode.rotate.set(rotateSpeed.get());
                waitUntilAtLeast(HeadingSensor.absoluteYaw, startingYaw - actualDegree);
            }
        }

        DriveCode.rotate.set(0.0f);
    }
    protected void turn(float degree, boolean adjustAngle, float speed) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        DriveCode.octocanumShifting.set(true);
        float startingYaw = HeadingSensor.absoluteYaw.get();

        if (degree > 0) {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() + rotateOffset.get() : degree;
            if (actualDegree > 0) {
                DriveCode.rotate.set(speed);
                waitUntilAtMost(HeadingSensor.absoluteYaw, startingYaw - actualDegree);
            }
        } else {
            float actualDegree = adjustAngle ? degree * rotateMultiplier.get() - rotateOffset.get() : degree;

            if (actualDegree < 0) {
                DriveCode.rotate.set(-speed);
                waitUntilAtLeast(HeadingSensor.absoluteYaw, startingYaw - actualDegree);
            }
        }

        DriveCode.rotate.set(0.0f);
    }


    protected void singleSideTurn(long time, boolean side) throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        DriveCode.octocanumShifting.set(true);

        FloatOutput motors = side ? DriveCode.leftMotors : DriveCode.rightMotors;
        // TODO: Maybe this should use encoders?
        motors.set(rotateSpeed.get());
        waitForTime(time);
        motors.set(0.0f);
    }

    protected void collectTote() throws AutonomousModeOverException, InterruptedException {
        collectTote(false, 0);
    }

    protected void collectTote(boolean shake, int timeout) throws AutonomousModeOverException, InterruptedException {
        // Move elevator.
        Elevator.setTop.event();

        // Move clamp.
        float currentClampHeight = Clamp.heightReadout.get();
        if (currentClampHeight < AutoLoader.clampHeightThreshold.get()) {
            Clamp.mode.set(Clamp.MODE_HEIGHT);
            Clamp.height.set(AutoLoader.clampHeightThreshold.get());
            waitUntil(BooleanMixing.andBooleans(Clamp.atDesiredHeight, Elevator.atTop));
        } else {
            waitUntil(Elevator.atTop);
        }

        // Run rollers.
        Rollers.direction.set(Rollers.INPUT);
        Rollers.running.set(true);
        Rollers.closed.set(true);

        if (shake) {
            while (true) {
                DriveCode.rotate.set(0.5f);
                waitForTime(100);
                if (AutoLoader.crateInPosition.get()) {
                    break;
                }
                DriveCode.rotate.set(-0.5f);
                waitForTime(100);
                if (AutoLoader.crateInPosition.get()) {
                    break;
                }
            }
            Rollers.running.set(false);
            Rollers.closed.set(false);
        } else if (timeout > 0) {
            float end = Utils.getCurrentTimeSeconds() + timeout / 1000f;
            waitUntil(BooleanMixing.orBooleans(AutoLoader.crateInPosition, FloatMixing.floatIsAtLeast(Utils.currentTimeSeconds, end)));
            Rollers.running.set(false);
            Rollers.closed.set(false);
        } else if (timeout == 0) {
            waitUntil(AutoLoader.crateInPosition);
            Rollers.running.set(false);
            Rollers.closed.set(false);
        } else {
            waitForTime(250);
        }
    }
    
    protected void collectToteWithElevator() throws AutonomousModeOverException, InterruptedException {
        QuasarHelios.autoLoader.set(true);
        waitUntilNot(QuasarHelios.autoLoader);
    }

    protected void setClampOpen(boolean value) throws InterruptedException, AutonomousModeOverException {
        Clamp.open.set(value);
        waitForTime(30);
    }

    protected void startSetClampHeight(float value) {
        Clamp.mode.set(Clamp.MODE_HEIGHT);
        Clamp.height.set(value);
    }

    protected void setClampHeight(float value) throws AutonomousModeOverException, InterruptedException {
        Clamp.mode.set(Clamp.MODE_HEIGHT);
        Clamp.height.set(value);
        waitUntil(Clamp.atDesiredHeight);
    }

    protected void ejectTotes() throws AutonomousModeOverException, InterruptedException {
        straightening.set(false);
        QuasarHelios.autoEjector.set(true);
        waitUntilNot(QuasarHelios.autoEjector);
    }

    protected void pickupContainer(float nudge) throws AutonomousModeOverException, InterruptedException {
        setClampOpen(true);
        drive(nudge);
        setClampOpen(false);
        setClampHeight(0.5f);
    }

    protected void depositContainer(float height) throws AutonomousModeOverException, InterruptedException {
        setClampHeight(height);
        waitForTime(100);
        setClampOpen(true);
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
