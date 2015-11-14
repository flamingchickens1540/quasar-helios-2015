package org.team1540.quasarhelios;

import ccre.channel.BooleanCell;
import ccre.channel.BooleanInput;
import ccre.channel.EventCell;
import ccre.channel.EventOutput;
import ccre.channel.FloatCell;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.cluck.Cluck;
import ccre.ctrl.PIDController;
import ccre.frc.FRC;
import ccre.log.Logger;
import ccre.timers.ExpirationTimer;

public class DriveCode {
    public static final BooleanCell shiftEnabled = new BooleanCell();

    public static final FloatCell leftJoystickXRaw = new FloatCell();
    public static final FloatCell leftJoystickYRaw = new FloatCell();
    public static final FloatCell rightJoystickXRaw = new FloatCell();
    public static final FloatCell rightJoystickYRaw = new FloatCell();

    public static EventCell recalibrateButton = new EventCell();
    public static EventCell fieldCentricButton = new EventCell();
    public static EventCell strafingButton = new EventCell();
    public static FloatCell forwardTrigger = new FloatCell();
    public static FloatCell backwardTrigger = new FloatCell();

    private static final FloatInput multiplier = shiftEnabled.toFloat(1.0f, ControlInterface.teleTuning.getFloat("Drive Shift Multiplier +T", 0.5f));

    private static final BooleanCell pitMode = new BooleanCell();

    public static final EventOutput disablePitMode = pitMode.eventSet(false);

    private static FloatInput wrapForPitMode(FloatInput input) {
        return pitMode.toFloat(input, 0);
    }

    public static final FloatInput leftJoystickX = wrapForPitMode(leftJoystickXRaw);
    public static final FloatInput leftJoystickY = wrapForPitMode(leftJoystickYRaw.multipliedBy(multiplier).plus(backwardTrigger).minus(forwardTrigger));
    public static final FloatInput rightJoystickX = wrapForPitMode(rightJoystickXRaw);
    public static final FloatInput rightJoystickY = wrapForPitMode(rightJoystickYRaw.multipliedBy(multiplier).plus(backwardTrigger).minus(forwardTrigger));

    public static final BooleanCell disableMotorsForCurrentFault = new BooleanCell(false);

    private static final FloatOutput leftFrontMotor = wrapWithDriveDisable(FRC.talon(9, FRC.MOTOR_REVERSE, .1f));
    private static final FloatOutput leftBackMotor = wrapWithDriveDisable(FRC.talon(8, FRC.MOTOR_REVERSE, .1f));
    private static final FloatOutput rightFrontMotor = wrapWithDriveDisable(FRC.talon(0, FRC.MOTOR_FORWARD, .1f));
    private static final FloatOutput rightBackMotor = wrapWithDriveDisable(FRC.talon(1, FRC.MOTOR_FORWARD, .1f));
    public static final FloatOutput rightMotors = rightFrontMotor.combine(rightBackMotor);
    public static final FloatOutput leftMotors = leftFrontMotor.combine(leftBackMotor);
    public static final FloatOutput allMotors = leftMotors.combine(rightMotors);
    public static final FloatOutput rotate = leftMotors.combine(rightMotors.negate());
    public static final FloatOutput strafe = leftFrontMotor.negate().combine(rightBackMotor.negate()).combine(leftBackMotor).combine(rightFrontMotor);
    public static final BooleanCell onlyStrafing = new BooleanCell();
    private static final EventCell resetEncoders = new EventCell();
    public static final FloatInput leftEncoderRaw = FRC.encoder(6, 7, FRC.MOTOR_REVERSE, resetEncoders);
    public static final FloatInput rightEncoderRaw = FRC.encoder(8, 9, FRC.MOTOR_FORWARD, resetEncoders);
    public static final FloatInput encoderScaling = ControlInterface.mainTuning.getFloat("Drive Encoder Scaling +M", -0.0091f);
    public static final FloatInput leftEncoder = leftEncoderRaw.multipliedBy(encoderScaling);
    public static final FloatInput rightEncoder = rightEncoderRaw.multipliedBy(encoderScaling);

    private static final double π = Math.PI;

    private static FloatCell centricAngleOffset;
    private static BooleanCell headingControl;
    private static final FloatCell calibratedAngle = new FloatCell();
    private static final BooleanCell fieldCentric = ControlInterface.teleTuning.getBoolean("Teleop Field Centric Enabled +T", false);

    private static final FloatCell desiredAngle = new FloatCell();
    private static final BooleanInput isDisabled = FRC.robotDisabled();
    private static PIDController pid;

    private static EventOutput mecanum = new EventOutput() {
        public void event() {
            float distanceY = leftJoystickY.get();
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
                    rotationspeed = pid.get();
                } else {
                    desiredAngle.set(HeadingSensor.absoluteYaw.get());
                    HeadingSensor.resetAccumulator.event();
                }
            }

            driveInDirection(angle, speed, rotationspeed);
        }
    };

    private static EventOutput justStrafing = () -> {
        // float rotationspeed = -pid.get();
        driveInDirection(0, leftJoystickX.get(), rightJoystickX.get());
    };

    private static void driveInDirection(double angle, float speed, float rotationspeed) {
        float leftFront = (float) (speed * Math.sin(angle - π / 4) - rotationspeed);
        float rightFront = (float) (speed * Math.cos(angle - π / 4) + rotationspeed);
        float leftBack = (float) (speed * Math.cos(angle - π / 4) - rotationspeed);
        float rightBack = (float) (speed * Math.sin(angle - π / 4) + rotationspeed);
        float normalize = Math.max(Math.max(Math.abs(leftFront), Math.abs(rightFront)), Math.max(Math.abs(leftBack), Math.abs(rightBack)));
        if (normalize > 1) {
            leftFront /= normalize;
            rightFront /= normalize;
            leftBack /= normalize;
            rightBack /= normalize;
        }
        if (normalize < Math.abs(speed)) {
            float mul = Math.abs(speed) / normalize;
            leftFront *= mul;
            rightFront *= mul;
            leftBack *= mul;
            rightBack *= mul;
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

    private static FloatOutput wrapWithDriveDisable(FloatOutput out) {
        if (out == null) {
            throw new NullPointerException();
        }
        out.setWhen(0, disableMotorsForCurrentFault.onPress());
        return out.filterNot(disableMotorsForCurrentFault);
    }

    public static void setup() {
        Cluck.publish("(PIT) Pit Mode", pitMode);
        QuasarHelios.publishFault("in-pit-mode", pitMode, disablePitMode);
        FRC.startTele.and(FRC.isOnFMS()).send(disablePitMode);

        centricAngleOffset = ControlInterface.teleTuning.getFloat("Teleop Field Centric Default Angle +T", 0);
        headingControl = ControlInterface.teleTuning.getBoolean("Teleop Mecanum Keep Straight +T", false);
        headingControl.toggleWhen(fieldCentricButton);
        recalibrateButton.send(calibrate);

        FloatCell ultgain = ControlInterface.teleTuning.getFloat("Teleop PID Calibration Ultimate Gain +T", .03f);
        FloatCell period = ControlInterface.teleTuning.getFloat("Teleop PID Calibration Oscillation Period +T", 2f);
        FloatCell pconstant = ControlInterface.teleTuning.getFloat("Teleop PID Calibration P Constant +T", .6f);
        FloatCell iconstant = ControlInterface.teleTuning.getFloat("Teleop PID Calibration I Constant +T", 2f);
        FloatCell dconstant = ControlInterface.teleTuning.getFloat("Teleop PID Calibration D Constant +T", .125f);
        BooleanCell calibrating = ControlInterface.teleTuning.getBoolean("Teleop PID Calibration +T", false);

        FloatInput p = calibrating.toFloat(ultgain.multipliedBy(pconstant), ultgain).filterUpdates(calibrating);
        FloatInput i = calibrating.toFloat(p.multipliedBy(iconstant).dividedBy(period), 0);
        FloatInput d = calibrating.toFloat(p.multipliedBy(dconstant).multipliedBy(period), 0);

        Cluck.publish("Teleop PID P", p);
        Cluck.publish("Teleop PID I", i);
        Cluck.publish("Teleop PID D", d);

        pid = new PIDController(HeadingSensor.absoluteYaw, desiredAngle, p, i, d);
        pid.setOutputBounds(1f);

        pid.setIntegralBounds(ControlInterface.teleTuning.getFloat("Teleop PID Integral Bounds +T", .5f));

        FRC.globalPeriodic.send(pid);

        ExpirationTimer timer = new ExpirationTimer();
        timer.schedule(10, HeadingSensor.zeroGyro);
        timer.schedule(1000, calibrate);
        timer.start();

        onlyStrafing.toggleWhen(strafingButton);
        desiredAngle.setWhen(HeadingSensor.absoluteYaw, strafingButton);
        strafingButton.send(pid.integralTotal.eventSet(0));

        FRC.duringTele.andNot(onlyStrafing).send(mecanum);
        FRC.duringTele.and(onlyStrafing).send(justStrafing);

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
        Cluck.publish("Teleop Strafing Only", onlyStrafing);
        Cluck.publish("Drive Encoder Left Raw", leftEncoderRaw);
        Cluck.publish("Drive Encoder Right Raw", rightEncoderRaw);
        Cluck.publish("Drive Encoder Left", leftEncoder);
        Cluck.publish("Drive Encoder Right", rightEncoder);
        Cluck.publish("Teleop Field Centric Calibrate", calibrate);
        Cluck.publish("Teleop PID Output", (FloatInput) pid);
        Cluck.publish("Drive Encoder Reset", (EventOutput) resetEncoders);
    }

    public static void angularStrafeForAuto(float strafeAmount, float forwardAmount) {
        leftFrontMotor.set(forwardAmount - strafeAmount);
        leftBackMotor.set(forwardAmount + strafeAmount);
        rightFrontMotor.set(forwardAmount + strafeAmount);
        rightBackMotor.set(forwardAmount - strafeAmount);
    }
}
