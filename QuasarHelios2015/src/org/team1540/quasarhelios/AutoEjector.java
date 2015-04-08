package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.FloatInputPoll;
import ccre.ctrl.BooleanMixing;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoEjector extends InstinctModule {
    private final BooleanStatus running;
    private FloatInputPoll clampHeight = ControlInterface.mainTuning.getFloat("AutoEjector Clamp Height +M", 1.0f);
    private FloatInputPoll timeout = ControlInterface.mainTuning.getFloat("AutoEjector Timeout +M", 2.0f);

    private AutoEjector(BooleanStatus running) {
        this.running = running;
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus(false);
        AutoEjector a = new AutoEjector(b);

        a.setShouldBeRunning(b);
        a.updateWhen(Igneous.constantPeriodic);

        Rollers.running.setFalseWhen(BooleanMixing.onRelease(b));
        Rollers.direction.setTrueWhen(BooleanMixing.onRelease(b));

        b.setFalseWhen(Igneous.startDisabled);

        return b;
    }

    @Override
    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
            QuasarHelios.autoHumanLoader.set(false);

            Clamp.mode.set(Clamp.MODE_HEIGHT);
            Clamp.height.set(clampHeight.get());

            if (!Elevator.atBottom.get()) {
                Elevator.setBottom.event();
                waitUntil(BooleanMixing.andBooleans(Clamp.atDesiredHeight, Elevator.atBottom));
            } else {
                waitUntil(Clamp.atDesiredHeight);
            }

            boolean running = Rollers.running.get();
            boolean direction = Rollers.direction.get();
            boolean closed = Rollers.closed.get();

            try {
                Rollers.closed.set(false);
                Rollers.direction.set(Rollers.OUTPUT);
                Rollers.running.set(true);

                waitUntil(BooleanMixing.invert(AutoLoader.crateInPosition));
                waitForTime(timeout);
            } finally {
                Rollers.running.set(running);
                Rollers.direction.set(direction);
                //Rollers.closed.set(closed); - not actually helpful
                Rollers.closed.set(false);
            }
        } finally {
            running.set(false);
        }
    }

    @Override
    protected String getTypeName() {
        return "auto ejector";
    }
}
