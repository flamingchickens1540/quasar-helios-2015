package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanStatus;
import ccre.channel.EventOutput;
import ccre.channel.EventStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.ExtendedMotor;
import ccre.ctrl.ExtendedMotorFailureException;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.ctrl.PIDControl;
import ccre.ctrl.Ticker;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;
import ccre.log.Logger;

public class Clamp {
    public static final boolean MODE_SPEED = true;
    public static final boolean MODE_HEIGHT = false;

    public static FloatStatus height = new FloatStatus();
    public static FloatStatus speed = new FloatStatus();
    public static BooleanStatus mode = new BooleanStatus(MODE_SPEED);

    public static final BooleanStatus open = new BooleanStatus(BooleanMixing.invert(Igneous.makeSolenoid(3)));

    public static final EventOutput setBottom = EventMixing.combine(mode.getSetFalseEvent(), height.getSetEvent(0));

    private static final BooleanStatus useEncoder = ControlInterface.mainTuning.getBoolean("Clamp Use Encoder +M", true);

    public static FloatInputPoll heightReadout;
    public static final FloatInputPoll heightPadding = ControlInterface.autoTuning.getFloat("Clamp Height Padding +A", 0.1f);

    public static BooleanInputPoll atDesiredHeight;
    
    public static BooleanInput atTop;
    public static BooleanInput atBottom;
    private static final BooleanStatus needsAutoCalibration = new BooleanStatus(useEncoder.get()); // yes, this only sets the default value at startup.

    public static void setup() {
        QuasarHelios.publishFault("clamp-encoder-disabled", BooleanMixing.invert((BooleanInputPoll) useEncoder));

        EventStatus zeroEncoder = new EventStatus();
        needsAutoCalibration.setFalseWhen(zeroEncoder);
        FloatInputPoll encoder = Igneous.makeEncoder(10, 11, true, zeroEncoder);
        Igneous.startAuto.send(zeroEncoder);

        QuasarHelios.publishFault("clamp-encoder-zero", FloatMixing.floatIsInRange(encoder, -0.1f, 0.1f));

        ExtendedMotor clampCAN = Igneous.makeCANTalon(1);
        FloatOutput motorControlTemp = FloatMixing.ignoredFloatOutput;

        try {
            motorControlTemp = clampCAN.asMode(ExtendedMotor.OutputControlMode.VOLTAGE_FRACTIONAL);

            if (motorControlTemp == null) {
                motorControlTemp = FloatMixing.ignoredFloatOutput;
            }
        } catch (ExtendedMotorFailureException e) {
            Logger.severe("Exception thrown when creating clamp motor", e);
        }

        final FloatStatus speedControl = new FloatStatus(FloatMixing.addRamping(0.2f, Igneous.constantPeriodic, FloatMixing.negate(motorControlTemp)));

        Cluck.publish("Clamp CAN Enable", clampCAN.asEnable());
        Ticker updateCAN = new Ticker(100);
        Cluck.publish("Clamp CAN Bus Voltage", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.BUS_VOLTAGE), updateCAN));
        Cluck.publish("Clamp CAN Output Current", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_CURRENT), updateCAN));
        Cluck.publish("Clamp CAN Output Voltage", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.OUTPUT_VOLTAGE), updateCAN));
        Cluck.publish("Clamp CAN Temperature", FloatMixing.createDispatch(clampCAN.asStatus(ExtendedMotor.StatusType.TEMPERATURE), updateCAN));
        Cluck.publish("Clamp CAN Any Fault", QuasarHelios.publishFault("clamp-can", clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.ANY_FAULT)));
        Cluck.publish("Clamp CAN Bus Voltage Fault", BooleanMixing.createDispatch(clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.BUS_VOLTAGE_FAULT), updateCAN));
        Cluck.publish("Clamp CAN Temperature Fault", BooleanMixing.createDispatch(clampCAN.getDiagnosticChannel(ExtendedMotor.DiagnosticType.TEMPERATURE_FAULT), updateCAN));

        BooleanInput limitTop = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(2)), Igneous.constantPeriodic);
        BooleanInput limitBottom = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(3)), Igneous.constantPeriodic);

        BooleanStatus atTopStatus = new BooleanStatus(), atBottomStatus = new BooleanStatus();
        atTop = atTopStatus;
        atBottom = atBottomStatus;
        
        atTopStatus.setTrueWhen(EventMixing.filterEvent(FloatMixing.floatIsAtMost(speedControl, -0.01f), true, BooleanMixing.onPress(limitTop)));
        atTopStatus.setFalseWhen(EventMixing.filterEvent(FloatMixing.floatIsAtLeast(speedControl, 0.01f), true, BooleanMixing.onRelease(limitTop)));
        atBottomStatus.setTrueWhen(EventMixing.filterEvent(FloatMixing.floatIsAtLeast(speedControl, 0.01f), true, BooleanMixing.onPress(limitBottom)));
        atBottomStatus.setFalseWhen(EventMixing.filterEvent(FloatMixing.floatIsAtMost(speedControl, -0.01f), true, BooleanMixing.onRelease(limitBottom)));

        FloatStatus distance = ControlInterface.mainTuning.getFloat("Clamp Distance +M", 1.0f);

        FloatMixing.pumpWhen(EventMixing.filterEvent(useEncoder, true, BooleanMixing.onPress(atBottomStatus)), encoder, FloatMixing.negate((FloatOutput) distance));
        EventMixing.filterEvent(useEncoder, true, BooleanMixing.onPress(atTopStatus)).send(zeroEncoder);

        FloatStatus p = ControlInterface.mainTuning.getFloat("Clamp PID P", 20.0f);
        FloatStatus i = ControlInterface.mainTuning.getFloat("Clamp PID I", 0.0f);
        FloatStatus d = ControlInterface.mainTuning.getFloat("Clamp PID D", 0.0f);

        heightReadout = FloatMixing.normalizeFloat(encoder, FloatMixing.negate((FloatInput) distance), FloatMixing.always(0.0f));

        FloatInputPoll distanceFromTarget = FloatMixing.subtraction.of((FloatInputPoll) height, heightReadout);
        
        atDesiredHeight = BooleanMixing.andBooleans(FloatMixing.floatIsAtLeast(distanceFromTarget, FloatMixing.negate(heightPadding)), 
                FloatMixing.floatIsAtMost(distanceFromTarget, heightPadding));
        
        PIDControl pid = new PIDControl(heightReadout, height, p, i, d);

        pid.integralTotal.setWhen(0.0f, BooleanMixing.onRelease(mode));
        pid.setOutputBounds(ControlInterface.mainTuning.getFloat("clamp-max-height-speed", 1.0f));
        FloatMixing.pumpWhen(BooleanMixing.onRelease(mode), heightReadout, height);

        QuasarHelios.constantControl.send(pid);

        FloatOutput out = (value) -> {
            if (atTopStatus.get()) {
                value = Math.max(value, 0);
            }
            if (atBottomStatus.get()) {
                value = Math.min(value, 0);
            }
            if (value >= -0.1f && value <= 0.1f) {
                value = 0;
            }
            speedControl.set(value);
        };

        mode.setTrueWhen(EventMixing.filterEvent(FloatMixing.floatIsOutsideRange(speed, -0.3f, 0.3f), true, QuasarHelios.globalControl));
        mode.setTrueWhen(Igneous.startTele);

        FloatMixing.pumpWhen(QuasarHelios.constantControl, Mixing.select(BooleanMixing.orBooleans(mode,
                useEncoder.asInvertedInput()), pid, speed), out);

        // The autocalibrator runs when it's needed, AND allowed to by tuning (so that it can be disabled) AND the robot is enable in teleop or autonomous mode.
        // Once the encoder gets reset, it's no longer needed, and won't run. (Unless manually reactivated.)
        new InstinctModule(BooleanMixing.andBooleans(BooleanMixing.invert(Igneous.getIsDisabled()),
                needsAutoCalibration, ControlInterface.mainTuning.getBoolean("Clamp Autocalibration Allow +M", true),
                BooleanMixing.orBooleans(Igneous.getIsTeleop(), Igneous.getIsAutonomous()))) {
            private final FloatInputPoll downwardTime = ControlInterface.mainTuning.getFloat("Clamp Autocalibration Downward Time +M", 0.2f);

            @Override
            protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
                if (limitTop.get() || atTop.get()) {
                    zeroEncoder.event();
                    Logger.info("Autocalibrated: already at top.");
                } else {
                    Logger.info("Attempting autocalibration: not at top.");
                    mode.set(MODE_HEIGHT); // to make sure that the automatic set-to-current-value has time to complete, we do this here
                    waitForTime(200);
                    Logger.info("Jolting down...");
                    // first, go down momentarily, so if we're already at the top past the limit switch, we won't break anything
                    height.set(-2.0f); // make sure to go down even though not calibrated.
                    mode.set(MODE_HEIGHT);
                    waitForTime(downwardTime);
                    height.set(3.0f); // make sure to go up even though not calibrated.
                    mode.set(MODE_HEIGHT);
                    Logger.info("Autocalibration: aligning up...");
                    try {
                        waitUntil(atTop);
                    } finally {
                        height.set(0.0f);
                        speed.set(0); // will probably be overridden; but just in case.
                        mode.set(MODE_SPEED);
                    }
                }
            }

            protected String getTypeName() {
                return "clamp autocalibrator";
            }
        }.updateWhen(Igneous.globalPeriodic);

        Cluck.publish("Clamp Open Control", open);
        Cluck.publish("Clamp Height Encoder", FloatMixing.createDispatch(encoder, Igneous.globalPeriodic));
        Cluck.publish("Clamp Height Scaled", FloatMixing.createDispatch(heightReadout, Igneous.globalPeriodic));
        Cluck.publish("Clamp Height Setting", height);
        Cluck.publish("Clamp Integral Total", pid.integralTotal);
        Cluck.publish("Clamp Limit Top (Direct)", limitTop);
        Cluck.publish("Clamp Limit Bottom (Direct)", limitBottom);
        Cluck.publish("Clamp Limit Top", atTopStatus);
        Cluck.publish("Clamp Limit Bottom", atBottomStatus);
        Cluck.publish("Clamp Motor Speed", speedControl);
        Cluck.publish("Clamp PID Output", (FloatInput) pid);

        Cluck.publish("Clamp Distance", distance);
        Cluck.publish("Clamp Min Set", FloatMixing.pumpEvent(encoder, FloatMixing.negate((FloatOutput) distance)));
        Cluck.publish("Clamp Max Set", zeroEncoder);
        Cluck.publish("Clamp Mode", mode);
        Cluck.publish("Clamp Speed", speed);
        Cluck.publish("Clamp Needs Autocalibration", needsAutoCalibration);
    }
}
