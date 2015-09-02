package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.EventStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.ExtendedMotor;
import ccre.ctrl.ExtendedMotorFailureException;
import ccre.ctrl.PIDController;
import ccre.ctrl.PauseTimer;
import ccre.ctrl.Ticker;
import ccre.frc.FRC;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;
import ccre.log.Logger;

public class Clamp {
    public static final boolean MODE_SPEED = true;
    public static final boolean MODE_HEIGHT = false;

    public static FloatStatus height = new FloatStatus();
    public static FloatStatus speed = new FloatStatus();
    public static BooleanStatus mode = new BooleanStatus(MODE_SPEED);
    private static FloatStatus autocalibrationOverrideSpeed = new FloatStatus();
    private static BooleanStatus autocalibrationOverrideEnable = new BooleanStatus(false);

    public static final BooleanStatus open = new BooleanStatus(FRC.makeSolenoid(3).invert());

    public static final EventOutput setBottom = mode.getSetFalseEvent().combine(height.getSetEvent(0));

    private static final BooleanStatus useEncoder = ControlInterface.mainTuning.getBoolean("Clamp Use Encoder +M", true);

    public static FloatInput heightReadout;
    public static final FloatInput heightPadding = ControlInterface.autoTuning.getFloat("Clamp Height Padding +A", 0.1f);

    public static BooleanInput atDesiredHeight;

    public static BooleanInput atTop;
    public static BooleanInput atBottom;
    private static final BooleanInput autoCalibrationEnabled = BooleanInput.always(ControlInterface.mainTuning.getBoolean("Clamp Autocalibration Allow +M", true).get());
    private static final BooleanStatus needsAutoCalibration = new BooleanStatus(useEncoder.get());// yes, this only sets the default value at startup.

    public static final BooleanInput waitingForAutoCalibration = needsAutoCalibration.and(autoCalibrationEnabled);
    public static final BooleanStatus clampEnabled = new BooleanStatus(true);

    public static void setup() {
        open.set(true);

        QuasarHelios.publishFault("clamp-encoder-disabled", useEncoder.not());

        EventStatus zeroEncoder = new EventStatus();
        needsAutoCalibration.setFalseWhen(zeroEncoder);
        FloatInput encoder = FRC.makeEncoder(10, 11, true, zeroEncoder);

        ExtendedMotor clampCAN = FRC.makeCANTalon(1);
        FloatOutput motorControlTemp = FloatOutput.ignored;

        try {
            motorControlTemp = clampCAN.asMode(ExtendedMotor.OutputControlMode.VOLTAGE_FRACTIONAL);

            if (motorControlTemp == null) {
                motorControlTemp = FloatOutput.ignored;
            }
        } catch (ExtendedMotorFailureException e) {
            Logger.severe("Exception thrown when creating clamp motor", e);
        }

        final FloatStatus speedControl = new FloatStatus(motorControlTemp.negate().addRamping(0.2f, FRC.constantPeriodic));

        Cluck.publish("Clamp CAN Enable", clampCAN.asEnable());
        Ticker updateCAN = new Ticker(100);// don't update too frequently
        Cluck.publish("Clamp CAN Bus Voltage", clampCAN.asStatus(ExtendedMotor.StatusType.BUS_VOLTAGE, updateCAN));
        Cluck.publish("Clamp CAN Output Current", clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT, updateCAN));
        Cluck.publish("Clamp CAN Output Voltage", clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_VOLTAGE, updateCAN));
        Cluck.publish("Clamp CAN Temperature", clampCAN.asStatus(ExtendedMotor.StatusType.TEMPERATURE, updateCAN));
        Cluck.publish("Clamp CAN Any Fault", QuasarHelios.publishFault("clamp-can", clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.ANY_FAULT, updateCAN)));
        Cluck.publish("Clamp CAN Bus Voltage Fault", clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.BUS_VOLTAGE_FAULT, updateCAN));
        Cluck.publish("Clamp CAN Temperature Fault", clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.TEMPERATURE_FAULT, updateCAN));

        BooleanInput maxCurrentNow = clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT).atLeast(ControlInterface.mainTuning.getFloat("Clamp Max Current Amps +M", 45));
        EventInput maxCurrentEvent = FRC.constantPeriodic.and(maxCurrentNow);

        QuasarHelios.publishStickyFault("clamp-current-fault", maxCurrentEvent);

        FloatStatus clampResetTime = ControlInterface.mainTuning.getFloat("Clamp Reset Time", 1000);

        PauseTimer timer = new PauseTimer((long) clampResetTime.get());
        timer.triggerAtChanges(clampEnabled.getSetFalseEvent(), clampEnabled.getSetTrueEvent());
        maxCurrentEvent.send(timer);

        BooleanInput limitTop = FRC.makeDigitalInput(2, FRC.constantPeriodic).not();
        BooleanInput limitBottom = FRC.makeDigitalInput(3, FRC.constantPeriodic).not();

        BooleanStatus atTopStatus = new BooleanStatus(), atBottomStatus = new BooleanStatus();
        atTop = atTopStatus;
        atBottom = atBottomStatus;

        atTop.send(new BooleanOutput() {
            public void set(boolean value) {
                Logger.finer(value ? "Clamp at top" : "Clamp left top");
            }
        });
        atBottom.send(new BooleanOutput() {
            public void set(boolean value) {
                Logger.finer(value ? "Clamp at bottom" : "Clamp left bottom");
            }
        });

        atTopStatus.setTrueWhen(limitTop.onPress().and(speedControl.atMost(-0.01f)));
        atTopStatus.setFalseWhen(limitTop.onRelease().and(speedControl.atLeast(0.01f)));
        atBottomStatus.setTrueWhen(limitBottom.onPress().and(speedControl.atLeast(0.01f)));
        atBottomStatus.setFalseWhen(limitBottom.onRelease().and(speedControl.atMost(-0.01f)));
        atTopStatus.setFalseWhen(atBottomStatus.onPress());
        atBottomStatus.setFalseWhen(atTopStatus.onPress());

        QuasarHelios.publishFault("clamp-encoder-zero", encoder.inRange(-0.1f, 0.1f).andNot(atTopStatus));

        FloatStatus distance = ControlInterface.mainTuning.getFloat("Clamp Distance +M", 1.0f);

        distance.setWhen(encoder.negated(), atBottomStatus.onPress().and(useEncoder));
        atTopStatus.onPress().and(useEncoder).send(zeroEncoder);

        FloatStatus p = ControlInterface.mainTuning.getFloat("Clamp PID P", -20.0f);
        FloatStatus i = ControlInterface.mainTuning.getFloat("Clamp PID I", 0.0f);
        FloatStatus d = ControlInterface.mainTuning.getFloat("Clamp PID D", 0.0f);

        heightReadout = encoder.normalize(distance.negated(), 0f);

        FloatInput distanceFromTarget = height.minus(heightReadout);

        atDesiredHeight = distanceFromTarget.inRange(heightPadding.negated(), heightPadding);

        PIDController pid = new PIDController(heightReadout, height, p, i, d);

        pid.integralTotal.setWhen(0.0f, mode.onRelease());
        pid.setOutputBounds(ControlInterface.mainTuning.getFloat("clamp-max-height-speed", 1.0f));
        height.setWhen(heightReadout, mode.onRelease());

        QuasarHelios.constantControl.send(pid);

        FloatOutput out = (value) -> {
            if (atTopStatus.get()) {
                value = Math.max(value, 0);
            }
            if (atBottomStatus.get()) {
                value = Math.min(value, 0);
            }
            if ((value >= -0.1f && value <= 0.1f) || !clampEnabled.get()) {
                value = 0;
            }
            speedControl.set(value);
        };

        EventInput manualOverride = QuasarHelios.globalControl.and(speed.outsideRange(-0.3f, 0.3f));
        mode.setTrueWhen(manualOverride);
        needsAutoCalibration.setFalseWhen(manualOverride);// allow the user to override the autocalibration
        mode.setTrueWhen(FRC.startTele);

        FloatStatus slowdownThreshold = ControlInterface.mainTuning.getFloat("Clamp Slowdown Threshold +M", 0.2f);
        FloatInput speedFactor = heightReadout.atMost(slowdownThreshold).toFloat(1.0f, 0.5f);

        Cluck.publish("Clamp Speed Factor", speedFactor);

        autocalibrationOverrideEnable.toFloat(mode.orNot(useEncoder).toFloat(pid, speed).multipliedBy(speedFactor), autocalibrationOverrideSpeed).send(out);

        // The autocalibrator runs when it's needed, AND allowed to by tuning (so that it can be disabled) AND the robot is enable in teleop or autonomous mode.
        // Once the encoder gets reset, it's no longer needed, and won't run. (Unless manually reactivated.)
        new InstinctModule(FRC.getIsEnabled().and(waitingForAutoCalibration).and(FRC.getIsTeleop().or(FRC.getIsAutonomous()))) {
            private final FloatInput downwardTime = ControlInterface.mainTuning.getFloat("Clamp Autocalibration Downward Time +M", 0.1f);

            @Override
            protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
                try {
                    if (limitTop.get() || atTop.get()) {
                        zeroEncoder.event();
                        Logger.info("Autocalibrated: already at top.");
                    } else {
                        Logger.info("Attempting autocalibration: not at top; jolting down...");
                        // first, go down momentarily, so if we're already at the top past the limit switch, we won't break anything
                        autocalibrationOverrideSpeed.set(1.0f);
                        autocalibrationOverrideEnable.set(true);
                        waitForTime(downwardTime);
                        // now go up to the top
                        autocalibrationOverrideSpeed.set(-1.0f);
                        Logger.info("Autocalibration: aligning up...");
                        try {
                            waitUntil(atTop);
                        } finally {
                            autocalibrationOverrideSpeed.set(0.0f);
                        }
                    }
                } finally {
                    autocalibrationOverrideEnable.set(false);
                }
            }

            protected String getTypeName() {
                return "clamp autocalibrator";
            }
        };

        Cluck.publish("Clamp Open Control", open);
        Cluck.publish("Clamp Height Encoder", encoder);
        Cluck.publish("Clamp Height Scaled", heightReadout);
        Cluck.publish("Clamp Height Setting", height);
        Cluck.publish("Clamp Integral Total", pid.integralTotal);
        Cluck.publish("Clamp Limit Top (Direct)", limitTop);
        Cluck.publish("Clamp Limit Bottom (Direct)", limitBottom);
        Cluck.publish("Clamp Limit Top", atTopStatus);
        Cluck.publish("Clamp Limit Bottom", atBottomStatus);
        Cluck.publish("Clamp Motor Speed", speedControl);
        Cluck.publish("Clamp PID Output", (FloatInput) pid);

        Cluck.publish("Clamp Distance", distance);
        Cluck.publish("Clamp Min Set", encoder);
        Cluck.publish("Clamp Max Set", zeroEncoder);
        Cluck.publish("Clamp Mode", mode);
        Cluck.publish("Clamp Speed", speed);
        Cluck.publish("Clamp Enabled", clampEnabled);
        Cluck.publish("Clamp Needs Autocalibration", needsAutoCalibration);
        Cluck.publish("Clamp Max Current Amps Reached", maxCurrentEvent);
    }
}
