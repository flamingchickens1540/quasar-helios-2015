package org.team1540.quasarhelios;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanOutput;
import ccre.channel.BooleanStatus;
import ccre.channel.EventInput;
import ccre.channel.EventLogger;
import ccre.channel.EventOutput;
import ccre.channel.FloatInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.cluck.Cluck;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.EventMixing;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.Mixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;
import ccre.log.LogLevel;
import ccre.log.Logger;

public class Elevator {
    private static final CANTalonWrapper elevatorTalon = new CANTalonWrapper("Elevator CAN", 0);

    private static final BooleanStatus raising = new BooleanStatus();
    private static final BooleanStatus lowering = new BooleanStatus();

    public static final BooleanStatus overrideEnabled = new BooleanStatus();
    public static final FloatStatus overrideValue = new FloatStatus(0.0f);

    public static final EventOutput setTop = EventMixing.combine(lowering.getSetFalseEvent(), raising.getSetTrueEvent(), new EventLogger(LogLevel.FINE, "Send Elevator to top"));
    public static final EventOutput setBottom = EventMixing.combine(raising.getSetFalseEvent(), lowering.getSetTrueEvent(), new EventLogger(LogLevel.FINE, "Send Elevator to bottom"));
    public static final EventOutput stop = BooleanMixing.getSetEvent(BooleanMixing.combine(raising, lowering), false);

    private static final BooleanStatus atTopStatus = new BooleanStatus();
    private static final BooleanStatus atBottomStatus = new BooleanStatus();

    public static final BooleanInput atTop = atTopStatus;
    public static final BooleanInput atBottom = atBottomStatus;

    private static FloatInput winchRaisingSpeed = ControlInterface.mainTuning.getFloat("Elevator Winch Speed Raising +M", 1.0f);
    private static FloatInput winchLoweringSpeed = ControlInterface.mainTuning.getFloat("Elevator Winch Speed Lowering +M", 1.0f);

    public static void setup() {
        BooleanInputPoll actuallyRaising = BooleanMixing.orBooleans(
                BooleanMixing.andBooleans(BooleanMixing.invert((BooleanInput) overrideEnabled), raising),
                BooleanMixing.andBooleans(overrideEnabled, FloatMixing.floatIsAtLeast(overrideValue, 0)));

        BooleanInputPoll actuallyLowering = BooleanMixing.orBooleans(
                BooleanMixing.andBooleans(BooleanMixing.invert((BooleanInput) overrideEnabled), lowering),
                BooleanMixing.andBooleans(overrideEnabled, FloatMixing.floatIsAtMost(overrideValue, 0)));

        setupLimitSwitchesAndPublishing(actuallyRaising, actuallyLowering);

        raising.setFalseWhen(EventMixing.filterEvent(atTop, true, Igneous.constantPeriodic));
        lowering.setFalseWhen(EventMixing.filterEvent(atBottom, true, Igneous.constantPeriodic));

        atTop.send(new BooleanOutput() {
            public void set(boolean value) {
                Logger.finer(value ? "Elevator at top" : "Elevator left top");
            }
        });
        atBottom.send(new BooleanOutput() {
            public void set(boolean value) {
                Logger.finer(value ? "Elevator at bottom" : "Elevator left bottom");
            }
        });

        EventInput currentFault = setupMotorControl();

        setupTimeout();

        setupAutoalign(currentFault);
    }

    private static void setupLimitSwitchesAndPublishing(BooleanInputPoll reallyRaising, BooleanInputPoll reallyLowering) {
        BooleanInput limitTop = BooleanMixing.invert(Igneous.makeDigitalInputByInterrupt(0));
        BooleanInput limitBottom = BooleanMixing.invert(Igneous.makeDigitalInputByInterrupt(1));
        //BooleanInput limitTop = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(0)), Igneous.constantPeriodic);
        //BooleanInput limitBottom = BooleanMixing.createDispatch(BooleanMixing.invert(Igneous.makeDigitalInput(1)), Igneous.constantPeriodic);
        Logger.info("With interrupts.");

        atTopStatus.set(limitTop.get());
        atBottomStatus.set(limitBottom.get());

        atTopStatus.setTrueWhen(EventMixing.filterEvent(reallyRaising, true, BooleanMixing.onPress(limitTop)));
        atBottomStatus.setTrueWhen(EventMixing.filterEvent(reallyLowering, true, BooleanMixing.onPress(limitBottom)));

        atTopStatus.setFalseWhen(EventMixing.filterEvent(reallyLowering, true, BooleanMixing.onRelease(limitTop)));
        atBottomStatus.setFalseWhen(EventMixing.filterEvent(reallyRaising, true, BooleanMixing.onRelease(limitBottom)));

        atTopStatus.setFalseWhen(BooleanMixing.onPress(limitBottom));
        atBottomStatus.setFalseWhen(BooleanMixing.onPress(limitTop));

        // atTopStatus is also modified by setupAutoalign.

        publishControls(limitTop, limitBottom);
    }

    private static void publishControls(BooleanInput limitTop, BooleanInput limitBottom) {
        Cluck.publish("Elevator Moving Stop", stop);
        Cluck.publish("Elevator Position Top", setTop);
        Cluck.publish("Elevator Position Bottom", setBottom);

        Cluck.publish("Elevator Moving Raising", raising);
        Cluck.publish("Elevator Moving Lowering", lowering);

        Cluck.publish("Elevator Limit Top (Direct)", limitTop);
        Cluck.publish("Elevator Limit Bottom (Direct)", limitBottom);

        Cluck.publish("Elevator Limit Top", atTopStatus);
        Cluck.publish("Elevator Limit Bottom", atBottomStatus);
    }

    private static EventInput setupMotorControl() {
        FloatInputPoll main = Mixing.quadSelect(raising, lowering, FloatMixing.always(0.0f), FloatMixing.negate(winchLoweringSpeed), winchRaisingSpeed, FloatMixing.always(0.0f));
        QuasarHelios.publishFault("elevator-both-directions", BooleanMixing.andBooleans(raising, lowering));
        FloatInputPoll override = QuasarHelios.limitSwitches(overrideValue, atBottom, atTop);

        EventInput currentFault = elevatorTalon.setupCurrentBreakerWithFaultPublish(55, "elevator-current-fault");
        EventLogger.log(currentFault, LogLevel.FINE, "Elevator current fault!");
        currentFault.send(EventMixing.combine(raising.getSetFalseEvent(), lowering.getSetFalseEvent()));

        FloatOutput winch = elevatorTalon.getAdvanced(0.05f, "Elevator Winch Speed Output");
        FloatMixing.pumpWhen(QuasarHelios.constantControl, Mixing.select((BooleanInputPoll) overrideEnabled, main, override), winch);

        return currentFault;
    }

    private static void setupTimeout() {
        FloatInput elevatorTimeout = ControlInterface.mainTuning.getFloat("Elevator Timeout +M", 3.0f);

        ExpirationTimer timer = new ExpirationTimer();
        EventInput elevatorTimedOut = timer.schedule(elevatorTimeout);
        QuasarHelios.publishStickyFault("elevator-timeout-fault", elevatorTimedOut);
        BooleanMixing.setWhen(elevatorTimedOut, BooleanMixing.combine(raising, lowering), false);
        EventLogger.log(elevatorTimedOut, LogLevel.FINE, "Elevator timed out!");

        BooleanMixing.xorBooleans(raising, lowering).send(timer.getRunningControl());
    }

    private static void setupAutoalign(EventInput currentFault) {
        BooleanStatus needsAutoAlign = new BooleanStatus(!atTop.get() && !atBottom.get());

        Cluck.publish("Elevator Needs Autoalign", needsAutoAlign);

        needsAutoAlign.setFalseWhen(BooleanMixing.onPress(atTop));
        needsAutoAlign.setFalseWhen(BooleanMixing.onPress(atBottom));

        BooleanInputPoll shouldBeAutoaligning = BooleanMixing.andBooleans(BooleanMixing.invert(Igneous.getIsDisabled()),
                needsAutoAlign, ControlInterface.mainTuning.getBoolean("Elevator Enable Autoalign", true),
                BooleanMixing.orBooleans(Igneous.getIsTeleop(), Igneous.getIsAutonomous()));

        // If we run into the top of the elevator here - then we know that we're at the top.
        atTopStatus.setTrueWhen(EventMixing.filterEvent(shouldBeAutoaligning, true, currentFault));

        new InstinctModule(shouldBeAutoaligning) {
            @Override
            protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
                if (atTop.get() || atBottom.get()) {
                    needsAutoAlign.set(false);
                    return; // just in case
                }
                Logger.info("Autoaligning...");
                setTop.event();
                waitUntil(atTop);
                Logger.info("Autoaligned!");
            }

            protected String getTypeName() {
                return "elevator autoaligner";
            }
        }.updateWhen(Igneous.globalPeriodic);
    }
}
