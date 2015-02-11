package org.team1540.quasarhelios;

import ccre.channel.*;
import ccre.cluck.Cluck;
import ccre.ctrl.*;
import ccre.igneous.*;
import ccre.log.Logger;

public class DriveCode {
    private static FloatStatus leftJoystickChannelX = new FloatStatus();
    private static FloatStatus leftJoystickChannelY = new FloatStatus();
    private static FloatStatus rightJoystickChannelX = new FloatStatus();
    private static FloatStatus rightJoystickChannelY = new FloatStatus();

    public static FloatOutput leftJoystickX = leftJoystickChannelX;
    public static FloatOutput leftJoystickY = leftJoystickChannelY;
    public static FloatOutput rightJoystickX = rightJoystickChannelX;
    public static FloatOutput rightJoystickY = rightJoystickChannelY;

    public static EventInput octocanumShiftingButton;
    public static EventInput recalibrateButton;
    private static final FloatOutput leftFrontMotor = Igneous.makeTalonMotor(9, Igneous.MOTOR_REVERSE, .1f);
    private static final FloatOutput leftBackMotor = Igneous.makeTalonMotor(8, Igneous.MOTOR_REVERSE, .1f);
    private static final FloatOutput rightFrontMotor = Igneous.makeTalonMotor(0, Igneous.MOTOR_FORWARD, .1f);
    private static final FloatOutput rightBackMotor = Igneous.makeTalonMotor(1, Igneous.MOTOR_FORWARD, .1f);
    private static final FloatOutput rightMotors = FloatMixing.combine(rightFrontMotor, rightBackMotor);
    private static final FloatOutput leftMotors = FloatMixing.combine(leftFrontMotor, leftBackMotor);
    public static final FloatOutput allMotors = FloatMixing.combine(leftMotors, rightMotors);
    public static final FloatOutput rotate = FloatMixing.combine(leftMotors, FloatMixing.negate(rightMotors));
    public static final BooleanStatus octocanumShifting = new BooleanStatus(Igneous.makeSolenoid(0));
    public static final FloatInput leftEncoderRaw = FloatMixing.createDispatch(Igneous.makeEncoder(6, 7, Igneous.MOTOR_REVERSE), Igneous.globalPeriodic);
    public static final FloatInput rightEncoderRaw = FloatMixing.createDispatch(Igneous.makeEncoder(8, 9, Igneous.MOTOR_FORWARD), Igneous.globalPeriodic);
    public static final FloatInput encoderScaling = ControlInterface.mainTuning.getFloat("main-encoder-scaling", 0.1f);
    public static final FloatInput leftEncoder = FloatMixing.multiplication.of(leftEncoderRaw, encoderScaling);
    public static final FloatInput rightEncoder = FloatMixing.multiplication.of(rightEncoderRaw, encoderScaling);

    private static final double π = Math.PI;

    private static FloatStatus centricAngleOffset;
    private static final FloatStatus calibratedAngle = new FloatStatus(0);
    private static final BooleanStatus fieldCentric = new BooleanStatus();
    private static final BooleanStatus headingControl = new BooleanStatus(true);

    private static final FloatStatus desiredAngle = new FloatStatus();
    private static final BooleanInputPoll isDisabled = Igneous.getIsDisabled();
    private static PIDControl pid;

    private static EventOutput mecanum = new EventOutput() {
        public void event() {
            float distanceY = leftJoystickChannelY.get();
            float distanceX = leftJoystickChannelX.get();
            float speed = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);
            if (speed > 1) {
                speed = 1;
            }
            float rotationspeed = rightJoystickChannelX.get();
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
        }
    };

    private static EventOutput calibrate = new EventOutput() {
        public void event() {
            HeadingSensor.resetAccumulator.event();
            float yaw = HeadingSensor.yaw.get();
            desiredAngle.set(HeadingSensor.absoluteYaw.get());
            if (isDisabled.get()) {
                yaw -= centricAngleOffset.get();
            }
            calibratedAngle.set((float) (yaw / 180 * π));
            Logger.info("Calibrated Angle: " + yaw);
        }
    };

    public static void setup() {
        centricAngleOffset = ControlInterface.mainTuning.getFloat("main-drive-centricAngle", 0);
        recalibrateButton.send(calibrate);

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

        Cluck.publish("Drive PID P",p);
        Cluck.publish("Drive PID I",i);
        Cluck.publish("Drive PID D",d);

        pid = new PIDControl(HeadingSensor.absoluteYaw, desiredAngle, p, i, d);
        pid.setOutputBounds(-1f, 1f);
        pid.setIntegralBounds(-.5f, .5f);

        Igneous.globalPeriodic.send(pid);

        ExpirationTimer timer = new ExpirationTimer();
        timer.schedule(10, HeadingSensor.zeroGyro);
        timer.schedule(1000, calibrate);
        timer.start();

        fieldCentric.setFalseWhen(Igneous.startAuto);
        fieldCentric.setTrueWhen(Igneous.startTele);
        octocanumShifting.toggleWhen(octocanumShiftingButton);
        FloatMixing.pumpWhen(octocanumShiftingButton, HeadingSensor.absoluteYaw, desiredAngle);
        Igneous.duringTele.send(EventMixing.filterEvent(octocanumShifting, false, mecanum));
        Igneous.duringTele.send(EventMixing.filterEvent(octocanumShifting, true,
                DriverImpls.createTankDriverEvent(leftJoystickChannelY, rightJoystickChannelY, leftMotors, rightMotors)));

        Cluck.publish(QuasarHelios.testPrefix + "Drive Motor Left Rear", leftBackMotor);
        Cluck.publish(QuasarHelios.testPrefix + "Drive Motor Left Forward", leftFrontMotor);
        Cluck.publish(QuasarHelios.testPrefix + "Drive Motor Right Rear", rightBackMotor);
        Cluck.publish(QuasarHelios.testPrefix + "Drive Motor Right Forward", rightFrontMotor);
        Cluck.publish(QuasarHelios.testPrefix + "Drive Mode", octocanumShifting);
        Cluck.publish(QuasarHelios.testPrefix + "Drive Encoder Left", leftEncoderRaw);
        Cluck.publish(QuasarHelios.testPrefix + "Drive Encoder Rightr", rightEncoderRaw);
        Cluck.publish("Calibrate Field Centric Angle", calibrate);
        Cluck.publish("Toggle Field Centric", fieldCentric);
        Cluck.publish("Toggle Heading Control", headingControl);
        Cluck.publish("Drive PID", (FloatInput) pid);
    }
}
