package org.team1540.quasarhelios;

import ccre.channel.BooleanCell;
import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.EventCell;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.FloatCell;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.cluck.Cluck;
import ccre.ctrl.ExtendedMotor;
import ccre.ctrl.ExtendedMotorFailureException;
import ccre.ctrl.PIDController;
import ccre.frc.FRC;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;
import ccre.log.Logger;
import ccre.timers.PauseTimer;

public class Clamp {
    public static final boolean MODE_SPEED = true;
    public static final boolean MODE_HEIGHT = false;

    public static FloatCell height = new FloatCell();
    public static FloatCell speed = new FloatCell();
    public static BooleanCell mode = new BooleanCell(MODE_SPEED);
    private static FloatCell autocalibrationOverrideSpeed = new FloatCell();
    private static BooleanCell autocalibrationOverrideEnable = new BooleanCell(false);

    public static final BooleanCell open = new BooleanCell(FRC.solenoid(3).invert());

    public static final EventOutput setBottom = mode.eventSet(false).combine(height.eventSet(0));

    private static final BooleanCell useEncoder = ControlInterface.mainTuning.getBoolean("Clamp Use Encoder +M", true);

    public static FloatInput heightReadout;
    public static final FloatInput heightPadding = ControlInterface.autoTuning.getFloat("Clamp Height Padding +A", 0.1f);

    public static BooleanInput atDesiredHeight;

    public static BooleanInput atTop;
    public static BooleanInput atBottom;
    // yes, this only sets the default value at startup.
    private static final BooleanInput autoCalibrationEnabled = BooleanInput.always(ControlInterface.mainTuning.getBoolean("Clamp Autocalibration Allow +M", true).get());
    private static final BooleanCell needsAutoCalibration = new BooleanCell(useEncoder.get());

    public static final BooleanInput waitingForAutoCalibration = needsAutoCalibration.and(autoCalibrationEnabled);
    public static final BooleanCell clampEnabled = new BooleanCell(true);

    public static void setup() {
        open.set(true);

        QuasarHelios.publishFault("clamp-encoder-disabled", useEncoder.not());

        EventCell zeroEncoder = new EventCell();
        needsAutoCalibration.setFalseWhen(zeroEncoder);
        FloatInput encoder = FRC.encoder(10, 11, true, zeroEncoder);

        ExtendedMotor clampCAN = FRC.talonCAN(1);
        FloatOutput motorControlTemp = FloatOutput.ignored;

        try {
            motorControlTemp = clampCAN.simpleControl();
        } catch (ExtendedMotorFailureException e) {
            Logger.severe("Exception thrown when creating clamp motor", e);
        }

        final FloatCell speedControl = new FloatCell(motorControlTemp.negate().addRamping(0.2f, FRC.constantPeriodic));

        Cluck.publish("Clamp CAN Enable", clampCAN.asEnable());
        Cluck.publish("Clamp CAN Bus Voltage", clampCAN.asStatus(ExtendedMotor.StatusType.BUS_VOLTAGE));
        Cluck.publish("Clamp CAN Output Current", clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT));
        Cluck.publish("Clamp CAN Output Voltage", clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_VOLTAGE));
        Cluck.publish("Clamp CAN Temperature", clampCAN.asStatus(ExtendedMotor.StatusType.TEMPERATURE));
        Cluck.publish("Clamp CAN Any Fault", QuasarHelios.publishFault("clamp-can", clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.ANY_FAULT)));
        Cluck.publish("Clamp CAN Bus Voltage Fault", clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.BUS_VOLTAGE_FAULT));
        Cluck.publish("Clamp CAN Temperature Fault", clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.TEMPERATURE_FAULT));

        BooleanInput maxCurrentNow = clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT).atLeast(ControlInterface.mainTuning.getFloat("Clamp Max Current Amps +M", 45));
        EventInput maxCurrentEvent = FRC.constantPeriodic.and(maxCurrentNow);

        QuasarHelios.publishStickyFault("clamp-current-fault", maxCurrentEvent);

        FloatCell clampResetTime = ControlInterface.mainTuning.getFloat("Clamp Reset Time", 1000);

        PauseTimer timer = new PauseTimer((long) clampResetTime.get());
        timer.triggerAtChanges(clampEnabled.eventSet(false), clampEnabled.eventSet(true));
        maxCurrentEvent.send(timer);

        BooleanInput limitTop = FRC.digitalInput(2, FRC.constantPeriodic).not();
        BooleanInput limitBottom = FRC.digitalInput(3, FRC.constantPeriodic).not();

        BooleanCell atTopStatus = new BooleanCell(), atBottomStatus = new BooleanCell();
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

        FloatCell distance = ControlInterface.mainTuning.getFloat("Clamp Distance +M", 1.0f);

        distance.negate().setWhen(encoder, atBottomStatus.onPress().and(useEncoder));
        zeroEncoder.on(atTopStatus.onPress().and(useEncoder));

        FloatCell p = ControlInterface.mainTuning.getFloat("Clamp PID P", 20.0f);
        FloatCell i = ControlInterface.mainTuning.getFloat("Clamp PID I", 0.0f);
        FloatCell d = ControlInterface.mainTuning.getFloat("Clamp PID D", 0.0f);

        heightReadout = encoder.normalize(distance.negated(), 0);

        FloatInput distanceFromTarget = height.minus(heightReadout);

        atDesiredHeight = distanceFromTarget.inRange(heightPadding.negated(), heightPadding);

        PIDController pid = new PIDController(heightReadout, height, p, i, d);

        pid.integralTotal.setWhen(0, mode.onRelease());
        pid.setOutputBounds(ControlInterface.mainTuning.getFloat("clamp-max-height-speed", 1.0f));
        height.setWhen(heightReadout, mode.onRelease());

        QuasarHelios.constantControl.send(pid);

        EventInput manualOverride = QuasarHelios.globalControl.and(speed.outsideRange(-0.3f, 0.3f));
        mode.setTrueWhen(manualOverride);
        // allow the user to override the autocalibration
        needsAutoCalibration.setFalseWhen(manualOverride);
        mode.setTrueWhen(FRC.startTele);

        FloatInput speedFactor = heightReadout.atMost(ControlInterface.mainTuning.getFloat("Clamp Slowdown Threshold +M", 0.2f)).toFloat(1.0f, 0.5f);

        Cluck.publish("Clamp Speed Factor", speedFactor);

        FloatInput userSpeed = mode.orNot(useEncoder).toFloat(pid.negated(), speed).multipliedBy(speedFactor);
        FloatInput controlSpeed = QuasarHelios.limitSwitches(autocalibrationOverrideEnable.toFloat(userSpeed, autocalibrationOverrideSpeed), atTopStatus, atBottomStatus);
        clampEnabled.toFloat(0, controlSpeed.deadzone(0.1f)).send(speedControl);

        // The autocalibrator runs when it's needed, AND allowed to by tuning
        // (so that it can be disabled) AND the robot is enable in teleop or
        // autonomous mode.
        // Once the encoder gets reset, it's no longer needed, and won't run.
        // (Unless manually reactivated.)
        new InstinctModule(FRC.robotEnabled().and(waitingForAutoCalibration).and(FRC.inTeleopMode().or(FRC.inAutonomousMode()))) {
            private final FloatInput downwardTime = ControlInterface.mainTuning.getFloat("Clamp Autocalibration Downward Time +M", 0.1f);

            @Override
            protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
                try {
                    if (limitTop.get() || atTop.get()) {
                        zeroEncoder.event();
                        Logger.info("Autocalibrated: already at top.");
                    } else {
                        Logger.info("Attempting autocalibration: not at top; jolting down...");
                        // first, go down momentarily, so if we're already at
                        // the top past the limit switch, we won't break
                        // anything
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
        Cluck.publish("Clamp Min Set", distance.negate().eventSet(encoder));
        Cluck.publish("Clamp Max Set", zeroEncoder);
        Cluck.publish("Clamp Mode", mode);
        Cluck.publish("Clamp Speed", speed);
        Cluck.publish("Clamp Enabled", clampEnabled);
        Cluck.publish("Clamp Needs Autocalibration", needsAutoCalibration);
        Cluck.publish("Clamp Max Current Amps Reached", maxCurrentEvent);
    }
}
