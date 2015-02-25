package org.team1540.quasarhelios;

import ccre.channel.*;
import ccre.cluck.Cluck;
import ccre.ctrl.*;
import ccre.igneous.*;
import ccre.log.Logger;

public class DriveCode {
    public static final BooleanStatus shiftEnabled = new BooleanStatus();

    public static final FloatStatus leftJoystickXRaw = new FloatStatus();
    public static final FloatStatus leftJoystickYRaw = new FloatStatus();
    public static final FloatStatus rightJoystickXRaw = new FloatStatus();
    public static final FloatStatus rightJoystickYRaw = new FloatStatus();

    private static final FloatInput multiplier = Mixing.select(shiftEnabled, FloatMixing.always(1.0f), ControlInterface.teleTuning.getFloat("Drive Shift Multiplier +T", 0.5f));

    public static final FloatInput leftJoystickX = leftJoystickXRaw;
    public static final FloatInput leftJoystickY = FloatMixing.multiplication.of(multiplier, (FloatInput) leftJoystickYRaw);
    public static final FloatInput rightJoystickX = rightJoystickXRaw;
    public static final FloatInput rightJoystickY = FloatMixing.multiplication.of(multiplier, (FloatInput) rightJoystickYRaw);

    public static EventStatus octocanumShiftingButton = new EventStatus();
    public static EventStatus recalibrateButton = new EventStatus();
    public static EventStatus strafingButton = new EventStatus();
    public static FloatStatus forwardTrigger = new FloatStatus();
    public static FloatStatus backwardTrigger = new FloatStatus();

    public static final FloatInput leftJoystickYChannel = FloatMixing.subtraction.of(FloatMixing.addition.of((FloatInput) leftJoystickY, (FloatInput) forwardTrigger), (FloatInput) backwardTrigger);
    public static final FloatInput rightJoystickYChannel = FloatMixing.subtraction.of(FloatMixing.addition.of((FloatInput) rightJoystickY, (FloatInput) forwardTrigger), (FloatInput) backwardTrigger);

    private static final FloatOutput leftFrontMotor = Igneous.makeTalonMotor(9, Igneous.MOTOR_REVERSE, .1f);
    private static final FloatOutput leftBackMotor = Igneous.makeTalonMotor(8, Igneous.MOTOR_REVERSE, .1f);
    private static final FloatOutput rightFrontMotor = Igneous.makeTalonMotor(0, Igneous.MOTOR_FORWARD, .1f);
    private static final FloatOutput rightBackMotor = Igneous.makeTalonMotor(1, Igneous.MOTOR_FORWARD, .1f);
    public static final FloatOutput rightMotors = FloatMixing.combine(rightFrontMotor, rightBackMotor);
    public static final FloatOutput leftMotors = FloatMixing.combine(leftFrontMotor, leftBackMotor);
    public static final FloatOutput allMotors = FloatMixing.combine(leftMotors, rightMotors);
    public static final FloatOutput rotate = FloatMixing.combine(leftMotors, FloatMixing.negate(rightMotors));
    public static final FloatOutput strafe = FloatMixing.combine(
            FloatMixing.negate(FloatMixing.combine(leftFrontMotor, rightBackMotor)),
            FloatMixing.combine(leftBackMotor, rightFrontMotor));
    public static final BooleanStatus octocanumShifting = new BooleanStatus(Igneous.makeSolenoid(0));
    public static final BooleanStatus onlyStrafing = new BooleanStatus();
    private static final EventStatus resetEncoders = new EventStatus();
    public static final FloatInput leftEncoderRaw = FloatMixing.createDispatch(Igneous.makeEncoder(6, 7, Igneous.MOTOR_REVERSE, resetEncoders), Igneous.globalPeriodic);
    public static final FloatInput rightEncoderRaw = FloatMixing.createDispatch(Igneous.makeEncoder(8, 9, Igneous.MOTOR_FORWARD, resetEncoders), Igneous.globalPeriodic);
    public static final FloatInput encoderScaling = ControlInterface.mainTuning.getFloat("Drive Encoder Scaling +M", 0.1f);
    public static final FloatInput leftEncoder = FloatMixing.multiplication.of(leftEncoderRaw, encoderScaling);
    public static final FloatInput rightEncoder = FloatMixing.multiplication.of(rightEncoderRaw, encoderScaling);

    private static final double π = Math.PI;

    private static FloatStatus centricAngleOffset;
    private static BooleanStatus headingControl;
    private static final FloatStatus calibratedAngle = new FloatStatus();
    private static final BooleanStatus fieldCentric = ControlInterface.teleTuning.getBoolean("Teleop Field Centric Enabled +T", false);

    private static final FloatStatus desiredAngle = new FloatStatus();
    private static final BooleanInputPoll isDisabled = Igneous.getIsDisabled();
    private static PIDControl pid;

    private static EventOutput mecanum = new EventOutput() {
        public void event() {
            float distanceY = leftJoystickYChannel.get();
            float distanceX = leftJoystickX.get();
            float speed = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);
            if (speed > 1) {
                speed = 1;
            }
            float rotationspeed = rightJoystickX.get();
            double angle;
            if (distanceX == 0) {
                if (distanceY > 0) {
                    angle = π / 2;
                } else {
                    angle = 3 * π / 2;
                }
            } else {
                angle = Math.atan(distanceY / distanceX);
                if (distanceX < 0) {
                    angle += π;
                }
            }

            float currentAngle = HeadingSensor.yaw.get();

            if (fieldCentric.get()) {
                double centric = calibratedAngle.get();
                double angleOffset = currentAngle / 180 * π;
                angleOffset -= centric;
                angle -= angleOffset;
            }

            if (headingControl.get()) {
                if (rotationspeed == 0 && speed > 0) {
                    rotationspeed = -pid.get();
                } else {
                    desiredAngle.set(HeadingSensor.absoluteYaw.get());
                    HeadingSensor.resetAccumulator.event();
                }
            }

            driveInDirection(angle, speed, rotationspeed);
        }
    };

    private static EventOutput justStrafing = () -> {
        float rotationspeed = -pid.get();
        driveInDirection(0, leftJoystickX.get(), rotationspeed);
    };

    private static void driveInDirection(double angle, float speed, float rotationspeed) {
        float leftFront = (float) (speed * Math.sin(angle - π / 4) - rotationspeed);
        float rightFront = (float) (speed * Math.cos(angle - π / 4) + rotationspeed);
        float leftBack = (float) (speed * Math.cos(angle - π / 4) - rotationspeed);
        float rightBack = (float) (speed * Math.sin(angle - π / 4) + rotationspeed);
        float normalize = Math.max(
                Math.max(Math.abs(leftFront), Math.abs(rightFront)),
                Math.max(Math.abs(leftBack), Math.abs(rightBack)));
        if (normalize > 1) {
            leftFront /= normalize;
            rightFront /= normalize;
            leftBack /= normalize;
            rightBack /= normalize;
        }
        if (normalize < Math.abs(speed)) {
            float multiplier = Math.abs(speed) / normalize;
            leftFront *= multiplier;
            rightFront *= multiplier;
            leftBack *= multiplier;
            rightBack *= multiplier;
        }
        rightFrontMotor.set(rightFront);
        leftFrontMotor.set(leftFront);
        rightBackMotor.set(rightBack);
        leftBackMotor.set(leftBack);
    };

    private static EventOutput calibrate = () -> {
        HeadingSensor.resetAccumulator.event();
        float yaw = HeadingSensor.yaw.get();
        desiredAngle.set(HeadingSensor.absoluteYaw.get());
        if (isDisabled.get()) {
            yaw -= centricAngleOffset.get();
        }
        calibratedAngle.set((float) (yaw / 180 * π));
        Logger.info("Calibrated Angle: " + yaw);
    };

    public static void setup() {
        centricAngleOffset = ControlInterface.teleTuning.getFloat("Teleop Field Centric Default Angle +T", 0);
        headingControl = ControlInterface.teleTuning.getBoolean("Teleop Mecanum Keep Straight +T", false);
        recalibrateButton.send(calibrate);

        FloatStatus ultgain = ControlInterface.teleTuning.getFloat("Teleop PID Calibration Ultimate Gain +T", .03f);
        FloatStatus period = ControlInterface.teleTuning.getFloat("Teleop PID Calibration Oscillation Period +T", 2f);
        FloatStatus pconstant = ControlInterface.teleTuning.getFloat("Teleop PID Calibration P Constant +T", .6f);
        FloatStatus iconstant = ControlInterface.teleTuning.getFloat("Teleop PID Calibration I Constant +T", 2f);
        FloatStatus dconstant = ControlInterface.teleTuning.getFloat("Teleop PID Calibration D Constant +T", .125f);
        BooleanStatus calibrating = ControlInterface.teleTuning.getBoolean("Teleop PID Calibration +T", false);

        FloatInput p = FloatMixing.createDispatch(
                Mixing.select(calibrating, FloatMixing.multiplication.of((FloatInput) ultgain, (FloatInput) pconstant), ultgain),
                EventMixing.filterEvent(calibrating, true, FloatMixing.onUpdate(ultgain)));
        FloatInput i = Mixing.select(calibrating, FloatMixing.division.of(
                FloatMixing.multiplication.of(p, (FloatInput) iconstant), (FloatInput) period), FloatMixing.always(0));
        FloatInput d = Mixing.select(calibrating, FloatMixing.multiplication.of(
                FloatMixing.multiplication.of(p, (FloatInput) dconstant), (FloatInput) period), FloatMixing.always(0));

        Cluck.publish("Teleop PID P", p);
        Cluck.publish("Teleop PID I", i);
        Cluck.publish("Teleop PID D", d);

        pid = new PIDControl(HeadingSensor.absoluteYaw, desiredAngle, p, i, d);
        pid.setOutputBounds(-1f, 1f);

        FloatStatus integralBounds = ControlInterface.teleTuning.getFloat("Teleop PID Integral Bounds +T", .5f);
        pid.setIntegralBounds(FloatMixing.negate((FloatInput) integralBounds), integralBounds);

        Igneous.globalPeriodic.send(pid);

        ExpirationTimer timer = new ExpirationTimer();
        timer.schedule(10, HeadingSensor.zeroGyro);
        timer.schedule(1000, calibrate);
        timer.start();

        octocanumShifting.toggleWhen(octocanumShiftingButton);
        onlyStrafing.toggleWhen(strafingButton);
        onlyStrafing.setFalseWhen(octocanumShiftingButton);
        FloatMixing.pumpWhen(strafingButton, HeadingSensor.absoluteYaw, desiredAngle);
        strafingButton.send(pid.integralTotal.getSetEvent(0));
        FloatMixing.pumpWhen(octocanumShiftingButton, HeadingSensor.absoluteYaw, desiredAngle);

        Igneous.duringTele.send(EventMixing.filterEvent(BooleanMixing.andBooleans(octocanumShifting, onlyStrafing.asInvertedInput()), true, mecanum));
        Igneous.duringTele.send(EventMixing.filterEvent(BooleanMixing.andBooleans(octocanumShifting, onlyStrafing), true, justStrafing));
        Igneous.duringTele.send(EventMixing.filterEvent(octocanumShifting, false,
                DriverImpls.createTankDriverEvent(leftJoystickYChannel, rightJoystickYChannel, leftMotors, rightMotors)));

        Cluck.publish("Joystick 1 Right X Axis Raw", (FloatInput) rightJoystickXRaw);
        Cluck.publish("Joystick 1 Right Y Axis Raw", (FloatInput) rightJoystickYRaw);
        Cluck.publish("Joystick 1 Left X Axis Raw", (FloatInput) leftJoystickXRaw);
        Cluck.publish("Joystick 1 Left Y Axis Raw", (FloatInput) leftJoystickYRaw);

        Cluck.publish("Joystick 1 Right X Axis", rightJoystickX);
        Cluck.publish("Joystick 1 Right Y Axis", rightJoystickY);
        Cluck.publish("Joystick 1 Left X Axis", leftJoystickX);
        Cluck.publish("Joystick 1 Left Y Axis", leftJoystickY);

        Cluck.publish("Drive Motor Left Rear", leftBackMotor);
        Cluck.publish("Drive Motor Left Forward", leftFrontMotor);
        Cluck.publish("Drive Motor Right Rear", rightBackMotor);
        Cluck.publish("Drive Motor Right Forward", rightFrontMotor);
        Cluck.publish("Drive Mode", octocanumShifting);
        Cluck.publish("Teleop Strafing Only", onlyStrafing);
        Cluck.publish("Drive Encoder Left Raw", leftEncoderRaw);
        Cluck.publish("Drive Encoder Right Raw", rightEncoderRaw);
        Cluck.publish("Drive Encoder Left", leftEncoder);
        Cluck.publish("Drive Encoder Right", rightEncoder);
        Cluck.publish("Teleop Field Centric Calibrate", calibrate);
        Cluck.publish("Teleop PID Output", (FloatInput) pid);
        Cluck.publish("Drive Encoder Reset", (EventOutput) resetEncoders);
    }
}
