package org.team1540.quasarhelios;

import ccre.channel.BooleanCell;
import ccre.channel.FloatInput;
import ccre.frc.FRC;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;

public class AutoEjector extends InstinctModule {
    private final BooleanCell running;
    private FloatInput clampHeight = ControlInterface.mainTuning.getFloat("AutoEjector Clamp Height +M", 1.0f);
    private FloatInput timeout = ControlInterface.mainTuning.getFloat("AutoEjector Timeout +M", 2.0f);

    private AutoEjector(BooleanCell running) {
        this.running = running;
    }

    public static BooleanCell create() {
        BooleanCell b = new BooleanCell(false);
        AutoEjector a = new AutoEjector(b);

        a.setShouldBeRunning(b);

        Rollers.running.setFalseWhen(b.onRelease());
        Rollers.direction.setTrueWhen(b.onRelease());

        b.setFalseWhen(FRC.startDisabled);

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
                waitUntil(Clamp.atDesiredHeight);
                waitUntil(Elevator.atBottom);
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
