package org.team1540.quasarhelios;

import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.igneous.Igneous;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoEjector extends InstinctModule {
    private final BooleanStatus running;
    private FloatInput clampHeight = ControlInterface.mainTuning.getFloat("AutoEjector Clamp Height +M", 1.0f);
    private FloatInput timeout = ControlInterface.mainTuning.getFloat("AutoEjector Timeout +M", 2.0f);

    private AutoEjector(BooleanStatus running) {
        this.running = running;
    }

    public static BooleanStatus create() {
        BooleanStatus b = new BooleanStatus(false);
        AutoEjector a = new AutoEjector(b);

        a.setShouldBeRunning(b);

        Rollers.running.setFalseWhen(b.onRelease());
        Rollers.direction.setTrueWhen(b.onRelease());

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
                waitUntil(Clamp.atDesiredHeight.and(Elevator.atBottom)); // TODO: fix this
            } else {
                waitUntil(Clamp.atDesiredHeight);
            }

            boolean wasRunning = Rollers.running.get();
            boolean direction = Rollers.direction.get();

            try {
                Rollers.closed.set(false);
                Rollers.direction.set(Rollers.OUTPUT);
                Rollers.running.set(true);

                waitUntilNot(AutoLoader.crateInPosition);
                waitForTime(timeout);
            } finally {
                Rollers.running.set(wasRunning);
                Rollers.direction.set(direction);
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
