package org.team1540.quasarhelios;

import ccre.channel.BooleanCell;
import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.EventInput;
import ccre.channel.EventLogger;
import ccre.channel.EventOutput;
import ccre.channel.FloatCell;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.cluck.Cluck;
import ccre.frc.FRC;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;
import ccre.log.LogLevel;
import ccre.log.Logger;
import ccre.timers.ExpirationTimer;

public class Elevator {
    private static final CANTalonWrapper elevatorTalon = new CANTalonWrapper("Elevator CAN", 0);

    private static final BooleanCell raising = new BooleanCell();
    private static final BooleanCell lowering = new BooleanCell();

    public static final BooleanCell overrideEnabled = new BooleanCell();
    public static final FloatCell overrideValue = new FloatCell(0.0f);

    public static final EventOutput setTop = () -> {
        lowering.set(false);
        raising.set(true);
        Logger.fine("Send Elevator to top");
    };
    public static final EventOutput setBottom = () -> {
        raising.set(false);
        lowering.set(true);
        Logger.fine("Send Elevator to bottom");
    };
    public static final EventOutput stop = () -> {
        raising.set(false);
        lowering.set(false);
    };

    private static final BooleanCell atTopStatus = new BooleanCell();
    private static final BooleanCell atBottomStatus = new BooleanCell();

    public static final BooleanInput atTop = atTopStatus;
    public static final BooleanInput atBottom = atBottomStatus;

    private static FloatInput winchRaisingSpeed = ControlInterface.mainTuning.getFloat("Elevator Winch Speed Raising +M", 1.0f);
    private static FloatInput winchLoweringSpeed = ControlInterface.mainTuning.getFloat("Elevator Winch Speed Lowering +M", 1.0f);

    public static void setup() {
        BooleanInput actuallyRaising = raising.andNot(overrideEnabled).or(overrideValue.atLeast(0).and(overrideEnabled));

        BooleanInput actuallyLowering = lowering.andNot(overrideEnabled).or(overrideValue.atMost(0).and(overrideEnabled));

        setupLimitSwitchesAndPublishing(actuallyRaising, actuallyLowering);

        raising.setFalseWhen(FRC.constantPeriodic.and(atTop));
        lowering.setFalseWhen(FRC.constantPeriodic.and(atBottom));

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

    private static void setupLimitSwitchesAndPublishing(BooleanInput reallyRaising, BooleanInput reallyLowering) {
        BooleanInput limitTop = FRC.digitalInputByInterrupt(0).not();
        BooleanInput limitBottom = FRC.digitalInputByInterrupt(1).not();

        atTopStatus.set(limitTop.get());
        atBottomStatus.set(limitBottom.get());

        atTopStatus.setTrueWhen(limitTop.onPress().and(reallyRaising));
        atBottomStatus.setTrueWhen(limitBottom.onPress().and(reallyLowering));

        atTopStatus.setFalseWhen(limitTop.onRelease().and(reallyLowering));
        atBottomStatus.setFalseWhen(limitBottom.onRelease().and(reallyRaising));

        atTopStatus.setFalseWhen(limitBottom.onPress());
        atBottomStatus.setFalseWhen(limitTop.onPress());

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
        FloatInput main = raising.toFloat(lowering.toFloat(0, winchLoweringSpeed.negated()), lowering.toFloat(winchRaisingSpeed, 0));
        QuasarHelios.publishFault("elevator-both-directions", raising.and(lowering));
        FloatInput override = QuasarHelios.limitSwitches(overrideValue, atBottom, atTop);

        EventInput currentFault = elevatorTalon.setupCurrentBreakerWithFaultPublish(55, "elevator-current-fault");
        EventLogger.log(currentFault, LogLevel.FINE, "Elevator current fault!");
        currentFault.send(raising.eventSet(false).combine(lowering.eventSet(false)));

        FloatOutput winch = elevatorTalon.getAdvanced(0.05f, "Elevator Winch Speed Output");
        overrideEnabled.toFloat(main, override).send(winch);

        return currentFault;
    }

    private static void setupTimeout() {
        FloatInput elevatorTimeout = ControlInterface.mainTuning.getFloat("Elevator Timeout +M", 3.0f);

        ExpirationTimer timer = new ExpirationTimer();
        EventInput elevatorTimedOut = timer.schedule(elevatorTimeout);
        QuasarHelios.publishStickyFault("elevator-timeout-fault", elevatorTimedOut);
        elevatorTimedOut.send(stop);
        EventLogger.log(elevatorTimedOut, LogLevel.FINE, "Elevator timed out!");

        raising.xor(lowering).send(timer.getRunningControl());
    }

    private static void setupAutoalign(EventInput currentFault) {
        BooleanCell needsAutoAlign = new BooleanCell(!atTop.get() && !atBottom.get());

        Cluck.publish("Elevator Needs Autoalign", needsAutoAlign);

        needsAutoAlign.setFalseWhen(atTop.onPress());
        needsAutoAlign.setFalseWhen(atBottom.onPress());

        BooleanInput shouldBeAutoaligning = FRC.robotEnabled().and(needsAutoAlign).and(ControlInterface.mainTuning.getBoolean("Elevator Enable Autoalign", true)).and(FRC.inTeleopMode().or(FRC.inAutonomousMode()));

        // If we run into the top of the elevator here - then we know that we're
        // at the top.
        atTopStatus.setTrueWhen(currentFault.and(shouldBeAutoaligning));

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
        };
    }
}
